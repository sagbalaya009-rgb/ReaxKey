package com.reaxkey.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun ReaxTheme(theme:String="SYSTEM",content: @Composable () -> Unit) {
    val dark=theme=="DARK" || (theme=="SYSTEM" && isSystemInDarkTheme())
    val colors = if (dark) darkColorScheme(primary = Color(0xFFA6D5BC))
        else lightColorScheme(primary = Color(0xFF38634F), background = Color(0xFFF6F8F3))
    MaterialTheme(colorScheme = colors, content = content)
}
