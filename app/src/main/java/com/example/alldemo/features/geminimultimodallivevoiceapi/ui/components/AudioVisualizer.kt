package com.example.alldemo.features.geminimultimodallivevoiceapi.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AudioVisualizer(
    isRecording: Boolean,
    audioLevel: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 9,
    barWidth: Dp = 6.dp,
    barMaxHeight: Dp = 48.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveAnimation")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(barMaxHeight)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val baseMultipliers = listOf(0.4f, 0.7f, 1.0f, 0.6f, 0.9f, 0.5f, 0.8f, 0.3f, 0.6f)

        for (i in 0 until barCount) {
            val phaseOffset = (i * 120) % 500

            val animatedPhase by if (isRecording) {
                infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 350 + phaseOffset, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "barPhase_$i"
                )
            } else {
                animateFloatAsState(targetValue = 0.1f, label = "barIdle_$i")
            }

            val multiplier = baseMultipliers.getOrElse(i) { 0.5f }
            val currentLevel = if (isRecording) {
                (audioLevel.coerceIn(0.01f, 1.0f) * 0.7f + animatedPhase * 0.3f) * multiplier
            } else {
                0.08f
            }.coerceIn(0.08f, 1.0f)

            val barHeightState by animateFloatAsState(
                targetValue = currentLevel,
                animationSpec = tween(durationMillis = 80, easing = LinearEasing),
                label = "barHeight_$i"
            )

            val barColor = if (isRecording) activeColor else inactiveColor

            Box(
                modifier = Modifier
                    .width(barWidth)
                    .fillMaxHeight(barHeightState)
                    .clip(RoundedCornerShape(barWidth / 2))
                    .background(barColor)
            )
        }
    }
}
