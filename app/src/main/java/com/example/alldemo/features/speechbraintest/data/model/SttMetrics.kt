package com.example.alldemo.features.speechbraintest.data.model

data class SttMetrics(
    val latencyMs: Long,
    val audioDurationMs: Long,
    val realTimeFactor: Float = if (audioDurationMs > 0) latencyMs.toFloat() / audioDurationMs else 0f,
    val confidence: Float,
    val rawJsonSizeBytes: Int = 0,
    val rawJson: String = "",
    val httpStatusCode: Int = 200,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)
