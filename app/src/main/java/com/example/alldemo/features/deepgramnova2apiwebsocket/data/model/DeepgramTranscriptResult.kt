package com.example.alldemo.features.deepgramnova2apiwebsocket.data.model

/**
 * Data model representing the parsed transcript result from Deepgram WebSocket.
 */
data class DeepgramTranscriptResult(
    val transcript: String,
    val isFinal: Boolean,
    val confidence: Double = 0.0,
    val speechFinal: Boolean = false
)
