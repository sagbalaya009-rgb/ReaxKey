package com.reaxkey.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.reaxkey.model.SetupState
import com.reaxkey.ui.R

@Composable
fun SetupScreen(state: SetupState, enable: () -> Unit, select: () -> Unit,find:()->Unit={},hasReactions:Boolean=false) {
    var testText by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.welcome_body), style = MaterialTheme.typography.bodyLarge)
        Card { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.privacy_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.privacy_body))
        } }
        Text(stringResource(when(state) {
            SetupState.NOT_ENABLED -> R.string.keyboard_disabled
            SetupState.ENABLED_NOT_SELECTED -> R.string.keyboard_enabled
            SetupState.SELECTED -> R.string.keyboard_selected
        }))
        Button(onClick = enable, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.enable_keyboard)) }
        OutlinedButton(onClick = select, modifier = Modifier.fillMaxWidth(), enabled = state != SetupState.NOT_ENABLED) { Text(stringResource(R.string.select_keyboard)) }
        com.reaxkey.ui.ReaxButton((if(hasReactions) "✓ " else "3  ")+stringResource(R.string.add_reactions),find,state==SetupState.SELECTED)
        Text(stringResource(R.string.test_title), style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = testText, onValueChange = { testText = it }, label = { Text(stringResource(R.string.test_hint)) }, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.test_privacy), style = MaterialTheme.typography.bodySmall)
    }
}
