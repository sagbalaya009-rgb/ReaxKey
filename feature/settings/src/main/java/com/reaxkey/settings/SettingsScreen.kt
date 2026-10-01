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
import com.reaxkey.ui.R
import kotlinx.coroutines.launch

@Composable fun SettingsScreen(preferences:Preferences,setup:()->Unit,diagnostics:()->Unit) {
    val value by preferences.flow.collectAsState(UserPreferences())
    val scope=rememberCoroutineScope()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.settings),style=MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.privacy_local))
        Text(stringResource(R.string.theme),style=MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("SYSTEM" to R.string.theme_system,"LIGHT" to R.string.theme_light,"DARK" to R.string.theme_dark).forEach { (key,label) -> FilterChip(selected=value.theme==key,onClick={scope.launch { preferences.theme(key) }},label={Text(stringResource(label))}) }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text(stringResource(R.string.haptics)); Switch(value.haptics,{scope.launch { preferences.haptics(it) }}) }
        Text(stringResource(R.string.language),style=MaterialTheme.typography.titleMedium)
        listOf("en" to "English","fr" to "Français","es" to "Español","yo" to "Yorùbá").forEach { (tag,name) ->
            FilterChip(value.language==tag,{scope.launch { preferences.language(tag) }},label={Text(name)})
        }
        Button(onClick=setup) { Text(stringResource(R.string.setup_keyboard)) }
        OutlinedButton(onClick=diagnostics) { Text(stringResource(R.string.export_diagnostics)) }
    }
}
