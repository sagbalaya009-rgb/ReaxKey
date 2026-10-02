package com.reaxkey.keyboard

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.*
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.test.core.app.ApplicationProvider
import com.reaxkey.ime.RichContentSender
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class RichContentTest {
    @Test fun acceptedTypesUseContentUriAndTemporaryReadFlag() {
        for(mime in listOf("image/png","image/jpeg","image/gif","image/webp")) {
            var received:InputContentInfo?=null;var flags=0
            val connection=object:BaseInputConnection(View(ApplicationProvider.getApplicationContext()),true) {
                override fun commitContent(content:InputContentInfo,grant:Int,opts:Bundle?):Boolean {received=content;flags=grant;return true}
            }
            val editor=EditorInfo();EditorInfoCompat.setContentMimeTypes(editor,arrayOf(mime))
            val uri=Uri.parse("content://test/image")
            assertTrue(RichContentSender.commit(connection,editor,uri,mime,"test"))
            assertEquals(uri,received!!.contentUri);assertEquals(InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION,flags)
        }
    }
    @Test fun incompatibleFieldsAndFileUrisNeverReachConnection() {
        var calls=0
        val connection=object:BaseInputConnection(View(ApplicationProvider.getApplicationContext()),true) {
            override fun commitContent(content:InputContentInfo,flags:Int,opts:Bundle?):Boolean {calls++;return true}
        }
        val editor=EditorInfo()
        assertFalse(RichContentSender.commit(connection,editor,Uri.parse("content://test/image"),"image/png","test"))
        EditorInfoCompat.setContentMimeTypes(editor,arrayOf("image/png"))
        assertFalse(RichContentSender.commit(connection,editor,Uri.parse("content://test/image"),"image/gif","test"))
        assertFalse(RichContentSender.commit(connection,editor,Uri.parse("file:///image.png"),"image/png","test"))
        assertEquals(0,calls)
    }
    @Test fun rejectedCommitIsNotReportedAsSuccess() {
        val connection=object:BaseInputConnection(View(ApplicationProvider.getApplicationContext()),true) {
            override fun commitContent(content:InputContentInfo,flags:Int,opts:Bundle?)=false
        }
        val editor=EditorInfo();EditorInfoCompat.setContentMimeTypes(editor,arrayOf("image/*"))
        assertFalse(RichContentSender.commit(connection,editor,Uri.parse("content://test/image"),"image/png","test"))
    }
}
