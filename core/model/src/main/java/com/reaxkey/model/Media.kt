package com.reaxkey.model

import kotlinx.coroutines.flow.Flow

interface ReaxHost { val repository:MediaRepository }

data class MediaItem(val id:Long=0,val sourceId:String,val uri:String,val name:String,val mime:String,val kind:String,
    val width:Int=0,val height:Int=0,val size:Long=0,val modified:Long=0,val animated:Boolean=false,
    val available:Boolean=true,val label:String="",val favorite:Boolean=false,val usage:Long=0,val lastUsed:Long=0,
    val owned:Boolean=false,val removed:Boolean=false,val tags:String="",val created:Long=System.currentTimeMillis())
data class MediaSource(val id:String,val type:String,val name:String,val uri:String="",val health:String="OK",val enabled:Boolean=true,
    val permissionFlags:Int=0,val lastScan:Long=0,val lastSuccess:Long=0,val count:Int=0)
data class Collection(val id:Long=0,val name:String)
data class MediaQuery(val text:String="",val filter:String="All",val source:String="",val collection:Long=0,val limit:Int=100,val offset:Int=0)
interface MediaRepository {
    fun observe(query:MediaQuery):Flow<List<MediaItem>>
    fun sources():Flow<List<MediaSource>>
    fun collections():Flow<List<Collection>>
    suspend fun source(id:String):MediaSource?
    suspend fun putSource(source:MediaSource)
    suspend fun find(source:String,uri:String):MediaItem?
    suspend fun item(id:Long):MediaItem?
    suspend fun index(item:MediaItem,generation:Long):Long
    suspend fun finishScan(id:String,generation:Long,health:String,count:Int)
    suspend fun markUnavailable(id:Long)
    suspend fun favorite(id:Long,value:Boolean)
    suspend fun edit(id:Long,label:String,tags:String,kind:String)
    suspend fun remove(id:Long)
    suspend fun restore(id:Long)
    suspend fun recordUse(id:Long)
    suspend fun createCollection(name:String)
    suspend fun membership(collection:Long,media:Long,add:Boolean)
    suspend fun disconnect(id:String)
    suspend fun diagnostics():String
}
interface SemanticMetadataProvider { suspend fun analyze(item:MediaItem):List<SemanticTag> }
data class SemanticTag(val value:String,val confidence:Float,val origin:String="FUTURE_VISION")
class DisabledSemanticProvider:SemanticMetadataProvider { override suspend fun analyze(item:MediaItem)=emptyList<SemanticTag>() }

object MediaRules {
    val supported=setOf("image/png","image/jpeg","image/gif","image/webp")
    fun accepts(mime:String,accepted:List<String>)=mime in supported && accepted.any { it==mime || it=="image/*" || it=="*/*" }
    fun classify(mime:String,animated:Boolean,width:Int,height:Int):String = when {
        mime=="image/gif" -> "GIF"
        animated -> "ANIMATED_IMAGE"
        mime in listOf("image/png","image/webp") && width in 1..512 && height in 1..512 -> "STICKER"
        else -> "IMAGE"
    }
    fun contentUri(uri:String)=uri.startsWith("content://") && uri.length>10
}
