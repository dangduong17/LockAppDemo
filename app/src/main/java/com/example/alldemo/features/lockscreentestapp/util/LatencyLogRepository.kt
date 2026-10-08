package com.example.alldemo.features.lockscreentestapp.util

import com.example.alldemo.features.lockscreentestapp.model.LatencyLogEntry
import com.example.alldemo.features.lockscreentestapp.model.LatencySummaryStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Repository managing latency tracking logs and history statistics.
 */
object LatencyLogRepository {

    /**
     * Threshold in milliseconds above which a render delay or launcher leak is suspected.
     */
    const val LEAK_LATENCY_THRESHOLD_MS = 500L

    private val _logs = MutableStateFlow<List<LatencyLogEntry>>(emptyList())
    val logs: StateFlow<List<LatencyLogEntry>> = _logs.asStateFlow()

    /**
     * Normalizes the event action string into standardized event types:
     * "SCREEN_OFF", "SCREEN_ON", or "MANUAL_TEST".
     */
    fun normalizeEventType(rawEvent: String): String {
        return when {
            rawEvent.contains("SCREEN_OFF", ignoreCase = true) -> "SCREEN_OFF"
            rawEvent.contains("SCREEN_ON", ignoreCase = true) -> "SCREEN_ON"
            rawEvent.contains("TEST", ignoreCase = true) ||
                    rawEvent.contains("MANUAL", ignoreCase = true) -> "MANUAL_TEST"
            else -> rawEvent.uppercase()
        }
    }

    /**
     * Logs a new screen broadcast / overlay render latency entry.
     */
    fun logEvent(
        timestamp: Long,
        eventType: String,
        latencyMs: Long,
        customLeakCheck: Boolean? = null,
        statusMessage: String? = null
    ) {
        val normalizedType = normalizeEventType(eventType)
        val isLeak = customLeakCheck ?: (latencyMs > LEAK_LATENCY_THRESHOLD_MS)
        val status = statusMessage ?: if (isLeak) {
            "Potential Leak / Delay (>${LEAK_LATENCY_THRESHOLD_MS}ms)"
        } else {
            "Rendered Successfully"
        }

        val entry = LatencyLogEntry(
            timestamp = timestamp,
            eventType = normalizedType,
            latencyMs = latencyMs,
            isPotentialLeak = isLeak,
            statusMessage = status
        )

        _logs.update { currentList ->
            listOf(entry) + currentList
        }
    }

    /**
     * Clears all recorded latency logs.
     */
    fun clearLogs() {
        _logs.value = emptyList()
    }

    /**
     * Calculates summary statistics for the provided list of logs (or current logs state).
     */
    fun getSummaryStats(entries: List<LatencyLogEntry> = _logs.value): LatencySummaryStats {
        if (entries.isEmpty()) {
            return LatencySummaryStats()
        }
        val total = entries.size
        val avg = entries.asSequence().map { it.latencyMs }.average()
        val max = entries.maxOfOrNull { it.latencyMs } ?: 0L
        val leaks = entries.count { it.isPotentialLeak }

        return LatencySummaryStats(
            totalEvents = total,
            averageLatencyMs = avg,
            maxLatencyMs = max,
            leakCount = leaks
        )
    }
}
