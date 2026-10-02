package com.reaxkey.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.reaxkey.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class RepositoryTest {
    private lateinit var db:ReaxDatabase
    private lateinit var repo:LocalRepository
    @Before fun setup() { db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),ReaxDatabase::class.java).allowMainThreadQueries().build(); repo=LocalRepository(db) }
    @After fun close() { db.close() }
    @Test fun scanFinishingAfterDisconnectCannotRestoreSource()=runBlocking {
        repo.putSource(MediaSource("s","SAF_TREE","Folder"))
        val item=MediaItem(sourceId="s",uri="content://test/a",name="a",mime="image/png",kind="IMAGE")
        repo.disconnect("s")
        repo.index(item,2)
        assertTrue(repo.observe(MediaQuery()).first().isEmpty())
        repo.finishScan("s",2,"OK",1)
        assertEquals("DISCONNECTED",repo.source("s")!!.health)
        assertFalse(repo.item(repo.find("s",item.uri)!!.id)!!.available)
    }
    @Test fun importedSourceCountTracksDeduplicationAndRemoval()=runBlocking {
        repo.putSource(MediaSource("imports","IMPORTED","Imports"))
        val item=MediaItem(sourceId="imports",uri="content://test/a",name="a",mime="image/png",kind="IMAGE")
        val id=repo.index(item,1);repo.index(item,2)
        assertEquals(1,repo.source("imports")!!.count)
        repo.remove(id)
        assertEquals(0,repo.source("imports")!!.count)
    }
    @Test fun rescanPreservesFavoriteAndRemoval()=runBlocking {
        repo.putSource(MediaSource("s","SAF_TREE","Folder"))
        val item=MediaItem(sourceId="s",uri="content://test/a",name="a.png",mime="image/png",kind="STICKER")
        val id=repo.index(item,1); repo.favorite(id,true); repo.remove(id); repo.index(item,2)
        assertTrue(repo.item(id)!!.favorite); assertTrue(repo.item(id)!!.removed)
        assertTrue(repo.observe(MediaQuery()).first().isEmpty())
    }
    @Test fun ftsIncludesSourceTagsCollectionsAndFilters()=runBlocking {
        repo.putSource(MediaSource("s","SAF_TREE","Holiday"))
        val id=repo.index(MediaItem(sourceId="s",uri="content://test/a",name="café.png",mime="image/png",kind="STICKER"),1)
        repo.edit(id,"Laugh","happy","STICKER"); repo.createCollection("Friends"); repo.membership(repo.collections().first().single().id,id,true)
        for(query in listOf("cafe","holiday","happy","friends","sticker")) assertEquals(id,repo.observe(MediaQuery(text=query)).first().single().id)
        assertTrue(repo.observe(MediaQuery(filter="GIFs")).first().isEmpty())
    }
    @Test fun failedSourceIsHiddenAndRecoveryRestoresIt()=runBlocking {
        repo.putSource(MediaSource("s","SAF_TREE","Folder"))
        val item=MediaItem(sourceId="s",uri="content://test/a",name="a",mime="image/png",kind="IMAGE")
        repo.index(item,1); repo.finishScan("s",1,"REVOKED",0)
        assertTrue(repo.observe(MediaQuery()).first().isEmpty())
        repo.index(item,2); repo.finishScan("s",2,"OK",1)
        assertEquals(1,repo.observe(MediaQuery()).first().size)
    }
}
