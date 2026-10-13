package com.example.alldemo.features.speechbraintest.ui.audio

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.alldemo.features.speechbraintest.data.model.AudioPayload
import com.example.alldemo.features.speechbraintest.ui.components.AudioWaveformVisualizer
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AudioRecorderSection(
    viewModel: AudioRecorderViewModel,
    onAudioCaptured: (AudioPayload) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val permissionState = rememberPermissionState(
        permission = Manifest.permission.RECORD_AUDIO
    )

    val hasPermission = permissionState.status.isGranted ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(uiState.lastRecordedPayload) {
        uiState.lastRecordedPayload?.let { payload ->
            onAudioCaptured(payload)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        AudioWaveformVisualizer(
            amplitudes = uiState.amplitudeHistory,
            isRecording = uiState.isRecording,
            elapsedTimeMs = uiState.elapsedTimeMs,
            targetDurationMs = uiState.targetDurationMs,
            onTargetDurationChange = { viewModel.setTargetDuration(it) },
            onStartRecording = { viewModel.startRecording() },
            onStopRecording = { viewModel.stopRecording() },
            hasPermission = hasPermission || uiState.isMockMode,
            onRequestPermission = { permissionState.launchPermissionRequest() }
        )

        uiState.lastRecordedPayload?.let { payload ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Captured Audio Payload",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "File: ${payload.audioFileName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Duration: ${payload.durationMs}ms | Format: ${payload.encoding.value} ${payload.sampleRateHertz}Hz",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Size: ${(payload.audioBytes?.size ?: 0) / 1024} KB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}
