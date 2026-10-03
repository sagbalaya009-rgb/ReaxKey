package com.reaxkey.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable fun ReaxButton(text:String,onClick:()->Unit,enabled:Boolean=true) {
    Button(onClick=onClick,enabled=enabled,shape=RoundedCornerShape(18.dp),
        colors=ButtonDefaults.buttonColors(containerColor=Color.Transparent,contentColor=Color.White),
        contentPadding=PaddingValues(0.dp),modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {
        Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF356AF4),Color(0xFF8A4CE5)))).padding(18.dp),contentAlignment=androidx.compose.ui.Alignment.Center) {
            Text(text,style=MaterialTheme.typography.titleSmall)
        }
    }
}
