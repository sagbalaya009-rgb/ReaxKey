package com.reaxkey.data

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.reaxkey.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class LargeLibraryTest {
    @Test fun boundedSearchAndRescanAtThreeLibrarySizes()=runBlocking {
        for(size in listOf(100,1000,10000)) {
            val db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),ReaxDatabase::class.java).build()
            try {
                val repo=LocalRepository(db);repo.putSource(MediaSource("s","IMPORTED","Generated"))
                val indexing=measureTimeMillis {db.withTransaction {repeat(size) {n->repo.index(MediaItem(sourceId="s",uri="content://generated/$n",name="reaction-$n.png",mime="image/png",kind="STICKER"),1)}}}
                val search=measureTimeMillis {assertEquals(25,repo.observe(MediaQuery(text="reaction",limit=25)).first().size)}
                val rescan=measureTimeMillis {db.withTransaction {repeat(size) {n->repo.index(MediaItem(sourceId="s",uri="content://generated/$n",name="reaction-$n.png",mime="image/png",kind="STICKER"),2)}}}
                assertEquals(size,db.dao().count())
                println("Generated rows=$size indexMs=$indexing searchMs=$search rescanMs=$rescan; desktop SQLite only, not physical media decoding")
            } finally {db.close()}
        }
    }
}
