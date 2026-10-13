package com.example.alldemo.features.geminimultimodallivevoiceapi.ui.state

import com.example.alldemo.features.geminimultimodallivevoiceapi.data.model.TranscribedMessage
import com.example.alldemo.features.geminimultimodallivevoiceapi.network.ConnectionState
import com.example.alldemo.features.geminimultimodallivevoiceapi.network.GeminiLiveClient

enum class VerificationStatus {
    IDLE,
    LISTENING,
    SUCCESS,
    FAILED
}

data class RealtimeUiState(
    val apiKey: String = "",
    val selectedModel: String = GeminiLiveClient.DEFAULT_MODEL,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val isRecording: Boolean = false,
    val audioLevel: Float = 0f,
    val messages: List<TranscribedMessage> = emptyList(),
    val errorMessage: String? = null,
    val targetPassword: String = "mở khóa",
    val verificationStatus: VerificationStatus = VerificationStatus.IDLE,
    val lastVerifiedTranscript: String = ""
)
