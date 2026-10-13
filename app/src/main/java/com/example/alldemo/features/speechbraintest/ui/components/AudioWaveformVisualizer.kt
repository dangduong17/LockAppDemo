package com.example.alldemo.features.speechbraintest.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun AudioWaveformVisualizer(
    amplitudes: List<Float>,
    isRecording: Boolean,
    elapsedTimeMs: Long,
    targetDurationMs: Long,
    onTargetDurationChange: (Long) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.outlineVariant
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh

    val animatedBarColor by animateColorAsState(
        targetValue = if (isRecording) activeColor else inactiveColor,
        animationSpec = tween(300),
        label = "BarColor"
    )

    val progress = (elapsedTimeMs.toFloat() / targetDurationMs.toFloat()).coerceIn(0f, 1f)

    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRecording) "Recording Voice..." else "Ready to Record",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1000L, 2000L, 3000L).forEach { duration ->
                        val durationSeconds = duration / 1000
                        val selected = targetDurationMs == duration
                        FilterChip(
                            selected = selected,
                            onClick = { if (!isRecording) onTargetDurationChange(duration) },
                            enabled = !isRecording,
                            label = { Text("${durationSeconds}s", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (amplitudes.isEmpty() && !isRecording) {
                    Text(
                        text = "Tap microphone to capture 1-3s audio sample",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    WaveformCanvas(
                        amplitudes = amplitudes,
                        isRecording = isRecording,
                        barColor = animatedBarColor,
                        secondaryColor = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainer
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val formattedElapsed = String.format(Locale.US, "%.1fs", elapsedTimeMs / 1000f)
                    val formattedTarget = String.format(Locale.US, "%.1fs", targetDurationMs / 1000f)
                    Text(
                        text = "Time: $formattedElapsed / $formattedTarget",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = if (isRecording) "Auto-stop active" else "PCM 16kHz LINEAR16",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    )
                }

                Surface(
                    onClick = {
                        if (!hasPermission) {
                            onRequestPermission()
                        } else if (isRecording) {
                            onStopRecording()
                        } else {
                            onStartRecording()
                        }
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .semantics {
                            contentDescription = if (isRecording) "Stop Recording" else "Start Recording"
                        },
                    shape = CircleShape,
                    color = when {
                        !hasPermission -> MaterialTheme.colorScheme.errorContainer
                        isRecording -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.primaryContainer
                    },
                    contentColor = when {
                        !hasPermission -> MaterialTheme.colorScheme.onErrorContainer
                        isRecording -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    shadowElevation = 4.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(64.dp)
                    ) {
                        val icon = when {
                            !hasPermission -> Icons.Rounded.MicOff
                            isRecording -> Icons.Rounded.Stop
                            else -> Icons.Rounded.Mic
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            if (!hasPermission) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Microphone permission required to record audio",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun WaveformCanvas(
    amplitudes: List<Float>,
    isRecording: Boolean,
    barColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 36
) {
    val displayAmplitudes = if (amplitudes.size >= barCount) {
        amplitudes.takeLast(barCount)
    } else {
        List(barCount - amplitudes.size) { 0.05f } + amplitudes
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val gap = 6f
        val totalGaps = (barCount - 1) * gap
        val barWidth = (width - totalGaps) / barCount.toFloat()

        displayAmplitudes.forEachIndexed { index, rawAmplitude ->
            val amplitude = rawAmplitude.coerceIn(0.04f, 1.0f)
            val barHeight = (height * 0.85f * amplitude).coerceAtLeast(6f)

            val x = index * (barWidth + gap)
            val top = centerY - (barHeight / 2f)

            val isRecent = index >= barCount - 6
            val color = if (isRecording && isRecent) secondaryColor else barColor

            drawRoundRect(
                color = color,
                topLeft = Offset(x, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
