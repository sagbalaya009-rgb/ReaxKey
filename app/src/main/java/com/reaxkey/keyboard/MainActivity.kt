package com.reaxkey.keyboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.reaxkey.data.Preferences
import com.reaxkey.data.UserPreferences
import com.reaxkey.library.*
import com.reaxkey.media.MediaAccess
import com.reaxkey.model.*
import com.reaxkey.onboarding.SetupScreen
import com.reaxkey.settings.SettingsScreen
import com.reaxkey.ui.ReaxTheme
import com.reaxkey.ui.R
import kotlinx.coroutines.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val setup=mutableStateOf(SetupState.NOT_ENABLED)
    private val repo get()=(application as ReaxHost).repository
    private val media by lazy {MediaAccess(this,repo)}
    private val preferences by lazy {Preferences(this)}
    private val folder=registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> if(uri!=null) operation {
        contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val document=DocumentsContract.buildDocumentUriUsingTree(uri,DocumentsContract.getTreeDocumentId(uri))
        val name=contentResolver.query(document,arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),null,null,null)?.use {if(it.moveToFirst()) it.getString(0) else null} ?: getString(R.string.sources)
        val id=UUID.nameUUIDFromBytes(uri.toString().toByteArray()).toString()
        repo.putSource(MediaSource(id,"SAF_TREE",name,uri.toString(),permissionFlags=Intent.FLAG_GRANT_READ_URI_PERMISSION))
        ScanWorker.enqueue(this@MainActivity,id)
    } }
    private val files=registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris -> if(uris.isNotEmpty()) operation {
        var count=0
        for(uri in uris) try {media.importCopy(uri);count++} catch(e:CancellationException){throw e} catch(_:Exception) { }
        withContext(Dispatchers.Main) {Toast.makeText(this@MainActivity,getString(R.string.import_complete,count,uris.size),Toast.LENGTH_LONG).show()}
    } }
    private val permissions=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if(result.values.any {it}) operation {connectShared()} else Toast.makeText(this,R.string.permission_denied,Toast.LENGTH_LONG).show()
    }
    private val diagnostics=registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri -> if(uri!=null) operation {
        val text="ReaxKey 0.1.0-dev\nAndroid API ${Build.VERSION.SDK_INT}\nManufacturer=${Build.MANUFACTURER}\n"+repo.diagnostics()
        contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {it.write(text)}
    } }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState); refreshSetup()
        setContent {
            val prefs by preferences.flow.collectAsState(UserPreferences())
            var screen by rememberSaveable {mutableStateOf(if(setup.value==SetupState.SELECTED) "library" else "setup")}
            ReaxTheme(prefs.theme) { Scaffold(bottomBar={NavigationBar {
                listOf("library" to R.string.library,"collections" to R.string.collections,"sources" to R.string.sources,"settings" to R.string.settings).forEach { (key,label) ->
                    NavigationBarItem(selected=screen==key,onClick={screen=key},icon={Text(stringResource(label).take(1))},label={Text(stringResource(label))})
                }
            }}) { padding -> Box(Modifier.fillMaxSize().padding(padding)) {
                val delete:(MediaItem)->Unit={item->operation {media.deleteOwned(item)}}
                when(screen) {
                    "setup" -> SetupScreen(setup.value,{startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))},{getSystemService(InputMethodManager::class.java).showInputMethodPicker()})
                    "library" -> LibraryScreen(repo,deleteOwned=delete)
                    "collections" -> CollectionsScreen(repo,delete)
                    "sources" -> SourcesScreen(repo,{folder.launch(null)},{files.launch(arrayOf("image/png","image/jpeg","image/gif","image/webp"))},{requestShared()},{source->
                        if(source.type=="SAF_TREE" && source.health in listOf("REVOKED","DISCONNECTED")) folder.launch(Uri.parse(source.uri))
                        else if(source.type=="MEDIASTORE") requestShared()
                        else operation {repo.putSource(source.copy(enabled=true));ScanWorker.enqueue(this@MainActivity,source.id)}
                    },{source->operation {
                        WorkManager.getInstance(this@MainActivity).cancelUniqueWork("scan-${source.id}")
                        repo.disconnect(source.id)
                        if(source.type=="SAF_TREE") try {contentResolver.releasePersistableUriPermission(Uri.parse(source.uri),Intent.FLAG_GRANT_READ_URI_PERMISSION)} catch(_:SecurityException) { }
                    }})
                    else -> SettingsScreen(preferences,{screen="setup"},{diagnostics.launch("reaxkey-diagnostics.txt")})
                }
            } } }
        }
    }
    override fun onResume() {super.onResume();refreshSetup()}
    private fun refreshSetup() {
        val manager=getSystemService(InputMethodManager::class.java)
        val id=manager.enabledInputMethodList.firstOrNull {it.packageName==packageName}?.id
        setup.value=SetupState.from(id!=null,id!=null && Settings.Secure.getString(contentResolver,Settings.Secure.DEFAULT_INPUT_METHOD)==id)
    }
    private fun requestShared() {
        val required=if(Build.VERSION.SDK_INT>=34) arrayOf(Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            else if(Build.VERSION.SDK_INT>=33) arrayOf(Manifest.permission.READ_MEDIA_IMAGES) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        if(required.all {ContextCompat.checkSelfPermission(this,it)==PackageManager.PERMISSION_GRANTED}) operation {connectShared()}
        else permissions.launch(required)
    }
    private suspend fun connectShared() {repo.putSource(MediaSource("shared","MEDIASTORE",getString(R.string.images)));ScanWorker.enqueue(this,"shared")}
    private fun operation(block:suspend ()->Unit) {lifecycleScope.launch(Dispatchers.IO) {
        try {block()} catch(e:CancellationException){throw e} catch(_:Exception){withContext(Dispatchers.Main){Toast.makeText(this@MainActivity,R.string.operation_failed,Toast.LENGTH_LONG).show()}}
    }}
}
