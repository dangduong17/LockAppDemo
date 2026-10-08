package com.example.alldemo.features.futovoiceinputengine

/**
 * UI State for the Voice Password feature following Unidirectional Data Flow pattern.
 */
data class VoicePasswordUiState(
    val targetPassword: String = "",
    val recognizedText: String = "",
    val isListening: Boolean = false,
    val isMatched: Boolean = false,
    val errorMessage: String? = null,
    val partialResults: String = "",
    val activeEngine: String = ""
)
