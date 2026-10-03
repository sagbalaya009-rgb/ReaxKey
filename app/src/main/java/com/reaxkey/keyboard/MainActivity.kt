package com.reaxkey.keyboard

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.reaxkey.data.Preferences
import com.reaxkey.data.UserPreferences
import com.reaxkey.library.*
import com.reaxkey.media.MediaAccess
import com.reaxkey.media.SourceDiscoveryEngine
import com.reaxkey.model.*
import com.reaxkey.settings.SettingsScreen
import com.reaxkey.ui.ReaxTheme
import com.reaxkey.ui.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val setup=mutableStateOf(SetupState.NOT_ENABLED)
    private val screen=mutableStateOf("setup")
    private val accessLevel=mutableStateOf(MediaAccessLevel.NONE)
    private val repo get()=(application as ReaxHost).repository
    private val media by lazy {MediaAccess(this,repo)}
    private val preferences by lazy {Preferences(this)}
    private val discovery by lazy {ViewModelProvider(this)[DiscoveryViewModel::class.java]}
    private val folder=registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> if(uri!=null) operation {
        require(SourceDiscoveryEngine.validTree(uri))
        contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val document=DocumentsContract.buildDocumentUriUsingTree(uri,DocumentsContract.getTreeDocumentId(uri))
        val valid=contentResolver.query(document,arrayOf(DocumentsContract.Document.COLUMN_MIME_TYPE),null,null,null)?.use {it.moveToFirst() && it.getString(0)==DocumentsContract.Document.MIME_TYPE_DIR} ?: false
        require(valid)
        val id=UUID.nameUUIDFromBytes(uri.toString().toByteArray()).toString()
        val old=repo.source(id)
        repo.putSource((old ?: MediaSource(id,"SAF_TREE",getString(R.string.personal_source),uri.toString())).copy(enabled=true,health="SCANNING",permissionFlags=Intent.FLAG_GRANT_READ_URI_PERMISSION))
        ScanWorker.enqueue(this@MainActivity,id)
        withContext(Dispatchers.Main){screen.value="stickers"}
    } }
    private val files=registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris -> if(uris.isNotEmpty()) operation {
        var count=0
        for(uri in uris) try {media.importCopy(uri);count++} catch(e:CancellationException){throw e} catch(_:Exception) { }
        withContext(Dispatchers.Main) {Toast.makeText(this@MainActivity,getString(R.string.import_complete,count,uris.size),Toast.LENGTH_LONG).show();screen.value="stickers"}
    } }
    private val permissions=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        accessLevel.value=SourceDiscoveryEngine.accessLevel(this);screen.value="stickers";discovery.find()
        operation {repo.sources().first().filter {source->source.type=="MEDIASTORE" && source.enabled}.forEach {source->ScanWorker.enqueue(this@MainActivity,source.id)}}
    }
    private val diagnostics=registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri -> if(uri!=null) operation {
        val text="ReaxKey ${packageManager.getPackageInfo(packageName,0).versionName}\nAndroid API ${Build.VERSION.SDK_INT}\nManufacturer=${Build.MANUFACTURER}\n"+repo.diagnostics()
        contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {it.write(text)}
    } }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState); refreshSetup()
        screen.value=if(savedInstanceState?.getString("screen")=="settings") "settings" else "stickers"
        setContent {
            val prefs by preferences.flow.collectAsState(UserPreferences())
            val found by discovery.state.collectAsState()
            val sources by remember {repo.sources()}.collectAsState(emptyList())
            ReaxTheme(prefs.theme) { Scaffold(bottomBar={NavigationBar {
                listOf("stickers" to R.string.sticker_tab,"settings" to R.string.settings).forEach { (key,label) ->
                    NavigationBarItem(selected=screen.value==key,onClick={screen.value=key},icon={Text(stringResource(label).take(1))},label={Text(stringResource(label))})
                }
            }}) { padding -> Box(Modifier.fillMaxSize().padding(padding)) {
                when(screen.value) {
                    "stickers" -> StickerHomeScreen(repo,found.candidates,found.checking,::importFiles,::refreshStickers,::selectStickerFolder,{requestShared()},{candidate->addCandidates(listOf(candidate))})
                    else -> SettingsScreen(preferences,setup.value!=SetupState.NOT_ENABLED,setup.value==SetupState.SELECTED,
                        {startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))},
                        {getSystemService(InputMethodManager::class.java).showInputMethodPicker()},
                        ::selectStickerFolder,::refreshStickers,sources,{diagnostics.launch("reaxkey-diagnostics.txt")})
                }
            } }
            }
        }
        discovery.find()
    }
    override fun onSaveInstanceState(outState:Bundle){outState.putString("screen",screen.value);super.onSaveInstanceState(outState)}
    override fun onResume() {super.onResume();refreshSetup();accessLevel.value=SourceDiscoveryEngine.accessLevel(this)}
    private fun importFiles(){files.launch(arrayOf("image/png","image/gif","image/webp"))}
    private fun selectStickerFolder() {
        // A picker location hint is not a storage grant; Android still requires the user to select it.
        val knownWhatsAppFolder=DocumentsContract.buildDocumentUri(
            "com.android.externalstorage.documents",
            "primary:Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Stickers"
        )
        folder.launch(knownWhatsAppFolder)
    }
    private fun sourceNameResource(source:MediaSource)=when {
        source.type=="IMPORTED" -> R.string.imports_source
        source.uri=="whatsapp" -> R.string.whatsapp_stickers
        source.uri=="whatsapp-business" -> R.string.business_stickers
        else -> R.string.personal_source
    }
    private fun refreshStickers() {
        discovery.find()
        operation {repo.sources().first().filter {it.enabled}.forEach {ScanWorker.enqueue(this@MainActivity,it.id)}}
    }
    private fun addCandidates(candidates:List<SourceCandidate>)=operation {
        for(candidate in candidates) {
            val source=candidate.source
            val old=repo.source(source.id)
            repo.putSource((old ?: source).copy(name=if(source.type=="SAF_TREE") source.name else getString(sourceNameResource(source)),enabled=true,health="SCANNING"))
            ScanWorker.enqueue(this@MainActivity,source.id)
        }
        withContext(Dispatchers.Main){screen.value="stickers"}
    }
    private fun refreshSetup() {
        val manager=getSystemService(InputMethodManager::class.java)
        val id=manager.enabledInputMethodList.firstOrNull {it.packageName==packageName}?.id
        setup.value=SetupState.from(id!=null,id!=null && Settings.Secure.getString(contentResolver,Settings.Secure.DEFAULT_INPUT_METHOD)==id)
    }
    private fun requestShared() {
        if(SourceDiscoveryEngine.accessLevel(this)!=MediaAccessLevel.NONE) {discovery.find();return}
        val required=if(Build.VERSION.SDK_INT>=34) arrayOf(Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            else if(Build.VERSION.SDK_INT>=33) arrayOf(Manifest.permission.READ_MEDIA_IMAGES) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        permissions.launch(required)
    }
    private fun operation(block:suspend ()->Unit) {lifecycleScope.launch(Dispatchers.IO) {
        try {block()} catch(e:CancellationException){throw e} catch(_:Exception){withContext(Dispatchers.Main){Toast.makeText(this@MainActivity,R.string.operation_failed,Toast.LENGTH_LONG).show()}}
    }}
}
