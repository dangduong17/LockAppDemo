package com.example.alldemo.features.lockscreentestapp.model

/**
 * Aggregated summary statistics for latency log history.
 */
data class LatencySummaryStats(
    val totalEvents: Int = 0,
    val averageLatencyMs: Double = 0.0,
    val maxLatencyMs: Long = 0L,
    val leakCount: Int = 0,
)
