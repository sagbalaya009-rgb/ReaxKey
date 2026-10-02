package com.reaxkey.keyboard

import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.reaxkey.media.MediaAccess
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Runs against the installed app's real provider and image decoder. */
@RunWith(AndroidJUnit4::class)
class DeviceMediaTest {
    @Test fun supportedFormatsImportWithRealDecoders()=runBlocking {
        val app=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as ReaxApplication
        val access=MediaAccess(app,app.repository)
        val bitmap=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.CYAN)
        val formats=listOf("png" to Bitmap.CompressFormat.PNG,"jpg" to Bitmap.CompressFormat.JPEG,"webp" to Bitmap.CompressFormat.WEBP)
        val files=formats.map { (extension,format) ->
            File(app.filesDir,"imports/QA-sample.$extension").apply { parentFile!!.mkdirs();outputStream().use {bitmap.compress(format,90,it)} }
        }+File(app.filesDir,"imports/QA-sample.gif").apply {
            writeBytes(android.util.Base64.decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7",android.util.Base64.DEFAULT))
        }
        bitmap.recycle()
        try {
            for(file in files) {
                val id=access.importCopy(FileProvider.getUriForFile(app,"${app.packageName}.media",file))
                val item=app.repository.item(id)!!
                assertTrue(item.width>0 && item.height>0)
                assertEquals(when(file.extension){"jpg"->"image/jpeg";else->"image/${file.extension}"},item.mime)
                if(InstrumentationRegistry.getArguments().getString("retainFixtures")!="true") access.deleteOwned(item)
            }
        } finally {files.forEach {it.delete()} }
    }
    @Test fun importDeduplicatesAndOutgoingCopyRemainsReadable() = runBlocking {
        val app=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as ReaxApplication
        val original=File(app.filesDir,"imports/device-fixture.png")
        original.parentFile!!.mkdirs()
        val bitmap=Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.MAGENTA)
        original.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }
        bitmap.recycle()
        val access=MediaAccess(app,app.repository)
        try {
            val uri=FileProvider.getUriForFile(app,"${app.packageName}.media",original)
            val id=access.importCopy(uri)
            assertEquals(id,access.importCopy(uri))
            val item=app.repository.item(id)!!
            assertEquals("image/png",item.mime)
            val outgoing=access.sendUri(item)
            assertTrue(app.contentResolver.openInputStream(outgoing)!!.use { it.readBytes().isNotEmpty() })
            app.repository.remove(id)
            assertTrue(original.exists())
            access.deleteOwned(item)
        } finally { original.delete() }
    }
    @Test fun providerRefusesPrivateDatabaseAndTraversal() {
        val app=InstrumentationRegistry.getInstrumentation().targetContext
        for(file in listOf(File(app.filesDir,"private.txt"),File(app.filesDir,"imports/../private.txt"))) {
            try { FileProvider.getUriForFile(app,"${app.packageName}.media",file);fail("Private file exposed") }
            catch(_:IllegalArgumentException) { }
        }
    }
}
