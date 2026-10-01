package com.reaxkey.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.reaxkey.model.*
import com.reaxkey.model.Collection
import com.reaxkey.ui.R
import kotlinx.coroutines.launch

val filters=listOf("All" to R.string.all,"Stickers" to R.string.stickers,"GIFs" to R.string.gifs,"Images" to R.string.images,"Favorites" to R.string.favorites,"Recent" to R.string.recent)

@Composable fun LibraryScreen(repo:MediaRepository,collection:Long=0,deleteOwned:(MediaItem)->Unit) {
    var query by rememberSaveable { mutableStateOf("") }; var filter by rememberSaveable { mutableStateOf("All") }; var offset by rememberSaveable { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<MediaItem?>(null) }
    val flow=remember(query,filter,collection,offset) { repo.observe(MediaQuery(query,filter,collection=collection,offset=offset)) }
    val items by flow.collectAsState(emptyList())
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.library_title),style=MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.library_subtitle),style=MaterialTheme.typography.bodyMedium)
        OutlinedTextField(query,{query=it;offset=0},label={Text(stringResource(R.string.search_library))},singleLine=true,modifier=Modifier.fillMaxWidth())
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) { filters.forEach { (key,label) -> FilterChip(filter==key,{filter=key;offset=0},label={Text(stringResource(label))}) } }
        if(items.isEmpty()) Text(stringResource(R.string.empty_library),modifier=Modifier.weight(1f))
        else LazyVerticalGrid(GridCells.Adaptive(104.dp),modifier=Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            items(items,key={it.id}) { item -> Card(Modifier.clickable { selected=item }) {
                AsyncImage(model=item.uri,contentDescription=item.label.ifBlank { item.name },modifier=Modifier.fillMaxWidth().aspectRatio(1f),contentScale=ContentScale.Fit)
                Text(item.label.ifBlank { item.name },maxLines=1,modifier=Modifier.padding(8.dp),style=MaterialTheme.typography.labelSmall)
            } }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            TextButton(onClick={offset=(offset-100).coerceAtLeast(0)},enabled=offset>0) { Text(stringResource(R.string.previous_page)) }
            TextButton(onClick={offset+=100},enabled=items.size==100) { Text(stringResource(R.string.next_page)) }
        }
    }
    selected?.let { item -> Details(repo,item,{selected=null},deleteOwned) }
}

@Composable private fun Details(repo:MediaRepository,item:MediaItem,close:()->Unit,deleteOwned:(MediaItem)->Unit) {
    var label by remember(item.id) { mutableStateOf(item.label) }; var tags by remember(item.id) { mutableStateOf(item.tags) }; var kind by remember(item.id) { mutableStateOf(item.kind) }
    var confirm by remember { mutableStateOf(false) }; val scope=rememberCoroutineScope()
    val collections by remember { repo.collections() }.collectAsState(emptyList())
    AlertDialog(onDismissRequest=close,title={Text(stringResource(R.string.details))},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        AsyncImage(item.uri,item.name,modifier=Modifier.fillMaxWidth().height(160.dp))
        Text(stringResource(R.string.media_info,item.mime,item.width,item.height,item.size/1024))
        OutlinedTextField(label,{label=it},label={Text(stringResource(R.string.label))})
        OutlinedTextField(tags,{tags=it},label={Text(stringResource(R.string.tags))})
        Row(Modifier.horizontalScroll(rememberScrollState())) { listOf("STICKER" to R.string.stickers,"GIF" to R.string.gifs,"IMAGE" to R.string.images).forEach { (value,text) -> FilterChip(kind==value,{kind=value},label={Text(stringResource(text))}) } }
        TextButton(onClick={scope.launch { repo.favorite(item.id,!item.favorite);close() }}) { Text(stringResource(if(item.favorite) R.string.unfavorite else R.string.favorite)) }
        collections.forEach { collection ->
            Text(collection.name)
            Row { TextButton(onClick={scope.launch { repo.membership(collection.id,item.id,true) }}) { Text(stringResource(R.string.add_collection)) }; TextButton(onClick={scope.launch { repo.membership(collection.id,item.id,false) }}) { Text(stringResource(R.string.remove_collection)) } }
        }
        TextButton(onClick={scope.launch { repo.remove(item.id);close() }}) { Text(stringResource(R.string.remove_index)) }
        if(item.owned) TextButton(onClick={confirm=true}) { Text(stringResource(R.string.delete_copy)) }
    }},confirmButton={TextButton(onClick={scope.launch { repo.edit(item.id,label,tags,kind);close() }}) {Text(stringResource(R.string.save))}},dismissButton={TextButton(onClick=close){Text(stringResource(R.string.close))}})
    if(confirm) AlertDialog(onDismissRequest={confirm=false},text={Text(stringResource(R.string.delete_confirm))},confirmButton={TextButton(onClick={deleteOwned(item);close()}){Text(stringResource(R.string.delete_copy))}},dismissButton={TextButton(onClick={confirm=false}){Text(stringResource(R.string.cancel))}})
}

@Composable fun CollectionsScreen(repo:MediaRepository,deleteOwned:(MediaItem)->Unit) {
    val collections by remember {repo.collections()}.collectAsState(emptyList()); var name by remember {mutableStateOf("")}; var selected by remember {mutableStateOf<Collection?>(null)}; val scope=rememberCoroutineScope()
    if(selected!=null) Column { TextButton(onClick={selected=null}){Text(stringResource(R.string.back))}; LibraryScreen(repo,selected!!.id,deleteOwned) }
    else Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.collections),style=MaterialTheme.typography.headlineMedium)
        OutlinedTextField(name,{name=it},label={Text(stringResource(R.string.collection_name))})
        Button(onClick={scope.launch {repo.createCollection(name);name=""}},enabled=name.isNotBlank()){Text(stringResource(R.string.create_collection))}
        if(collections.isEmpty()) Text(stringResource(R.string.no_collections))
        LazyColumn { items(collections,key={it.id}) { collection -> ListItem(headlineContent={Text(collection.name)},modifier=Modifier.clickable {selected=collection}) } }
    }
}

@Composable fun SourcesScreen(repo:MediaRepository,connect:()->Unit,importFiles:()->Unit,shared:()->Unit,rescan:(MediaSource)->Unit,disconnect:(MediaSource)->Unit) {
    val sources by remember {repo.sources()}.collectAsState(emptyList())
    LazyColumn(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Text(stringResource(R.string.sources),style=MaterialTheme.typography.headlineMedium) }
        item { Text(stringResource(R.string.storage_truth)) }
        item { Button(onClick=connect){Text(stringResource(R.string.connect_folder))} }
        item { OutlinedButton(onClick=importFiles){Text(stringResource(R.string.import_files))} }
        item { OutlinedButton(onClick=shared){Text(stringResource(R.string.scan_shared))} }
        items(sources,key={it.id}) { source -> Card { Column(Modifier.padding(16.dp)) {
            Text(source.name,style=MaterialTheme.typography.titleMedium)
            Text(stringResource(when(source.health){"OK"->R.string.source_ready;"SCANNING"->R.string.source_scanning;"REVOKED"->R.string.source_revoked;"DISCONNECTED"->R.string.source_disconnected;"MISSING"->R.string.source_missing;else->R.string.source_error}))
            Text(stringResource(R.string.source_count,source.count))
            Row { TextButton(onClick={rescan(source)}){Text(stringResource(if(source.enabled) R.string.rescan else R.string.reconnect))}; if(source.enabled) TextButton(onClick={disconnect(source)}){Text(stringResource(R.string.disconnect))} }
        } } }
    }
}
