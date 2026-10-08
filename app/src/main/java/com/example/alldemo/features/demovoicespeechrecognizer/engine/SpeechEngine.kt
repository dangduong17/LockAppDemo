package com.example.alldemo.features.demovoicespeechrecognizer.engine

import kotlinx.coroutines.flow.StateFlow

enum class EngineType(val displayName: String, val description: String) {
    ANDROID_NATIVE("Android SpeechRecognizer", "Online / System Speech Engine"),
    SHERPA_ONNX("Sherpa-onnx Offline", "On-device Offline Speech Engine")
}

enum class RecognitionLanguage(val code: String, val displayName: String, val description: String) {
    VIETNAMESE("vi-VN", "Tiếng Việt (vi-VN)", "Vietnamese speech recognition"),
    ENGLISH("en-US", "English (en-US)", "English speech recognition"),
    AUTO("auto", "Tự động / Hệ thống (Auto)", "System default locale recognition")
}

sealed interface RecognitionState {
    data object Idle : RecognitionState
    data object Initializing : RecognitionState
    data object Listening : RecognitionState
    data object Processing : RecognitionState
    data class PartialResult(val text: String, val latencyMs: Long) : RecognitionState
    data class FinalResult(val text: String, val latencyMs: Long) : RecognitionState
    data class Error(val message: String) : RecognitionState
}

data class RecognitionLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val engineType: EngineType,
    val languageCode: String = "vi-VN",
    val text: String,
    val latencyMs: Long,
    val isKeywordMatched: Boolean,
    val matchedKeyword: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

interface SpeechRecognitionEngine {
    val engineType: EngineType
    val state: StateFlow<RecognitionState>
    fun startListening(language: String = "vi-VN")
    fun stopListening()
    fun destroy()
}
