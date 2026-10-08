package com.example.alldemo.features.wakewordvoiceprint.ui.verification

enum class PipelineStage {
    IDLE,
    LISTENING_KWS,
    VERIFYING_VOICEPRINT
}

sealed interface VerificationResult {
    data object Idle : VerificationResult

    data class Granted(
        val kwsScore: Float,
        val voiceprintScore: Float,
        val detectedKeyword: String
    ) : VerificationResult

    data class DeniedVoiceprint(
        val kwsScore: Float,
        val voiceprintScore: Float,
        val detectedKeyword: String,
        val message: String = "Từ chối: Không phải giọng chủ nhân"
    ) : VerificationResult

    data class DeniedKeyword(
        val energy: Float,
        val kwsScore: Float,
        val message: String = "Từ chối: Sai từ khóa kích hoạt"
    ) : VerificationResult

    data class NoVoiceprintProfile(
        val message: String = "Chưa đăng ký vân giọng chủ nhân"
    ) : VerificationResult
}

data class VerificationUiState(
    val isListening: Boolean = false,
    val pipelineStage: PipelineStage = PipelineStage.IDLE,
    val result: VerificationResult = VerificationResult.Idle,
    val currentAmplitude: Float = 0f,
    val kwsScore: Float = 0f,
    val voiceprintScore: Float = 0f,
    val kwsThreshold: Float = 0.65f,
    val voiceprintThreshold: Float = 0.70f,
    val selectedWakeword: String = "Hey Assistant",
    val hasOwnerProfile: Boolean = false,
    val isUnlocked: Boolean = false,
    val statusMessage: String = "Sẵn sàng nhận diện 2 tầng"
)
