package com.example.alldemo.features.geminimultimodallivevoiceapi.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.alldemo.features.geminimultimodallivevoiceapi.network.ConnectionState

@Composable
fun VoiceControlsCard(
    isRecording: Boolean,
    audioLevel: Float,
    connectionState: ConnectionState,
    onRecordToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onPermissionDenied: (String) -> Unit = {}
) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onRecordToggle()
        } else {
            onPermissionDenied("Microphone permission required to record audio.")
            Toast.makeText(context, "Microphone permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleRecordClick() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            onRecordToggle()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val isConnected = connectionState is ConnectionState.Connected

    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val animatedPulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val pulseScale by animateFloatAsState(
        targetValue = if (isRecording) animatedPulse else 1.0f,
        label = "pulseScaleState"
    )

    val buttonColor by animateColorAsState(
        targetValue = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        label = "buttonColor"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isRecording) "Đang lắng nghe giọng nói..." else "Nhấn nút để nói mật khẩu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(110.dp)
            ) {
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .scale(pulseScale)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    )
                }

                Button(
                    onClick = { handleRecordClick() },
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) buttonColor else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isConnected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector = when {
                            !isConnected -> Icons.Filled.MicOff
                            isRecording -> Icons.Filled.Stop
                            else -> Icons.Filled.Mic
                        },
                        contentDescription = if (isRecording) "Dừng nói" else "Nói mật khẩu",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Button(
                onClick = { handleRecordClick() },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isRecording) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.GraphicEq,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRecording) "Dừng nói" else "Nói mật khẩu",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            AudioVisualizer(
                isRecording = isRecording,
                audioLevel = audioLevel,
                barCount = 11,
                barMaxHeight = 40.dp
            )

            if (!isConnected) {
                Text(
                    text = "Vui lòng kết nối WebSocket trước khi nói mật khẩu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
