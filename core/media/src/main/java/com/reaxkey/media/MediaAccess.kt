package com.reaxkey.media

import android.content.Context
import android.content.ContentUris
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.reaxkey.model.*
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

class MediaAccess(private val context:Context,private val repository:MediaRepository) {
    private val resolver=context.contentResolver
    private val imports=File(context.filesDir,"imports").apply { mkdirs() }
    suspend fun scan(source:MediaSource,progress:suspend(Int)->Unit={})=withContext(Dispatchers.IO) {
        if(!source.enabled) return@withContext
        val generation=System.currentTimeMillis()
        var count=0
        suspend fun index(uri:Uri,name:String,size:Long,modified:Long) {
            ensureActive()
            val old=repository.find(source.id,uri.toString())
            val item=if(old!=null && old.size==size && old.modified==modified && size>0 && modified>0) old.copy(available=true)
                else metadata(uri,source.id,name,size,modified,false)
            if(item!=null) { repository.index(item,generation); count++; if(count%25==0) progress(count) }
        }
        try {
            when(source.type) {
                "SAF_TREE" -> {
                    val tree=Uri.parse(source.uri)
                    if(resolver.persistedUriPermissions.none { it.uri==tree && it.isReadPermission }) throw SecurityException()
                    val queue=ArrayDeque<String>(); val seen=HashSet<String>()
                    queue.add(DocumentsContract.getTreeDocumentId(tree))
                    while(queue.isNotEmpty()) {
                        ensureActive()
                        val parent=queue.removeFirst(); if(!seen.add(parent)) continue
                        resolver.query(DocumentsContract.buildChildDocumentsUriUsingTree(tree,parent),arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.COLUMN_SIZE,DocumentsContract.Document.COLUMN_LAST_MODIFIED),null,null,null)?.use { cursor ->
                            while(cursor.moveToNext()) {
                                ensureActive()
                                val id=cursor.getString(0); val mime=cursor.getString(2)
                                if(mime==DocumentsContract.Document.MIME_TYPE_DIR) queue.add(id)
                                else if(mime in MediaRules.supported || mime=="application/octet-stream") index(DocumentsContract.buildDocumentUriUsingTree(tree,id),cursor.getString(1) ?: "",cursor.getLong(3),cursor.getLong(4))
                            }
                        } ?: throw IOException("Provider unavailable")
                    }
                }
                "MEDIASTORE" -> {
                    val base=MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    resolver.query(base,arrayOf(MediaStore.Images.Media._ID,MediaStore.Images.Media.DISPLAY_NAME,MediaStore.Images.Media.SIZE,MediaStore.Images.Media.DATE_MODIFIED),null,null,null)?.use { cursor ->
                        while(cursor.moveToNext()) index(ContentUris.withAppendedId(base,cursor.getLong(0)),cursor.getString(1) ?: "",cursor.getLong(2),cursor.getLong(3)*1000)
                    } ?: throw IOException("Provider unavailable")
                }
                "IMPORTED" -> imports.listFiles()?.filter { !it.name.endsWith(".part") }?.forEach { file ->
                    val uri=FileProvider.getUriForFile(context,"${context.packageName}.media",file)
                    val old=repository.find(source.id,uri.toString())
                    val item=metadata(uri,source.id,old?.name ?: file.name,file.length(),file.lastModified(),true)
                    if(item!=null) { repository.index(item,generation); count++ }
                }
            }
            repository.finishScan(source.id,generation,"OK",count); progress(count)
        } catch(e:CancellationException) { throw e }
        catch(_:SecurityException) { repository.finishScan(source.id,generation,"REVOKED",count) }
        catch(_:java.io.FileNotFoundException) { repository.finishScan(source.id,generation,"MISSING",count) }
        catch(_:Exception) { repository.finishScan(source.id,generation,"ERROR",count) }
    }
    private fun metadata(uri:Uri,source:String,name:String,size:Long,modified:Long,owned:Boolean):MediaItem? {
        if(uri.scheme!="content") return null
        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        // A bounds-only decode deliberately returns no Bitmap; inspect Options instead.
        val input=resolver.openInputStream(uri) ?: return null
        input.use { BitmapFactory.decodeStream(it,null,bounds) }
        val mime=bounds.outMimeType ?: return null
        if(mime !in MediaRules.supported || bounds.outWidth<=0 || bounds.outHeight<=0) return null
        var animated=mime=="image/gif"
        if(mime=="image/webp") resolver.openInputStream(uri)?.use { stream ->
            val bytes=ByteArray(32); val read=stream.read(bytes)
            animated=read>=21 && String(bytes,12,4,Charsets.US_ASCII)=="VP8X" && (bytes[20].toInt() and 2)!=0
        }
        return MediaItem(sourceId=source,uri=uri.toString(),name=name,mime=mime,kind=MediaRules.classify(mime,animated,bounds.outWidth,bounds.outHeight),width=bounds.outWidth,height=bounds.outHeight,size=size,modified=modified,animated=animated,owned=owned)
    }
    suspend fun importCopy(uri:Uri):Long=withContext(Dispatchers.IO) {
        require(uri.scheme=="content")
        val temporary=File(imports,"${UUID.randomUUID()}.part")
        try {
            val hash=MessageDigest.getInstance("SHA-256")
            resolver.openInputStream(uri)?.use { input -> temporary.outputStream().use { output ->
                val buffer=ByteArray(32768); var total=0L
                while(true) { ensureActive(); val n=input.read(buffer); if(n<0) break; total+=n; require(total<=MAX_BYTES) { "Media exceeds import limit" }; hash.update(buffer,0,n); output.write(buffer,0,n) }
            } } ?: throw IOException("Cannot open media")
            val temporaryUri=FileProvider.getUriForFile(context,"${context.packageName}.media",temporary)
            val name=resolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use { if(it.moveToFirst()) it.getString(0) else null } ?: "image"
            val metadata=metadata(temporaryUri,"imports",name,temporary.length(),temporary.lastModified(),true) ?: throw IOException("Unsupported media")
            val extension=when(metadata.mime) { "image/png"->"png"; "image/jpeg"->"jpg"; "image/gif"->"gif"; else->"webp" }
            val file=File(imports,hash.digest().joinToString("") { "%02x".format(it) }+".$extension")
            if(!file.exists() && !temporary.renameTo(file)) throw IOException("Cannot save media")
            if(repository.source("imports")==null) repository.putSource(MediaSource("imports","IMPORTED",context.getString(com.reaxkey.media.R.string.imported_source)))
            repository.index(metadata.copy(uri=FileProvider.getUriForFile(context,"${context.packageName}.media",file).toString()),System.currentTimeMillis())
        } finally { temporary.delete() }
    }
    suspend fun deleteOwned(item:MediaItem)=withContext(Dispatchers.IO) {
        require(item.owned && item.sourceId=="imports")
        val uri=Uri.parse(item.uri)
        require(uri.authority=="${context.packageName}.media")
        // Only our provider's restricted imports root may be deleted.
        val name=uri.lastPathSegment ?: throw IOException("Missing media")
        val file=File(imports,name).canonicalFile
        require(file.parentFile==imports.canonicalFile)
        if(file.exists() && !file.delete()) throw IOException("Cannot delete media")
        repository.remove(item.id); repository.markUnavailable(item.id)
    }
    suspend fun sendUri(item:MediaItem):Uri=withContext(Dispatchers.IO) {
        require(MediaRules.contentUri(item.uri))
        val directory=File(context.cacheDir,"outgoing").apply { mkdirs() }
        directory.listFiles()?.filter { System.currentTimeMillis()-it.lastModified()>86_400_000 }?.forEach { it.delete() }
        val extension=when(item.mime) { "image/png"->"png"; "image/jpeg"->"jpg"; "image/gif"->"gif"; else->"webp" }
        val file=File(directory,"${UUID.randomUUID()}.$extension")
        try {
            resolver.openInputStream(Uri.parse(item.uri))?.use { input -> file.outputStream().use { output ->
                var total=0L; val buffer=ByteArray(32768)
                while(true) { ensureActive(); val n=input.read(buffer); if(n<0) break; total+=n; require(total<=MAX_BYTES); output.write(buffer,0,n) }
            } } ?: throw IOException("Missing media")
            FileProvider.getUriForFile(context,"${context.packageName}.media",file)
        } catch(e:Exception) { file.delete(); if(e is java.io.FileNotFoundException || e is SecurityException) repository.markUnavailable(item.id); throw e }
    }
    companion object { const val MAX_BYTES=50L*1024*1024 }
}
