package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import kotlin.math.sin

@Composable
fun VoiceWaveVisualizer(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 24
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_transition")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(56.dp)) {
            val totalWidth = size.width
            val height = size.height
            val barWidth = (totalWidth / (barCount * 1.6f)).coerceIn(4f, 12f)
            val spacing = (totalWidth - (barWidth * barCount)) / (barCount + 1)

            val waveGradient = Brush.verticalGradient(
                colors = listOf(NeonCyan, ElectricViolet, Color(0xFF38BDF8))
            )

            for (i in 0 until barCount) {
                val x = spacing + i * (barWidth + spacing)

                val amplitudeMultiplier = if (isActive) {
                    val wave = (sin(phase + (i * 0.45f)) + 1f) / 2f
                    0.25f + 0.75f * wave
                } else {
                    0.12f
                }

                val barHeight = (height * amplitudeMultiplier).coerceIn(4f, height)
                val top = (height - barHeight) / 2f

                drawRoundRect(
                    brush = waveGradient,
                    topLeft = Offset(x, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                )
            }
        }
    }
}
