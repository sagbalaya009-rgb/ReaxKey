package com.reaxkey.model

import org.junit.Assert.*
import org.junit.Test

class DiscoveryTest {
    @Test fun emptyAccessiblePhoneReportsNotFound() {
        assertEquals(DiscoveryStatus.NOT_FOUND,SharedMediaRecognizer("whatsapp").discover(DiscoverySnapshot(MediaAccessLevel.FULL)).single().status)
    }
    @Test fun deniedAccessRequestsOnlyGenericPhonePermission() {
        val snapshot=DiscoverySnapshot(MediaAccessLevel.NONE)
        assertEquals(DiscoveryStatus.PERMISSION_REQUIRED,SharedMediaRecognizer("phone").discover(snapshot).single().status)
        assertEquals(DiscoveryStatus.NOT_FOUND,SharedMediaRecognizer("whatsapp").discover(snapshot).single().status)
    }
    @Test fun countsAreEvidenceAndPartialNeverBecomesFull() {
        val result=SharedMediaRecognizer("whatsapp").discover(DiscoverySnapshot(MediaAccessLevel.SELECTED,mapOf("whatsapp" to 7))).single()
        assertEquals(7,result.count);assertEquals(DiscoveryStatus.PARTIALLY_AVAILABLE,result.status)
    }
    @Test fun broadSharedMediaNeedsExplicitSelectionBeforeAdding() {
        val shared=SharedMediaRecognizer("whatsapp").discover(DiscoverySnapshot(MediaAccessLevel.FULL,mapOf("whatsapp" to 1415))).single()
        val imported=SourceCandidate(MediaSource("imports","IMPORTED","Imported"),DiscoveryStatus.AVAILABLE,4,SourceConfidence.EXACT)
        assertEquals(SourceConfidence.MEDIUM,shared.confidence)
        assertFalse(shared.autoSelected())
        assertTrue(imported.autoSelected())
    }
    @Test fun independentRecognizersUseExclusiveCounts() {
        val paths=listOf("Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Stickers/","Download/","Pictures/","DCIM/")
        val counts=paths.groupingBy(SourceAttribution::bucket).eachCount()
        val snapshot=DiscoverySnapshot(MediaAccessLevel.FULL,counts)
        val results=SourceAttribution.sharedIds.flatMap {SharedMediaRecognizer(it).discover(snapshot)}
        assertEquals(4,results.sumOf {it.count ?: 0})
        assertEquals(4,results.count {it.status==DiscoveryStatus.AVAILABLE})
    }
    @Test fun filenameAndSubstringDoNotImpersonateApps() {
        for(path in listOf("Pictures/whatsapp-memes/","Download/not-whatsapp/","Pictures/whatsapp.png")) assertNotEquals("whatsapp",SourceAttribution.bucket(path))
        assertEquals("whatsapp-business",SourceAttribution.bucket("Android/media/com.whatsapp.w4b/WhatsApp Business/Media/"))
        assertEquals("telegram",SourceAttribution.bucket("Pictures/Telegram/"))
    }
    @Test fun duplicateGrantsCollapseWithoutHidingPermissionState() {
        val candidate=SourceCandidate(MediaSource("s","SAF_TREE","Folder","content://provider/tree/a"),DiscoveryStatus.AVAILABLE_REQUIRES_USER_GRANT)
        val results=ExistingSafGrantRecognizer().discover(DiscoverySnapshot(MediaAccessLevel.NONE,grants=listOf(candidate,candidate)))
        assertEquals(1,results.size);assertEquals(DiscoveryStatus.AVAILABLE_REQUIRES_USER_GRANT,results.single().status)
    }
    @Test fun existingGrantDoesNotRequestAnotherGrant() {
        val candidate=SourceCandidate(MediaSource("s","SAF_TREE","Folder","content://provider/tree/a"),DiscoveryStatus.AVAILABLE,0)
        assertEquals(DiscoveryStatus.AVAILABLE,ExistingSafGrantRecognizer().discover(DiscoverySnapshot(MediaAccessLevel.NONE,grants=listOf(candidate))).single().status)
    }
    @Test fun errorDoesNotInventACount() {
        val result=SharedMediaRecognizer("phone").discover(DiscoverySnapshot(MediaAccessLevel.FULL,sharedError=true)).single()
        assertEquals(DiscoveryStatus.ERROR,result.status);assertNull(result.count)
        assertEquals(DiscoveryStatus.NOT_FOUND,SharedMediaRecognizer("whatsapp").discover(DiscoverySnapshot(MediaAccessLevel.FULL,sharedError=true)).single().status)
    }
    @Test fun protectedAndRootLocationsAreRejected() {
        for(id in listOf("primary:","primary:Download","primary:Android/data","primary:Android/data/com.example","primary:Android/obb","primary:Pictures/../Android/data")) assertFalse(id,SourceAttribution.allowedTreeId(id))
        assertTrue(SourceAttribution.allowedTreeId("primary:Download/Reactions"))
        assertTrue(SourceAttribution.allowedTreeId("primary:Android/media/com.whatsapp/WhatsApp/Media"))
    }
    @Test fun hugeCountsAreNotTruncatedOrFabricated() {
        assertEquals(10000,SharedMediaRecognizer("pictures").discover(DiscoverySnapshot(MediaAccessLevel.FULL,mapOf("pictures" to 10000))).single().count)
    }
}
