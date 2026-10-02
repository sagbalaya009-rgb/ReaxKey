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
    val sharedIds=listOf("whatsapp-business","whatsapp","telegram","downloads","pictures","phone")
    fun bucket(relativePath:String?):String {
        val segments=relativePath.orEmpty().lowercase().split('/').filter {it.isNotBlank()}
        fun packageMedia(id:String)=segments.take(3)==listOf("android","media",id)
        if(packageMedia("com.whatsapp.w4b") || segments.take(2)==listOf("whatsapp business","media")) return "whatsapp-business"
        if(packageMedia("com.whatsapp") || segments.take(2)==listOf("whatsapp","media")) return "whatsapp"
        if(packageMedia("org.telegram.messenger") || segments.firstOrNull()=="telegram" || segments.take(2)==listOf("pictures","telegram")) return "telegram"
        return when(segments.firstOrNull()) {"download","downloads"->"downloads";"pictures"->"pictures";else->"phone"}
    }
    fun allowedTreeId(id:String):Boolean {
        val path=id.substringAfter(':',"").trim('/').lowercase()
        return path.isNotEmpty() && path!="download" && path!="downloads" &&
            path!="android/data" && !path.startsWith("android/data/") &&
            path!="android/obb" && !path.startsWith("android/obb/") &&
            path.split('/').none {it==".."}
    }
}
