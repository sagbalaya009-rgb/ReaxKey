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
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable fun SettingsScreen(preferences:Preferences,keyboardEnabled:Boolean,keyboardSelected:Boolean,
    enable:()->Unit,select:()->Unit,folder:()->Unit,refresh:()->Unit,sources:List<MediaSource>,diagnostics:()->Unit) {
    val value by preferences.flow.collectAsState(UserPreferences())
    val scope=rememberCoroutineScope()
    val context=LocalContext.current
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
        Text(stringResource(R.string.typing_settings),style=MaterialTheme.typography.titleMedium)
        TypingToggle(stringResource(R.string.typing_suggestions),value.suggestions) { scope.launch { preferences.suggestions(it) } }
        TypingToggle(stringResource(R.string.typing_autocorrect),value.autocorrect) { scope.launch { preferences.autocorrect(it) } }
        TypingToggle(stringResource(R.string.typing_learn),value.learnWords) { scope.launch { preferences.learnWords(it) } }
        TypingToggle(stringResource(R.string.typing_trackpad),value.spaceTrackpad) { scope.launch { preferences.spaceTrackpad(it) } }
        TypingToggle(stringResource(R.string.haptics),value.haptics) { scope.launch { preferences.haptics(it) } }
        TextButton(onClick={val learned=context.getSharedPreferences("learned_words",Context.MODE_PRIVATE)
            learned.edit().clear().putLong("__clear_epoch",learned.getLong("__clear_epoch",0L)+1).apply()}) {
            Text(stringResource(R.string.typing_clear_learned))
        }
        Text(stringResource(R.string.sticker_access),style=MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.sticker_access_detail),style=MaterialTheme.typography.bodyMedium)
        sources.filter {it.enabled}.forEach {source->
            Text(if(source.health=="SCANNING") stringResource(R.string.source_sticker_indexing,source.name)
                else stringResource(R.string.source_stickers_count,source.name,source.count))
        }
        OutlinedButton(onClick=folder){Text(stringResource(R.string.select_sticker_folder))}
        TextButton(onClick=refresh){Text(stringResource(R.string.refresh_stickers))}
        Text(stringResource(R.string.privacy_settings),style=MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.privacy_sticker_detail),style=MaterialTheme.typography.bodyMedium)
        TextButton(onClick=diagnostics){Text(stringResource(R.string.export_diagnostics))}
    }
}

@Composable private fun TypingToggle(label:String,checked:Boolean,onChange:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text(label); Switch(checked,onChange) }
}
