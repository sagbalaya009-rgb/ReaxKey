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
    val colors = if (dark) darkColorScheme(primary=Color(0xFFD3D9E0),background=Color(0xFF202226),surface=Color(0xFF2A2D31),surfaceVariant=Color(0xFF383C41),onSurface=Color(0xFFF4F4F4))
        else lightColorScheme(primary=Color(0xFF374B60),background=Color(0xFFF6F7F8),surface=Color.White)
    MaterialTheme(colorScheme = colors, shapes=Shapes(medium=RoundedCornerShape(8.dp),large=RoundedCornerShape(10.dp)), content = content)
}
