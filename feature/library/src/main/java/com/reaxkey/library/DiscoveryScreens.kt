package com.reaxkey.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.reaxkey.model.*
import com.reaxkey.ui.R
import com.reaxkey.ui.ReaxButton

fun sourceNameResource(source:MediaSource):Int=when {
    source.type=="IMPORTED" -> R.string.imports_source
    source.type=="SAF_TREE" -> when(source.name){"whatsapp"->R.string.whatsapp_source;"whatsapp-business"->R.string.business_source;"telegram"->R.string.telegram_source;else->R.string.personal_source}
    else -> when(source.uri){"whatsapp"->R.string.whatsapp_source;"whatsapp-business"->R.string.business_source;"telegram"->R.string.telegram_source;"downloads"->R.string.downloads_source;"pictures"->R.string.pictures_source;else->R.string.phone_source}
}

@Composable fun AddReactionsScreen(find:()->Unit,import:()->Unit,folder:()->Unit,back:()->Unit) {
    var shareHelp by remember {mutableStateOf(false)}
    LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item {TextButton(onClick=back){Text(stringResource(R.string.back))}}
        item {Text(stringResource(R.string.add_reactions),style=MaterialTheme.typography.headlineLarge)}
        item {Card {Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.find_for_me_body),style=MaterialTheme.typography.bodyLarge)
            ReaxButton(stringResource(R.string.find_for_me),find)
        }}}
        item {ActionCard(R.string.import_phone,R.string.import_phone_body,import)}
        item {ActionCard(R.string.connect_another,R.string.connect_another_body,folder)}
        item {ActionCard(R.string.share_to_reax,R.string.share_help){shareHelp=true}}
    }
    if(shareHelp) AlertDialog(onDismissRequest={shareHelp=false},title={Text(stringResource(R.string.share_help_title))},text={Text(stringResource(R.string.share_help))},confirmButton={TextButton(onClick={shareHelp=false}){Text(stringResource(R.string.close))}})
}
@Composable private fun ActionCard(title:Int,body:Int,action:()->Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick=action)) {Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(stringResource(title),style=MaterialTheme.typography.titleLarge)
        Text(stringResource(body),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }}
}

@Composable fun DiscoveryScreen(started:Boolean,checking:Boolean,candidates:List<SourceCandidate>,failed:Boolean,
    find:()->Unit,add:(List<SourceCandidate>)->Unit,grant:(SourceCandidate)->Unit,permission:()->Unit,import:()->Unit,more:()->Unit) {
    var privacy by remember {mutableStateOf(false)}
    var included by rememberSaveable {mutableStateOf(emptyList<String>())}
    var excluded by rememberSaveable {mutableStateOf(emptyList<String>())}
    val available=candidates.filter {it.status in listOf(DiscoveryStatus.AVAILABLE,DiscoveryStatus.PARTIALLY_AVAILABLE) && (it.count ?: 0)>0}
    fun selected(candidate:SourceCandidate)=candidate.source.id in included || (candidate.autoSelected() && candidate.source.id !in excluded)
    val visible=candidates.filter {it.status!=DiscoveryStatus.NOT_FOUND}
    LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item {Text(stringResource(when {checking->R.string.looking_title;!started->R.string.find_title;available.isNotEmpty()->R.string.discovery_found;else->R.string.nothing_title}),style=MaterialTheme.typography.headlineLarge)}
        item {Text(stringResource(when {checking->R.string.looking_body;!started->R.string.find_body;available.isNotEmpty()->R.string.discovery_candidates;else->R.string.nothing_body}),style=MaterialTheme.typography.bodyLarge)}
        item {TextButton(onClick={privacy=!privacy}){Text(stringResource(R.string.discovery_privacy_title))};if(privacy) Text(stringResource(R.string.discovery_privacy_body))}
        if(checking) item {com.reaxkey.ui.ReaxWave()}
        if(!started) item {ReaxButton(stringResource(R.string.find_reactions),find)}
        if(failed) item {Text(stringResource(R.string.discovery_error));TextButton(onClick=find){Text(stringResource(R.string.find_more))}}
        items(visible,key={it.source.id}) { candidate ->
            Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(stringResource(sourceNameResource(candidate.source)),style=MaterialTheme.typography.titleLarge)
                    if(candidate in available) Checkbox(selected(candidate),{enabled->
                        if(enabled) {included=included+candidate.source.id;excluded=excluded-candidate.source.id}
                        else {included=included-candidate.source.id;excluded=excluded+candidate.source.id}
                    })
                }
                when(candidate.status) {
                    DiscoveryStatus.AVAILABLE,DiscoveryStatus.PARTIALLY_AVAILABLE -> {
                        Text(stringResource(R.string.found_count,candidate.count ?: 0))
                        if(candidate.status==DiscoveryStatus.PARTIALLY_AVAILABLE) Text(stringResource(R.string.selected_access))
                    }
                    DiscoveryStatus.PERMISSION_REQUIRED -> {
                        Text(stringResource(R.string.access_needed_body))
                        ReaxButton(stringResource(R.string.import_phone),import)
                        TextButton(onClick=permission){Text(stringResource(R.string.check_shared))}
                    }
                    DiscoveryStatus.AVAILABLE_REQUIRES_USER_GRANT -> {
                        Text(stringResource(R.string.source_lost))
                        TextButton(onClick={grant(candidate)}){Text(stringResource(R.string.reconnect))}
                    }
                    DiscoveryStatus.PRIVATE_OR_INACCESSIBLE -> Text(stringResource(R.string.unavailable))
                    else -> Text(stringResource(R.string.discovery_error))
                }
            }}
        }
        if(!checking && available.isNotEmpty()) item {ReaxButton(stringResource(R.string.add_reactions),{add(available.filter(::selected))},available.any(::selected))}
        if(started && !checking && available.isEmpty()) item {ReaxButton(stringResource(R.string.import_phone),import)}
        item {TextButton(onClick=more){Text(stringResource(R.string.add_reactions))}}
    }
}

