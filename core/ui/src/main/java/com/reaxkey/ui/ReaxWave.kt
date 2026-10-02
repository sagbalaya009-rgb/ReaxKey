package com.reaxkey.ui

import android.animation.ValueAnimator
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/** Indeterminate discovery motion. No invented percentage; respects disabled system animations. */
@Composable fun ReaxWave() {
    val animated=ValueAnimator.areAnimatorsEnabled()
    val transition=rememberInfiniteTransition(label="Discovery wave")
    val phase by transition.animateFloat(0f,6.283f,infiniteRepeatable(tween(2200,easing=LinearEasing)),label="Wave phase")
    val color=MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(36.dp)) {
        val center=size.width/2
        for(i in 0..12) {
            val height=8.dp.toPx()+(sin((if(animated) phase else 0f)+i*0.65f)+1f)*6.dp.toPx()
            val x=center+(i-6)*12.dp.toPx()
            drawLine(color,Offset(x,(size.height-height)/2),Offset(x,(size.height+height)/2),4.dp.toPx(),StrokeCap.Round)
        }
    }
}
