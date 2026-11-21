package com.aistudio.pianobar.kxuvzq.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

val isAiSlopTheme: Boolean
    @Composable get() = LocalPianobarPalette.current.id == AiSlopPalette.id

object AiSlopGradients {
    val borderBrush: Brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFC084FC),
            Color(0xFF818CF8),
            Color(0xFF22D3EE)
        )
    )

    val vibrantBrush: Brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFC084FC),
            Color(0xFF818CF8),
            Color(0xFF22D3EE)
        )
    )

    val textBrush: Brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF0ABFC),
            Color(0xFFC084FC),
            Color(0xFF22D3EE)
        )
    )

    val visualizerBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF472B6),
            Color(0xFFC084FC),
            Color(0xFF22D3EE)
        )
    )
}

/**
 * Satirical Slopstrap backdrop: `.slop-grid-bg` + floating `.slop-orb-layer`
 * purple/cyan ambient glow orbs rendered strictly for the opt-in AI Slop theme.
 */
@Composable
fun AiSlopBackdrop(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "slop_orb_motion")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_phase"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val step = 34.dp.toPx()
        val gridColor = Color(0xFF22D3EE).copy(alpha = 0.055f)
        var x = 0f
        while (x <= size.width) {
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
            x += step
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += step
        }

        // Top-left `.slop-orb-purple`
        val purpleCenter = Offset(
            x = size.width * (0.18f + 0.08f * cos(phase)),
            y = size.height * (0.14f + 0.05f * sin(phase))
        )
        val purpleRadius = size.minDimension * 0.68f * pulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFA855F7).copy(alpha = 0.30f),
                    Color(0xFF7E22CE).copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = purpleCenter,
                radius = purpleRadius
            ),
            radius = purpleRadius,
            center = purpleCenter
        )

        // Bottom-right `.slop-orb-cyan`
        val cyanCenter = Offset(
            x = size.width * (0.84f + 0.07f * sin(phase)),
            y = size.height * (0.76f + 0.06f * cos(phase))
        )
        val cyanRadius = size.minDimension * 0.72f * pulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF22D3EE).copy(alpha = 0.25f),
                    Color(0xFF0284C7).copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = cyanCenter,
                radius = cyanRadius
            ),
            radius = cyanRadius,
            center = cyanCenter
        )

        // Mid-right hot-pink accent glow
        val pinkCenter = Offset(
            x = size.width * (0.76f - 0.06f * cos(phase * 0.7f)),
            y = size.height * (0.34f + 0.05f * sin(phase * 0.7f))
        )
        val pinkRadius = size.minDimension * 0.45f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFF472B6).copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = pinkCenter,
                radius = pinkRadius
            ),
            radius = pinkRadius,
            center = pinkCenter
        )
    }
}

/**
 * Dual-ring pulsing `.slop-status` / `.slop-dot` pill badge.
 */
@Composable
fun AiSlopStatusBanner(
    isPlaying: Boolean,
    isBuffering: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "slop_dot_ping")
    val ringScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ping_scale"
    )
    val ringAlpha by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ping_alpha"
    )

    val dotColor = when {
        isBuffering -> PianoAmber
        isPlaying -> PianoGreen
        else -> Color(0xFF22D3EE)
    }
    val statusCopy = when {
        isBuffering -> "QUANTUM GENOME SYNCING..."
        isPlaying -> "ALL SYSTEMS VIBING · ENGINE LIVE"
        else -> "NEURAL STANDBY · READY TO SHIP"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(PianoSurface.copy(alpha = 0.74f))
            .border(1.dp, AiSlopGradients.borderBrush, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag("ai_slop_status_banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier.size(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .scale(ringScale)
                        .clip(CircleShape)
                        .background(dotColor.copy(alpha = ringAlpha))
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusCopy,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "✨ SLOP v2",
            style = TextStyle(
                brush = AiSlopGradients.textBrush,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            maxLines = 1
        )
    }
}

/**
 * Mandatory vibecoded KPI bento strip inside the hero card.
 */
@Composable
fun AiSlopBentoStrip(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .testTag("ai_slop_bento_strip"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SlopKpiChip(
                value = "99.9%",
                label = "VIBES",
                valueColor = PianoPurple,
                modifier = Modifier.weight(1f)
            )
            SlopKpiChip(
                value = "10x",
                label = "VELOCITY",
                valueColor = Color(0xFF22D3EE),
                modifier = Modifier.weight(1f)
            )
            SlopKpiChip(
                value = "ZERO",
                label = "FRICTION",
                valueColor = PianoGreen,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = "✨ Shipped with Slopstrap™ · Autonomous Acoustic Cloud",
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SlopKpiChip(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(PianoSurfaceElevated.copy(alpha = 0.72f))
            .border(1.dp, PianoSurfaceBorder, RoundedCornerShape(10.dp))
            .padding(vertical = 7.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = valueColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
        Text(
            text = label,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
