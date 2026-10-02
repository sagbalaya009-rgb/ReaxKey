package com.reaxkey.keyboard

import android.content.*
import android.database.*
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import com.reaxkey.media.SourceDiscoveryEngine
import com.reaxkey.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.toList
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class DiscoveryEngineTest {
    class MetadataProvider(private val paths:List<String>,private val denied:Boolean=false):ContentProvider() {
        var queries=0
        override fun onCreate()=true
        override fun query(uri:Uri,projection:Array<out String>?,selection:String?,args:Array<out String>?,sort:String?):Cursor {
            queries++;if(denied) throw SecurityException()
            return MatrixCursor(projection ?: emptyArray()).apply {paths.forEach {path -> addRow((projection ?: emptyArray()).map {if(it==MediaStore.Images.Media.MIME_TYPE) "image/webp" else path}.toTypedArray())}}
        }
        override fun getType(uri:Uri)="image/webp"
        override fun insert(uri:Uri,values:ContentValues?):Uri?=null
        override fun delete(uri:Uri,selection:String?,args:Array<out String>?)=0
        override fun update(uri:Uri,values:ContentValues?,selection:String?,args:Array<out String>?)=0
    }
    private fun provider(paths:List<String>,denied:Boolean=false):MetadataProvider {
        val p=MetadataProvider(paths,denied);ShadowContentResolver.registerProviderInternal("media",p);return p
    }
    @Test fun deniedMetadataNeverFabricatesAppSources()=runBlocking {
        val app=ApplicationProvider.getApplicationContext<ReaxApplication>();provider(emptyList(),true)
        val results=SourceDiscoveryEngine(app,app.repository).discover().toList()
        assertEquals(DiscoveryStatus.PERMISSION_REQUIRED,results.last {it.source.uri=="phone"}.status)
        assertTrue(results.filter {it.source.uri=="whatsapp"}.all {it.status==DiscoveryStatus.NOT_FOUND})
    }
    @Test fun revokedMediaPermissionCannotBecomeHealthyEmptySource()=runBlocking {
        val app=ApplicationProvider.getApplicationContext<ReaxApplication>();provider(emptyList())
        val source=MediaSource("discovered-phone","MEDIASTORE","Phone","phone")
        app.repository.putSource(source)
        com.reaxkey.media.MediaAccess(app,app.repository).scan(source)
        assertEquals("REVOKED",app.repository.source(source.id)!!.health)
    }
    @Test fun metadataScannedOnceAndPublishedProgressivelyAtScale()=runBlocking {
        val app=ApplicationProvider.getApplicationContext<ReaxApplication>()
        val p=provider(List(10000){"Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Stickers/"}+listOf("Download/","Pictures/"))
        val results=SourceDiscoveryEngine(app,app.repository).discover().toList()
        assertEquals(1,p.queries)
        assertEquals(10000,results.last {it.source.uri=="whatsapp"}.count)
        assertTrue(results.count {it.source.uri=="whatsapp"}>1)
        assertEquals(1,results.last {it.source.uri=="downloads"}.count)
    }
    @Test fun selectedPermissionIsReflectedInEveryAccessibleSource()=runBlocking {
        val app=ApplicationProvider.getApplicationContext<ReaxApplication>()
        Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        provider(listOf("Pictures/"))
        val results=SourceDiscoveryEngine(app,app.repository).discover().toList()
        assertEquals(DiscoveryStatus.PARTIALLY_AVAILABLE,results.last {it.source.uri=="pictures"}.status)
    }
    @Test fun missingGrantAndInvalidCandidateHaveDifferentStates()=runBlocking {
        val app=ApplicationProvider.getApplicationContext<ReaxApplication>();provider(emptyList())
        app.repository.putSource(MediaSource("saved","SAF_TREE","Saved","content://provider/tree/old"))
        app.repository.putSource(MediaSource("invalid","SAF_TREE","Invalid","file:///private"))
        val results=SourceDiscoveryEngine(app,app.repository).discover().toList()
        assertEquals(DiscoveryStatus.AVAILABLE_REQUIRES_USER_GRANT,results.single {it.source.id=="saved"}.status)
        assertEquals(DiscoveryStatus.PRIVATE_OR_INACCESSIBLE,results.single {it.source.id=="invalid"}.status)
        assertEquals("REVOKED",app.repository.source("saved")!!.health)
        assertEquals("MISSING",app.repository.source("invalid")!!.health)
    }
}
