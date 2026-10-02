package com.reaxkey.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.reaxkey.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

class SourceDiscoveryEngine(private val context:Context,private val repository:MediaRepository,
    private val recognizers:List<MediaSourceRecognizer> = SourceAttribution.sharedIds.map(::SharedMediaRecognizer)+ExistingSafGrantRecognizer()+ReaxKeyImportRecognizer()) {
    fun discover():Flow<SourceCandidate> = flow {
        val known=repository.sources().first()
        val imports=known.firstOrNull {it.type=="IMPORTED"}?.let { SourceCandidate(it,if(it.enabled) DiscoveryStatus.AVAILABLE else DiscoveryStatus.AVAILABLE_REQUIRES_USER_GRANT,it.count,SourceConfidence.EXACT) }
        imports?.let {emit(it)}
        val granted=context.contentResolver.persistedUriPermissions.filter {it.isReadPermission && DocumentsContract.isTreeUri(it.uri)}.associateBy {it.uri.toString()}
        val grants=mutableListOf<SourceCandidate>()
        for(uri in (granted.keys+known.filter {it.type=="SAF_TREE"}.map {it.uri}).distinct()) {
            currentCoroutineContext().ensureActive()
            val old=known.firstOrNull {it.uri==uri}
            val source=old ?: MediaSource(UUID.nameUUIDFromBytes(uri.toByteArray()).toString(),"SAF_TREE","Reaction folder",uri)
            val candidate=inspectTree(source,uri in granted)
            if(old?.enabled==true) {
                val health=when(candidate.status) {
                    DiscoveryStatus.AVAILABLE_REQUIRES_USER_GRANT->"REVOKED"
                    DiscoveryStatus.PRIVATE_OR_INACCESSIBLE->"MISSING"
                    DiscoveryStatus.ERROR->"ERROR"
                    else->null
                }
                if(health!=null) repository.finishScan(old.id,System.currentTimeMillis(),health,0)
            }
            grants+=candidate;emit(candidate)
        }
        val counts=mutableMapOf<String,Int>();var error=false
        val currentAccess=accessLevel(context)
        try {
            // Without RELATIVE_PATH we cannot distinguish stickers from ordinary photos.
            if(Build.VERSION.SDK_INT>=29) {
            val projection=arrayOf(MediaStore.Images.Media.MIME_TYPE,MediaStore.Images.Media.RELATIVE_PATH)
            val selection="${MediaStore.Images.Media.MIME_TYPE} IN (?,?,?) AND ("+
                SourceAttribution.stickerPathHints.joinToString(" OR ") { "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?" }+")"
            context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,projection,
                selection,(StickerRules.supported+SourceAttribution.stickerPathHints).toTypedArray(),null)?.use { cursor ->
                while(cursor.moveToNext()) {
                    currentCoroutineContext().ensureActive()
                    val bucket=SourceAttribution.bucket(cursor.getString(1))
                    if(bucket=="none") continue
                    counts[bucket]=(counts[bucket] ?: 0)+1
                    if(counts.getValue(bucket)==1 || counts.getValue(bucket)%500==0) {
                        emit(SharedMediaRecognizer(bucket).discover(DiscoverySnapshot(currentAccess,counts.toMap())).single())
                    }
                }
            } ?: run {error=true}
            }
        } catch(e:CancellationException){throw e} catch(_:SecurityException){error=accessLevel(context)!=MediaAccessLevel.NONE} catch(_:Exception){error=true}
        val snapshot=DiscoverySnapshot(accessLevel(context),counts,grants,imports,error)
        for(recognizer in recognizers) {
            if(recognizer is ExistingSafGrantRecognizer || recognizer is ReaxKeyImportRecognizer) continue
            for(result in recognizer.discover(snapshot)) emit(result)
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun inspectTree(source:MediaSource,granted:Boolean):SourceCandidate {
        val uri=Uri.parse(source.uri)
        if(!validTree(uri)) return SourceCandidate(source,DiscoveryStatus.PRIVATE_OR_INACCESSIBLE)
        if(!granted) return SourceCandidate(source,DiscoveryStatus.AVAILABLE_REQUIRES_USER_GRANT,initialUri=source.uri)
        return try {
            val resolver=context.contentResolver
            val root=DocumentsContract.getTreeDocumentId(uri)
            val directory=DocumentsContract.buildDocumentUriUsingTree(uri,root)
            val name=resolver.query(directory,arrayOf(DocumentsContract.Document.COLUMN_MIME_TYPE),null,null,null)?.use {
                it.moveToFirst() && it.getString(0)==DocumentsContract.Document.MIME_TYPE_DIR
            } ?: false
            if(!name) return SourceCandidate(source,DiscoveryStatus.PRIVATE_OR_INACCESSIBLE)
            var count=0;val queue=ArrayDeque<String>();val seen=HashSet<String>();queue.add(root)
            while(queue.isNotEmpty()) {
                currentCoroutineContext().ensureActive()
                val parent=queue.removeFirst();if(!seen.add(parent)) continue
                resolver.query(DocumentsContract.buildChildDocumentsUriUsingTree(uri,parent),arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_MIME_TYPE),null,null,null)?.use {cursor ->
                    while(cursor.moveToNext()) {
                        currentCoroutineContext().ensureActive()
                        val mime=cursor.getString(1)
                        if(mime==DocumentsContract.Document.MIME_TYPE_DIR) queue.add(cursor.getString(0))
                        else if(mime in StickerRules.supported) count++
                    }
                } ?: throw java.io.IOException()
            }
            val recognized=if(uri.authority=="com.android.externalstorage.documents") SourceAttribution.bucket(root.substringAfter(':')) else "phone"
            SourceCandidate(if(recognized in listOf("whatsapp","whatsapp-business","telegram")) source.copy(name=recognized) else source,
                DiscoveryStatus.AVAILABLE,count,SourceConfidence.EXACT,source.uri)
        } catch(e:CancellationException){throw e} catch(_:SecurityException){SourceCandidate(source,DiscoveryStatus.AVAILABLE_REQUIRES_USER_GRANT,initialUri=source.uri)}
        catch(_:java.io.FileNotFoundException){SourceCandidate(source,DiscoveryStatus.PRIVATE_OR_INACCESSIBLE)}
        catch(_:Exception){SourceCandidate(source,DiscoveryStatus.ERROR)}
    }
    companion object {
        fun accessLevel(context:Context):MediaAccessLevel {
            fun granted(permission:String)=ContextCompat.checkSelfPermission(context,permission)==PackageManager.PERMISSION_GRANTED
            return when {
                Build.VERSION.SDK_INT>=33 && granted(Manifest.permission.READ_MEDIA_IMAGES)->MediaAccessLevel.FULL
                Build.VERSION.SDK_INT>=34 && granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)->MediaAccessLevel.SELECTED
                Build.VERSION.SDK_INT<33 && granted(Manifest.permission.READ_EXTERNAL_STORAGE)->MediaAccessLevel.FULL
                else->MediaAccessLevel.NONE
            }
        }
        fun validTree(uri:Uri):Boolean=try {
            uri.scheme=="content" && DocumentsContract.isTreeUri(uri) && DocumentsContract.getTreeDocumentId(uri).isNotBlank() &&
                (uri.authority!="com.android.externalstorage.documents" || SourceAttribution.allowedTreeId(DocumentsContract.getTreeDocumentId(uri)))
        } catch(_:Exception){false}
    }
}
