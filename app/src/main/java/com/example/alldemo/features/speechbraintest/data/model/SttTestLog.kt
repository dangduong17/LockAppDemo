package com.example.alldemo.features.speechbraintest.data.model

import java.util.UUID

data class SttTestLog(
    val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val audioFileName: String,
    val audioDurationMs: Long,
    val audioEncoding: String,
    val sampleRateHertz: Int,
    val expectedTranscript: String? = null,
    val recognizedTranscript: String,
    val wordErrorRate: Float? = null,
    val metrics: SttMetrics,
    val isMockMode: Boolean,
    val languageCode: String,
    val model: String,
    val status: TestStatus = TestStatus.SUCCESS,
    val notes: String? = null
)

enum class TestStatus {
    SUCCESS,
    FAILED,
    IN_PROGRESS
}
