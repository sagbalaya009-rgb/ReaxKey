package com.reaxkey.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class MigrationTest {
    @Test fun versionOneMigratesWithoutLosingUserMetadata()=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val name="migration-test.db"; context.deleteDatabase(name)
        val path=context.getDatabasePath(name); path.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path,null).use { old ->
            javaClass.classLoader!!.getResourceAsStream("schema-v1.sql")!!.bufferedReader().use { it.readText() }.split(';').filter(String::isNotBlank).forEach { old.execSQL(it) }
            old.execSQL("INSERT INTO media (id,sourceId,uri,name,mime,kind,width,height,size,modified,animated,available,label,favorite,usage,lastUsed,owned,removed,tags,created,indexed,generation,durationMs) VALUES (1,'s','content://test/a','a.png','image/png','IMAGE',1,1,10,1,0,1,'Keep me',1,3,10,0,0,'happy',1,1,1,0)")
            old.version=1
        }
        val migrated=Room.databaseBuilder(context,ReaxDatabase::class.java,name).addMigrations(ReaxDatabase.MIGRATION_1_2).build()
        try {
            val row=migrated.dao().item(1)!!
            assertEquals("Keep me",row.label); assertTrue(row.favorite); assertEquals(3L,row.usage); assertEquals("USER",row.tagOrigin)
        } finally { migrated.close(); context.deleteDatabase(name) }
    }
}
