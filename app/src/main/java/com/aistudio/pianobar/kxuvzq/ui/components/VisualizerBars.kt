package com.aistudio.pianobar.kxuvzq.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aistudio.pianobar.kxuvzq.ui.theme.PianoGreen

@Composable
fun VisualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 18,
    maxHeight: Dp = 36.dp
) {
    val transition = rememberInfiniteTransition(label = "visualizer_anim")

    val anim1 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isPlaying) 0.95f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )

    val anim2 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = if (isPlaying) 0.85f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 620, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )

    val anim3 by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = if (isPlaying) 1.0f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    val anim4 by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = if (isPlaying) 0.7f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 540, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )

    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val anims = listOf(anim1, anim2, anim3, anim4)
        for (i in 0 until barCount) {
            val scale = anims[i % anims.size]
            // Add varied phase variation
            val adjustedFraction = if (isPlaying) {
                ((scale + (i * 0.05f)) % 0.85f + 0.15f).coerceIn(0.12f, 1.0f)
            } else {
                0.15f
            }

            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .fillMaxHeight(adjustedFraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PianoGreen)
            )
        }
    }
}
