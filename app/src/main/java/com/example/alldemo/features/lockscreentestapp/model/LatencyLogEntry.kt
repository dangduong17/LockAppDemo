package com.example.alldemo.features.lockscreentestapp.model

import java.util.UUID

/**
 * Data model for an overlay latency event log entry.
 */
data class LatencyLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long,
    val eventType: String, // "SCREEN_OFF", "SCREEN_ON", "MANUAL_TEST"
    val latencyMs: Long,
    val isPotentialLeak: Boolean,
    val statusMessage: String,
)