@Composable fun SourceHomeScreen(repo:MediaRepository,selectedAccess:Boolean,find:()->Unit,add:()->Unit,morePhotos:()->Unit,rescan:(MediaSource)->Unit,disconnect:(MediaSource)->Unit) {
    val sources by remember {repo.sources()}.collectAsState(emptyList())
    LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {Text(stringResource(R.string.sources_title),style=MaterialTheme.typography.headlineLarge)}
        for((title,group) in listOf(R.string.found_phone to sources.filter {it.type=="MEDIASTORE"},R.string.added_you to sources.filter {it.type!="MEDIASTORE"})) {
            item {Text(stringResource(title),style=MaterialTheme.typography.titleMedium)}
            if(group.isEmpty()) item {Text(stringResource(R.string.no_reactions),color=MaterialTheme.colorScheme.onSurfaceVariant)}
            items(group,key={it.id}) {source ->Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(stringResource(sourceNameResource(source)),style=MaterialTheme.typography.titleLarge)
                Text(stringResource(when(source.health){"SCANNING"->R.string.checking;"REVOKED","DISCONNECTED"->R.string.needs_access;"OK"->if(source.count==0) R.string.no_reactions else if(selectedAccess && source.type=="MEDIASTORE") R.string.selected_connected else R.string.connected;else->R.string.unavailable}))
                if(source.health=="OK") Text(stringResource(R.string.reaction_count,source.count))
                if(source.health=="REVOKED") Text(stringResource(R.string.source_lost))
                Row {TextButton(onClick={rescan(source)}){Text(stringResource(if(source.enabled && source.health!="REVOKED") R.string.rescan else R.string.reconnect))};if(source.enabled) TextButton(onClick={disconnect(source)}){Text(stringResource(R.string.disconnect))}}
                if(selectedAccess && source.type=="MEDIASTORE") TextButton(onClick=morePhotos){Text(stringResource(R.string.add_more_photos))}
            }}}
        }
        item {ReaxButton(stringResource(R.string.find_more),find)}
        item {TextButton(onClick=add){Text(stringResource(R.string.add_reactions))}}
    }
}

@Composable fun ReactionsReadyScreen(repo:MediaRepository,tryKeyboard:()->Unit,more:()->Unit) {
    val sources by remember {repo.sources()}.collectAsState(emptyList())
    val busy=sources.any {it.health=="SCANNING"};val count=sources.filter {it.enabled && it.health=="OK"}.sumOf {it.count}
    Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
        Text(stringResource(if(busy) R.string.adding_reactions else if(count>=1000) R.string.success_large else if(count>0) R.string.success_title else R.string.nothing_title),style=MaterialTheme.typography.headlineLarge)
        if(busy) LinearProgressIndicator(Modifier.fillMaxWidth()) else Text(stringResource(if(count>0) R.string.success_count else R.string.nothing_body,*if(count>0) arrayOf<Any>(count) else emptyArray()),style=MaterialTheme.typography.bodyLarge)
        ReaxButton(stringResource(R.string.try_keyboard),tryKeyboard,!busy)
        TextButton(onClick=more){Text(stringResource(R.string.find_more))}
    }
}
