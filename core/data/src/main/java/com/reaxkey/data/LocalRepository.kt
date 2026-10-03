package com.reaxkey.data

import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.reaxkey.model.*
import com.reaxkey.search.Lexical
import kotlinx.coroutines.flow.map

class LocalRepository(private val db:ReaxDatabase):MediaRepository {
    private val dao=db.dao()
    override fun observe(query:MediaQuery)=dao.observe(sql(query)).map { rows -> rows.map(::model) }
    override fun sources()=dao.sources().map { it.map(::sourceModel) }
    override fun collections()=dao.collections().map { it.map { row -> Collection(row.id,row.name) } }
    override suspend fun source(id:String)=dao.source(id)?.let(::sourceModel)
    override suspend fun putSource(source:MediaSource)=dao.source(SourceRow(source.id,source.type,source.name,source.uri,source.health,source.enabled,source.permissionFlags,source.lastScan,source.lastSuccess,source.count))
    override suspend fun find(source:String,uri:String)=dao.find(source,uri)?.let(::model)
    override suspend fun item(id:Long)=dao.item(id)?.let(::model)
    override suspend fun index(item:MediaItem,generation:Long):Long=db.withTransaction {
        val old=dao.find(item.sourceId,item.uri) ?: dao.findUri(item.uri)
        if(old!=null && old.sourceId!=item.sourceId && old.available && dao.source(old.sourceId)?.enabled==true) return@withTransaction old.id
        val row=MediaRow(old?.id ?: 0,item.sourceId,item.uri,item.name,item.mime,old?.kind ?: item.kind,item.width,item.height,item.size,item.modified,item.animated,item.available,
            old?.label ?: item.label,old?.favorite ?: item.favorite,old?.usage ?: 0,old?.lastUsed ?: 0,item.owned,old?.removed ?: false,old?.tags ?: item.tags,old?.created ?: item.created,System.currentTimeMillis(),generation)
        val inserted=dao.upsert(row); val id=old?.id ?: inserted
        refresh(id)
        dao.source(item.sourceId)?.takeIf { it.type=="IMPORTED" }?.let { dao.source(it.copy(count=dao.sourceCount(it.id))) }
        id
    }
    private suspend fun refresh(id:Long) {
        val row=dao.item(id) ?: return
        val source=dao.source(row.sourceId)
        dao.search(SearchRow(id,Lexical.normalize(listOf(row.name,row.label,row.tags,source?.name ?: "",row.kind,row.mime,dao.collectionNames(id).joinToString(" ")).joinToString(" "))))
    }
    override suspend fun finishScan(id:String,generation:Long,health:String,count:Int)=db.withTransaction {
        val source=dao.source(id) ?: return@withTransaction
        if(!source.enabled) {dao.unavailableSource(id);return@withTransaction}
        if(health=="OK") dao.stale(id,generation) else dao.unavailableSource(id)
        dao.source(source.copy(health=health,lastScan=System.currentTimeMillis(),lastSuccess=if(health=="OK") System.currentTimeMillis() else source.lastSuccess,count=dao.sourceCount(id)))
    }
    override suspend fun markUnavailable(id:Long)=dao.unavailable(id)
    override suspend fun favorite(id:Long,value:Boolean)=dao.favorite(id,value)
    override suspend fun edit(id:Long,label:String,tags:String,kind:String)=db.withTransaction {
        dao.item(id)?.let { dao.upsert(it.copy(label=label.take(200),tags=tags.take(2000),kind=kind)); refresh(id) }; Unit
    }
    override suspend fun remove(id:Long)=db.withTransaction {
        dao.remove(id)
        dao.item(id)?.let { item -> dao.source(item.sourceId)?.let { dao.source(it.copy(count=dao.sourceCount(it.id))) } }
        Unit
    }
    override suspend fun recordUse(id:Long)=dao.use(id,System.currentTimeMillis())
    override suspend fun restore(id:Long)=db.withTransaction {
        dao.restore(id)
        dao.item(id)?.let {item->dao.source(item.sourceId)?.let {dao.source(it.copy(enabled=true,health="OK",count=dao.sourceCount(it.id)))}}
        Unit
    }
    override suspend fun createCollection(name:String) { if(name.isNotBlank()) dao.collection(CollectionRow(name=name.trim().take(100))) }
    override suspend fun membership(collection:Long,media:Long,add:Boolean)=db.withTransaction {
        if(add) dao.add(Membership(collection,media)) else dao.removeMembership(collection,media)
        refresh(media)
    }
    override suspend fun disconnect(id:String)=db.withTransaction { dao.source(id)?.let { dao.source(it.copy(enabled=false,health="DISCONNECTED")); dao.unavailableSource(id) }; Unit }
    override suspend fun diagnostics()="items=${dao.count()}\nsourceHealth=${dao.health().groupingBy { it }.eachCount()}\n"
    companion object {
        fun sql(q:MediaQuery):SimpleSQLiteQuery {
            val where=mutableListOf("m.available=1","m.removed=0","m.kind IN ('STICKER','GIF','ANIMATED_IMAGE')","m.sourceId IN (SELECT id FROM source WHERE enabled=1)"); val args=mutableListOf<Any>()
            val normalized=Lexical.normalize(q.text)
            if(normalized.isNotEmpty()) { where.add("m.id IN (SELECT rowid FROM media_fts WHERE media_fts MATCH ?)"); args.add(Lexical.match(q.text)) }
            when(q.filter) {
                "Favorites" -> where.add("m.favorite=1")
                "Recent" -> where.add("m.lastUsed>0")
                "GIFs" -> where.add("m.kind IN ('GIF','ANIMATED_IMAGE')")
                "Stickers" -> where.add("m.kind='STICKER'")
                "Images" -> where.add("m.kind='IMAGE'")
            }
            if(q.source.isNotEmpty()) { where.add("m.sourceId=?"); args.add(q.source) }
            if(q.collection!=0L) { where.add("m.id IN (SELECT mediaId FROM membership WHERE collectionId=?)"); args.add(q.collection) }
            val order=if(normalized.isEmpty()) "m.lastUsed DESC,m.favorite DESC,m.created DESC,m.id DESC" else {
                args.add(q.text.trim()); args.add(q.text.trim()+"%")
                "(CASE WHEN m.label=? COLLATE NOCASE THEN 1000 WHEN m.label LIKE ? THEN 500 ELSE 0 END + m.favorite*30 + MIN(m.usage,20) + CASE WHEN m.lastUsed > (strftime('%s','now')*1000-604800000) THEN 15 ELSE 0 END) DESC,m.created DESC,m.id DESC"
            }
            args.add(q.limit.coerceIn(1,200)); args.add(q.offset.coerceAtLeast(0))
            return SimpleSQLiteQuery("SELECT m.* FROM media m WHERE ${where.joinToString(" AND ")} ORDER BY $order LIMIT ? OFFSET ?",args.toTypedArray())
        }
        private fun model(r:MediaRow)=MediaItem(r.id,r.sourceId,r.uri,r.name,r.mime,r.kind,r.width,r.height,r.size,r.modified,r.animated,r.available,r.label,r.favorite,r.usage,r.lastUsed,r.owned,r.removed,r.tags,r.created)
        private fun sourceModel(r:SourceRow)=MediaSource(r.id,r.type,r.name,r.uri,r.health,r.enabled,r.permissionFlags,r.lastScan,r.lastSuccess,r.count)
    }
}
