package com.example.alldemo.features.geminimultimodallivevoiceapi.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alldemo.features.geminimultimodallivevoiceapi.ui.components.ConnectionSettingsCard
import com.example.alldemo.features.geminimultimodallivevoiceapi.ui.components.StatusBanner
import com.example.alldemo.features.geminimultimodallivevoiceapi.ui.components.TranscriptionCard
import com.example.alldemo.features.geminimultimodallivevoiceapi.ui.components.VoiceControlsCard
import com.example.alldemo.features.geminimultimodallivevoiceapi.ui.state.RealtimeUiState
import com.example.alldemo.features.geminimultimodallivevoiceapi.ui.state.VerificationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealtimeVoiceScreen(
    uiState: RealtimeUiState,
    onApiKeyChange: (String) -> Unit,
    onTargetPasswordChange: (String) -> Unit,
    onModelChange: (String) -> Unit = {},
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onRecordToggle: () -> Unit,
    onSendTextMessage: (String) -> Unit,
    onClearMessages: () -> Unit,
    onResetVerification: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { errorMsg ->
            snackbarHostState.showSnackbar(errorMsg)
            onClearError()
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = if (uiState.verificationStatus == VerificationStatus.SUCCESS) {
            Color(0xFF1B5E20)
        } else {
            MaterialTheme.colorScheme.background
        },
        animationSpec = tween(durationMillis = 500),
        label = "backgroundColor"
    )

    val topBarContainerColor = if (uiState.verificationStatus == VerificationStatus.SUCCESS) {
        Color(0xFF2E7D32)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val topBarContentColor = if (uiState.verificationStatus == VerificationStatus.SUCCESS) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = "Security App Icon",
                            tint = if (uiState.verificationStatus == VerificationStatus.SUCCESS) Color.White else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (uiState.verificationStatus == VerificationStatus.SUCCESS) "UNLOCKED / ĐÃ MỞ KHÓA" else "Voice Password Lock",
                            fontWeight = FontWeight.Bold,
                            color = topBarContentColor
                        )
                    }
                },
                navigationIcon = navigationIcon,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = topBarContainerColor,
                    titleContentColor = topBarContentColor
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth >= 600.dp

            if (isWideScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatusBanner(
                            verificationStatus = uiState.verificationStatus,
                            targetPassword = uiState.targetPassword,
                            lastVerifiedTranscript = uiState.lastVerifiedTranscript,
                            onResetClick = onResetVerification
                        )

                        VoiceControlsCard(
                            isRecording = uiState.isRecording,
                            audioLevel = uiState.audioLevel,
                            connectionState = uiState.connectionState,
                            onRecordToggle = onRecordToggle
                        )

                        ConnectionSettingsCard(
                            apiKey = uiState.apiKey,
                            targetPassword = uiState.targetPassword,
                            selectedModel = uiState.selectedModel,
                            connectionState = uiState.connectionState,
                            onApiKeyChange = onApiKeyChange,
                            onTargetPasswordChange = onTargetPasswordChange,
                            onModelChange = onModelChange,
                            onConnectClick = onConnectClick,
                            onDisconnectClick = onDisconnectClick
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TranscriptionCard(
                            messages = uiState.messages,
                            onSendTextMessage = onSendTextMessage,
                            onClearMessages = onClearMessages,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatusBanner(
                        verificationStatus = uiState.verificationStatus,
                        targetPassword = uiState.targetPassword,
                        lastVerifiedTranscript = uiState.lastVerifiedTranscript,
                        onResetClick = onResetVerification
                    )

                    VoiceControlsCard(
                        isRecording = uiState.isRecording,
                        audioLevel = uiState.audioLevel,
                        connectionState = uiState.connectionState,
                        onRecordToggle = onRecordToggle
                    )

                    ConnectionSettingsCard(
                        apiKey = uiState.apiKey,
                        targetPassword = uiState.targetPassword,
                        selectedModel = uiState.selectedModel,
                        connectionState = uiState.connectionState,
                        onApiKeyChange = onApiKeyChange,
                        onTargetPasswordChange = onTargetPasswordChange,
                        onModelChange = onModelChange,
                        onConnectClick = onConnectClick,
                        onDisconnectClick = onDisconnectClick
                    )

                    TranscriptionCard(
                        messages = uiState.messages,
                        onSendTextMessage = onSendTextMessage,
                        onClearMessages = onClearMessages
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
