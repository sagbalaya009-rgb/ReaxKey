package com.reaxkey.keyboard

import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import com.reaxkey.media.MediaAccess
import com.reaxkey.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class ImportSafetyTest {
    @Test fun identicalImportsDeduplicateAndIndexRemovalKeepsOriginal()=runBlocking {
        val app=ApplicationProvider.getApplicationContext<ReaxApplication>()
        org.junit.Assume.assumeTrue("AndroidX FileProvider requires Unix paths; exercised by Linux CI and device tests", File.separatorChar == '/')
        val original=File(app.filesDir,"imports/original.png");original.parentFile!!.mkdirs()
        original.outputStream().use {Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG,100,it)}
        val uri=FileProvider.getUriForFile(app,"${app.packageName}.media",original)
        val access=MediaAccess(app,app.repository)
        val first=access.importCopy(uri);val second=access.importCopy(uri)
        assertEquals(first,second);assertTrue(app.repository.item(first)!!.owned)
        app.repository.remove(first);assertTrue(original.exists())
        assertTrue(app.repository.observe(MediaQuery()).first().isEmpty())
    }
    @Test fun cannotDeleteExternalItems()=runBlocking {
        val app=ApplicationProvider.getApplicationContext<ReaxApplication>()
        val item=MediaItem(sourceId="external",uri="content://media/a",name="x",mime="image/png",kind="IMAGE")
        try {MediaAccess(app,app.repository).deleteOwned(item);fail("External deletion accepted")} catch(_:IllegalArgumentException) { }
    }
    @Test fun invalidImageDoesNotEnterIndex()=runBlocking {
        val app=ApplicationProvider.getApplicationContext<ReaxApplication>()
        org.junit.Assume.assumeTrue("AndroidX FileProvider requires Unix paths; exercised by Linux CI and device tests", File.separatorChar == '/')
        val file=File(app.filesDir,"imports/invalid.png");file.parentFile!!.mkdirs();file.writeText("not an image")
        val uri=FileProvider.getUriForFile(app,"${app.packageName}.media",file)
        try {MediaAccess(app,app.repository).importCopy(uri);fail("Invalid image accepted")} catch(_:java.io.IOException) { }
        assertTrue(file.exists())
    }
    @Test fun revokedTreeIsRecordedWithoutCrashing()=runBlocking {
        val app=ApplicationProvider.getApplicationContext<ReaxApplication>()
        val source=MediaSource("revoked","SAF_TREE","test","content://documents/tree/test")
        app.repository.putSource(source);MediaAccess(app,app.repository).scan(source)
        assertEquals("REVOKED",app.repository.source(source.id)!!.health)
    }
}

