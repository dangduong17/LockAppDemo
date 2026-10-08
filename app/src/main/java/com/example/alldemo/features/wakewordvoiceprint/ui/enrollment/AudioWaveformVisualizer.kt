package com.example.alldemo.features.wakewordvoiceprint.ui.enrollment

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Animated real-time audio waveform visualizer component.
 * Renders dynamic audio frequency bars responding to incoming RMS amplitude.
 */
@Composable
fun AudioWaveformVisualizer(
    amplitude: Float,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 30,
    visualizerHeight: Dp = 120.dp
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainerHigh

    // Infinite transition for ambient idle / pulsing movement
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_phase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Animated global scale factor for smooth amplitude response
    val animatedAmplitude by animateFloatAsState(
        targetValue = if (isRecording) amplitude.coerceIn(0.05f, 1.0f) else 0.05f,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "amplitude_anim"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(visualizerHeight)
            .clip(RoundedCornerShape(24.dp))
            .background(surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerY = canvasHeight / 2f

            val totalBarSpacing = canvasWidth / barCount
            val barWidth = (totalBarSpacing * 0.55f).coerceAtLeast(4f)
            val spacing = totalBarSpacing - barWidth

            val gradientBrush = Brush.verticalGradient(
                colors = listOf(primaryColor, tertiaryColor, secondaryColor)
            )

            for (i in 0 until barCount) {
                val x = i * totalBarSpacing + spacing / 2f

                // Generate symmetrical wave height distribution (higher in center)
                val centerDistanceFactor = sin(PI * i / (barCount - 1)).toFloat()
                val sineVal = sin(phase.toDouble() + i * 0.45).toFloat()
                val sineModulation = sineVal * 0.2f + 0.8f

                val rawHeightFactor = if (isRecording) {
                    (animatedAmplitude * centerDistanceFactor * sineModulation)
                        .coerceIn(0.06f, 1.0f)
                } else {
                    val idleSine = sin(phase.toDouble() + i * 0.3).toFloat()
                    0.05f + 0.04f * idleSine
                }

                val barHeight = (canvasHeight * 0.85f * rawHeightFactor).coerceAtLeast(8f)
                val topY = centerY - (barHeight / 2f)

                drawRoundRect(
                    brush = gradientBrush,
                    topLeft = Offset(x, topY),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}
