package com.reaxkey.model

enum class DiscoveryStatus { AVAILABLE, AVAILABLE_REQUIRES_USER_GRANT, PARTIALLY_AVAILABLE, NOT_FOUND, PRIVATE_OR_INACCESSIBLE, PERMISSION_REQUIRED, ERROR }
enum class SourceConfidence { EXACT, HIGH, MEDIUM, UNKNOWN }
enum class MediaAccessLevel { FULL, SELECTED, NONE }
data class SourceCandidate(val source:MediaSource,val status:DiscoveryStatus,val count:Int?=null,val confidence:SourceConfidence=SourceConfidence.UNKNOWN,val initialUri:String?=null)
fun SourceCandidate.autoSelected():Boolean=confidence==SourceConfidence.EXACT
data class DiscoverySnapshot(val access:MediaAccessLevel,val sharedCounts:Map<String,Int> = emptyMap(),val grants:List<SourceCandidate> = emptyList(),val imports:SourceCandidate?=null,val sharedError:Boolean=false)
interface MediaSourceRecognizer { val sourceId:String; fun discover(snapshot:DiscoverySnapshot):List<SourceCandidate> }

class SharedMediaRecognizer(override val sourceId:String):MediaSourceRecognizer {
    override fun discover(snapshot:DiscoverySnapshot):List<SourceCandidate> {
        val count=snapshot.sharedCounts[sourceId] ?: 0
        val status=when {
            snapshot.sharedError && (sourceId=="phone" || count>0) -> DiscoveryStatus.ERROR
            count>0 && snapshot.access==MediaAccessLevel.SELECTED -> DiscoveryStatus.PARTIALLY_AVAILABLE
            count>0 -> DiscoveryStatus.AVAILABLE
            sourceId=="phone" && snapshot.access==MediaAccessLevel.NONE -> DiscoveryStatus.PERMISSION_REQUIRED
            else -> DiscoveryStatus.NOT_FOUND
        }
        return listOf(SourceCandidate(MediaSource("discovered-$sourceId","MEDIASTORE",sourceId,sourceId),status,
            if(status==DiscoveryStatus.PERMISSION_REQUIRED || status==DiscoveryStatus.ERROR) null else count,
            if(sourceId=="phone") SourceConfidence.UNKNOWN else SourceConfidence.MEDIUM))
    }
}
class ExistingSafGrantRecognizer:MediaSourceRecognizer {
    override val sourceId="grants"
    override fun discover(snapshot:DiscoverySnapshot)=snapshot.grants.distinctBy {it.source.uri}
}
class ReaxKeyImportRecognizer:MediaSourceRecognizer {
    override val sourceId="imports"
    override fun discover(snapshot:DiscoverySnapshot)=listOfNotNull(snapshot.imports)
}

/** Metadata path segments, never file-name guesses. Buckets partition shared media. */
object SourceAttribution {
    val sharedIds=listOf("whatsapp-business","whatsapp","phone")
    // A metadata hint narrows the provider query; bucket() still verifies exact path segments.
    val stickerPathHints=listOf("%/WhatsApp Stickers/%","%/WhatsApp Business Stickers/%")
    fun bucket(relativePath:String?):String {
        val segments=relativePath.orEmpty().lowercase().split('/').filter {it.isNotBlank()}
        val marker=segments.lastIndexOf("media")
        if(marker<0 || marker+1>=segments.size) return "none"
        val sticker=segments[marker+1]
        if(sticker !in listOf("whatsapp stickers","whatsapp business stickers")) return "none"
        val before=segments.take(marker)
        if(before==listOf("whatsapp") || before==listOf("android","media","com.whatsapp","whatsapp"))
            return if(sticker=="whatsapp stickers") "whatsapp" else "none"
        if(before==listOf("whatsapp business") || before==listOf("android","media","com.whatsapp.w4b","whatsapp business"))
            return "whatsapp-business"
        return "none"
    }
    fun allowedTreeId(id:String):Boolean {
        val path=id.substringAfter(':',"").trim('/').lowercase()
        return path.isNotEmpty() && path!="download" && path!="downloads" &&
            path!="android/data" && !path.startsWith("android/data/") &&
            path!="android/obb" && !path.startsWith("android/obb/") &&
            path.split('/').none {it==".."}
    }
}
