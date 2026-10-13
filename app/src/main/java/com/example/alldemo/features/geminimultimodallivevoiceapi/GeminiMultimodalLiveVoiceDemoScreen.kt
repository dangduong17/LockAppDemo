package com.example.alldemo.features.geminimultimodallivevoiceapi

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alldemo.features.geminimultimodallivevoiceapi.ui.screen.RealtimeVoiceScreen
import com.example.alldemo.features.geminimultimodallivevoiceapi.ui.viewmodel.RealtimeVoiceViewModel

@Composable
fun GeminiMultimodalLiveVoiceDemoScreen(
    onBackClick: () -> Unit,
    viewModel: RealtimeVoiceViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RealtimeVoiceScreen(
        uiState = uiState,
        onApiKeyChange = viewModel::updateApiKey,
        onTargetPasswordChange = viewModel::updateTargetPassword,
        onModelChange = viewModel::updateSelectedModel,
        onConnectClick = viewModel::connect,
        onDisconnectClick = viewModel::disconnect,
        onRecordToggle = viewModel::toggleRecording,
        onSendTextMessage = viewModel::sendTextMessage,
        onClearMessages = viewModel::clearMessages,
        onResetVerification = viewModel::resetVerification,
        onClearError = viewModel::clearError,
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
    )
}
