package com.example.alldemo.features.wakewordvoiceprint.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Compact spectrum visualizer for Master Voiceprint Feature Vector (26 dimensions: 13 MFCC Means + 13 Stds).
 */
@Composable
fun CompactVoiceprintSpectrumVisualizer(
    vector: FloatArray?,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 100.dp,
    barCount: Int = 26
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainerHigh
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val isEnrolled = vector != null && vector.isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (!isEnrolled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                drawRoundRect(
                    color = outlineColor,
                    style = Stroke(width = 2f, pathEffect = pathEffect),
                    cornerRadius = CornerRadius(12f, 12f)
                )
            }
            Text(
                text = "Chưa đăng ký vân giọng chủ nhân",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            val safeVector = vector ?: FloatArray(barCount)
            val maxVal = safeVector.maxOfOrNull { abs(it) }?.takeIf { it > 0.001f } ?: 1.0f

            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val centerY = canvasHeight / 2f

                val barDimensions = minOf(safeVector.size, barCount)
                val totalBarSpacing = canvasWidth / barDimensions
                val barWidth = (totalBarSpacing * 0.6f).coerceIn(3f, 16f)
                val spacing = totalBarSpacing - barWidth

                val gradientBrush = Brush.verticalGradient(
                    colors = listOf(primaryColor, tertiaryColor, secondaryColor)
                )

                for (i in 0 until barDimensions) {
                    val rawVal = safeVector[i]
                    val normalizedFactor = (abs(rawVal) / maxVal).coerceIn(0.08f, 1.0f)

                    val barHeight = (canvasHeight * 0.82f * normalizedFactor).coerceAtLeast(6f)
                    val x = i * totalBarSpacing + spacing / 2f
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
}
