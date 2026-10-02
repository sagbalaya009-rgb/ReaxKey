package com.reaxkey.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.reaxkey.model.*
import com.reaxkey.ui.R

@Composable
fun StickerHomeScreen(repo:MediaRepository,candidates:List<SourceCandidate>,checking:Boolean,
    import:()->Unit,refresh:()->Unit,folder:()->Unit,grant:()->Unit,add:(SourceCandidate)->Unit) {
    var offset by remember { mutableIntStateOf(0) }
    val flow=remember(offset) { repo.observe(MediaQuery(limit=200,offset=offset)) }
    val items by flow.collectAsState(emptyList())
    val sources by remember {repo.sources()}.collectAsState(emptyList())
    val found=candidates.filter {it.source.type=="MEDIASTORE" && it.source.uri in listOf("whatsapp","whatsapp-business") &&
        it.status in listOf(DiscoveryStatus.AVAILABLE,DiscoveryStatus.PARTIALLY_AVAILABLE) && (it.count ?: 0)>0 &&
        sources.none {source->source.id==it.source.id && source.enabled}}
    val needsPermission=candidates.any {it.source.uri=="phone" && it.status==DiscoveryStatus.PERMISSION_REQUIRED}
    var preview by remember {mutableStateOf<MediaItem?>(null)}
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.app_name),style=MaterialTheme.typography.headlineMedium,modifier=Modifier.padding(top=12.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick=import){Text(stringResource(R.string.import_stickers))}
            OutlinedButton(onClick=refresh,enabled=!checking){Text(stringResource(R.string.refresh_stickers))}
        }
        if(checking) LinearProgressIndicator(Modifier.fillMaxWidth())
        if(needsPermission) TextButton(onClick=grant){Text(stringResource(R.string.find_whatsapp_stickers))}
        found.forEach {candidate->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text(stringResource(if(candidate.source.uri=="whatsapp") R.string.whatsapp_stickers else R.string.business_stickers)+" · "+(candidate.count ?: 0))
                TextButton(onClick={add(candidate)}){Text(stringResource(R.string.add_stickers))}
            }
        }
        if(items.isEmpty()) {
            Text(stringResource(R.string.no_stickers_imported),modifier=Modifier.padding(top=24.dp))
            TextButton(onClick=folder){Text(stringResource(R.string.select_sticker_folder))}
        } else {
            LazyVerticalGrid(GridCells.Adaptive(88.dp),modifier=Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                items(items,key={it.id}) {item->
                    AsyncImage(model=item.uri,contentDescription=item.label.ifBlank {item.name},
                        modifier=Modifier.fillMaxWidth().aspectRatio(1f).clickable {preview=item},contentScale=ContentScale.Fit)
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                TextButton(onClick={offset=(offset-200).coerceAtLeast(0)},enabled=offset>0){Text(stringResource(R.string.previous_page))}
                TextButton(onClick={offset+=200},enabled=items.size==200){Text(stringResource(R.string.next_page))}
            }
        }
    }
    preview?.let {item->AlertDialog(onDismissRequest={preview=null},title={Text(item.label.ifBlank {item.name })},
        text={AsyncImage(item.uri,item.name,Modifier.fillMaxWidth().height(240.dp),contentScale=ContentScale.Fit)},
        confirmButton={TextButton(onClick={preview=null}){Text(stringResource(R.string.close))}})}
}
