package com.reaxkey.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import com.reaxkey.model.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName="media",indices=[Index(value=["sourceId","uri"],unique=true),Index("available"),Index("lastUsed")])
data class MediaRow(@PrimaryKey(autoGenerate=true) val id:Long=0,val sourceId:String,val uri:String,val name:String,val mime:String,val kind:String,
 val width:Int=0,val height:Int=0,val size:Long=0,val modified:Long=0,val animated:Boolean=false,val available:Boolean=true,
 val label:String="",val favorite:Boolean=false,val usage:Long=0,val lastUsed:Long=0,val owned:Boolean=false,val removed:Boolean=false,
 val tags:String="",val created:Long=0,val indexed:Long=0,val generation:Long=0,val durationMs:Long=0,val contentHash:String?=null,
 @ColumnInfo(defaultValue="'USER'") val tagOrigin:String="USER")
@Entity(tableName="source") data class SourceRow(@PrimaryKey val id:String,val type:String,val name:String,val uri:String="",val health:String="OK",val enabled:Boolean=true,val permissionFlags:Int=0,val lastScan:Long=0,val lastSuccess:Long=0,val count:Int=0)
@Entity(tableName="collection") data class CollectionRow(@PrimaryKey(autoGenerate=true) val id:Long=0,val name:String,val created:Long=System.currentTimeMillis(),val sortOrder:Int=0)
@Entity(tableName="membership",primaryKeys=["collectionId","mediaId"],indices=[Index("mediaId")],foreignKeys=[ForeignKey(entity=CollectionRow::class,parentColumns=["id"],childColumns=["collectionId"],onDelete=ForeignKey.CASCADE),ForeignKey(entity=MediaRow::class,parentColumns=["id"],childColumns=["mediaId"],onDelete=ForeignKey.CASCADE)])
data class Membership(val collectionId:Long,val mediaId:Long,val added:Long=System.currentTimeMillis())
@Fts4 @Entity(tableName="media_fts") data class SearchRow(@PrimaryKey @ColumnInfo(name="rowid") val id:Long,val text:String)

@Dao interface MediaDao {
    @RawQuery(observedEntities=[MediaRow::class,SearchRow::class,Membership::class,SourceRow::class]) fun observe(query:SupportSQLiteQuery):Flow<List<MediaRow>>
    @Query("SELECT * FROM source ORDER BY name") fun sources():Flow<List<SourceRow>>
    @Query("SELECT * FROM collection ORDER BY sortOrder,name") fun collections():Flow<List<CollectionRow>>
    @Query("SELECT * FROM source WHERE id=:id") suspend fun source(id:String):SourceRow?
    @Upsert suspend fun source(source:SourceRow)
    @Query("SELECT * FROM media WHERE sourceId=:source AND uri=:uri") suspend fun find(source:String,uri:String):MediaRow?
    @Query("SELECT * FROM media WHERE uri=:uri ORDER BY owned DESC,id LIMIT 1") suspend fun findUri(uri:String):MediaRow?
    @Query("SELECT * FROM media WHERE id=:id") suspend fun item(id:Long):MediaRow?
    @Upsert suspend fun upsert(item:MediaRow):Long
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun search(item:SearchRow)
    @Query("UPDATE media SET available=0 WHERE sourceId=:source AND generation!=:generation") suspend fun stale(source:String,generation:Long)
    @Query("UPDATE media SET available=0 WHERE sourceId=:source") suspend fun unavailableSource(source:String)
    @Query("UPDATE media SET available=0 WHERE id=:id") suspend fun unavailable(id:Long)
    @Query("UPDATE media SET favorite=:value WHERE id=:id") suspend fun favorite(id:Long,value:Boolean)
    @Query("UPDATE media SET removed=1 WHERE id=:id") suspend fun remove(id:Long)
    @Query("UPDATE media SET removed=0,available=1 WHERE id=:id") suspend fun restore(id:Long)
    @Query("UPDATE media SET usage=usage+1,lastUsed=:now WHERE id=:id") suspend fun use(id:Long,now:Long)
    @Insert suspend fun collection(value:CollectionRow):Long
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun add(value:Membership)
    @Query("DELETE FROM membership WHERE collectionId=:collection AND mediaId=:media") suspend fun removeMembership(collection:Long,media:Long)
    @Query("SELECT name FROM collection JOIN membership ON collection.id=membership.collectionId WHERE mediaId=:id") suspend fun collectionNames(id:Long):List<String>
    @Query("SELECT COUNT(*) FROM media WHERE removed=0") suspend fun count():Int
    @Query("SELECT COUNT(*) FROM media WHERE sourceId=:source AND removed=0 AND available=1") suspend fun sourceCount(source:String):Int
    @Query("SELECT health FROM source") suspend fun health():List<String>
}

@Database(entities=[MediaRow::class,SourceRow::class,CollectionRow::class,Membership::class,SearchRow::class],version=2,exportSchema=true)
abstract class ReaxDatabase:RoomDatabase() {
    abstract fun dao():MediaDao
    companion object {
        val MIGRATION_1_2=object:Migration(1,2) { override fun migrate(db:SupportSQLiteDatabase) { db.execSQL("ALTER TABLE media ADD COLUMN tagOrigin TEXT NOT NULL DEFAULT 'USER'") } }
        fun open(context:Context)=Room.databaseBuilder(context,ReaxDatabase::class.java,"reaxkey.db").addMigrations(MIGRATION_1_2).build()
    }
}
