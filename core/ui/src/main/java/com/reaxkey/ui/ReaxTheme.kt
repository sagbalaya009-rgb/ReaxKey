package com.reaxkey.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

@Composable
fun ReaxTheme(theme:String="SYSTEM",content: @Composable () -> Unit) {
    val dark=theme=="DARK" || (theme=="SYSTEM" && isSystemInDarkTheme())
    val colors = if (dark) darkColorScheme(primary=Color(0xFFA5B4FF),secondary=Color(0xFFC6A5FF),background=Color(0xFF0B0D10),surface=Color(0xFF141820),surfaceVariant=Color(0xFF202631),onSurface=Color(0xFFF3F5FA))
        else lightColorScheme(primary=Color(0xFF415BDA),secondary=Color(0xFF7848C9),background=Color(0xFFF6F7FC),surface=Color.White)
    MaterialTheme(colorScheme = colors, shapes=Shapes(medium=RoundedCornerShape(20.dp),large=RoundedCornerShape(20.dp)), content = content)
}
