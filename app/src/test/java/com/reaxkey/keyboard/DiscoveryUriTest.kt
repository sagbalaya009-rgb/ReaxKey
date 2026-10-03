package com.reaxkey.keyboard

import android.net.Uri
import com.reaxkey.media.SourceDiscoveryEngine
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class DiscoveryUriTest {
    @Test fun invalidAndPrivateCandidatesNeverOpenAsGuidedLocations() {
        for(value in listOf("file:///sdcard/a","https://example.com","content://provider/item/a","content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata"))
            assertFalse(value,SourceDiscoveryEngine.validTree(Uri.parse(value)))
    }
    @Test fun opaqueProviderTreesAndAllowedExternalTreesRemainSupported() {
        assertTrue(SourceDiscoveryEngine.validTree(Uri.parse("content://provider/tree/opaque-id")))
        assertTrue(SourceDiscoveryEngine.validTree(Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADownload%2FReactions")))
    }
}
