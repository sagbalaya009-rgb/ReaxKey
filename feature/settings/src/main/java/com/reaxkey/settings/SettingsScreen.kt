package com.reaxkey.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.reaxkey.data.*
import com.reaxkey.model.MediaSource
import com.reaxkey.ui.R
import kotlinx.coroutines.launch

@Composable fun SettingsScreen(preferences:Preferences,keyboardEnabled:Boolean,keyboardSelected:Boolean,
    enable:()->Unit,select:()->Unit,folder:()->Unit,refresh:()->Unit,sources:List<MediaSource>,diagnostics:()->Unit) {
    val value by preferences.flow.collectAsState(UserPreferences())
    val scope=rememberCoroutineScope()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.settings),style=MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.keyboard_settings),style=MaterialTheme.typography.titleMedium)
        if(!keyboardEnabled) Button(onClick=enable){Text(stringResource(R.string.enable_keyboard))}
        else if(!keyboardSelected) Button(onClick=select){Text(stringResource(R.string.select_keyboard))}
        else Text(stringResource(R.string.keyboard_selected))
        Text(stringResource(R.string.theme),style=MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("SYSTEM" to R.string.theme_system,"LIGHT" to R.string.theme_light,"DARK" to R.string.theme_dark).forEach { (key,label) -> FilterChip(selected=value.theme==key,onClick={scope.launch { preferences.theme(key) }},label={Text(stringResource(label))}) }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text(stringResource(R.string.haptics)); Switch(value.haptics,{scope.launch { preferences.haptics(it) }}) }
        Text(stringResource(R.string.sticker_access),style=MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.sticker_access_detail),style=MaterialTheme.typography.bodyMedium)
        sources.filter {it.enabled}.forEach {source->Text(stringResource(R.string.source_stickers_count,source.name,source.count))}
        OutlinedButton(onClick=folder){Text(stringResource(R.string.select_sticker_folder))}
        TextButton(onClick=refresh){Text(stringResource(R.string.refresh_stickers))}
        Text(stringResource(R.string.privacy_settings),style=MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.privacy_sticker_detail),style=MaterialTheme.typography.bodyMedium)
        TextButton(onClick=diagnostics){Text(stringResource(R.string.export_diagnostics))}
    }
}
