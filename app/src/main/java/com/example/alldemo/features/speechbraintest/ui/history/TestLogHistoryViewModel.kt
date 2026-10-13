package com.example.alldemo.features.speechbraintest.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alldemo.features.speechbraintest.data.model.SttTestLog
import com.example.alldemo.features.speechbraintest.data.model.TestStatus
import com.example.alldemo.features.speechbraintest.data.repository.SttRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogFilterOption(val label: String) {
    ALL("All Tests"),
    PASS_ONLY("Pass Only"),
    FAIL_ONLY("Fail Only")
}

data class HistorySummaryStats(
    val totalCount: Int = 0,
    val passCount: Int = 0,
    val failCount: Int = 0,
    val passRatePercentage: Int = 0,
    val averageLatencyMs: Long = 0L,
    val averageConfidencePercentage: Int = 0
)

data class TestLogHistoryUiState(
    val filteredLogs: List<SttTestLog> = emptyList(),
    val allLogs: List<SttTestLog> = emptyList(),
    val searchQuery: String = "",
    val filterOption: LogFilterOption = LogFilterOption.ALL,
    val summaryStats: HistorySummaryStats = HistorySummaryStats()
)

class TestLogHistoryViewModel(
    private val repository: SttRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _filterOption = MutableStateFlow(LogFilterOption.ALL)

    private val _uiState = MutableStateFlow(TestLogHistoryUiState())
    val uiState: StateFlow<TestLogHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.testLogsState, _searchQuery, _filterOption) { logs, query, filter ->
                val filtered = logs.filter { log ->
                    val matchesQuery = query.isBlank() ||
                        log.recognizedTranscript.contains(query, ignoreCase = true) ||
                        (log.expectedTranscript?.contains(query, ignoreCase = true) == true) ||
                        log.audioFileName.contains(query, ignoreCase = true)

                    val matchesFilter = when (filter) {
                        LogFilterOption.ALL -> true
                        LogFilterOption.PASS_ONLY -> log.status == TestStatus.SUCCESS
                        LogFilterOption.FAIL_ONLY -> log.status == TestStatus.FAILED
                    }

                    matchesQuery && matchesFilter
                }

                val stats = computeSummaryStats(logs)

                TestLogHistoryUiState(
                    filteredLogs = filtered,
                    allLogs = logs,
                    searchQuery = query,
                    filterOption = filter,
                    summaryStats = stats
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterOption(filter: LogFilterOption) {
        _filterOption.value = filter
    }

    fun clearLogs() {
        repository.clearLogs()
    }

    fun formatLogsForExport(): String {
        val logs = _uiState.value.allLogs
        if (logs.isEmpty()) return "No test logs to export."

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val builder = StringBuilder()
        builder.appendLine("Timestamp,Status,Audio File,Expected Target,Recognized Transcript,Latency (ms),Confidence (%),WER,Mode,Audio Duration (ms)")

        logs.forEach { log ->
            val dateStr = dateFormat.format(Date(log.timestampMs))
            val targetStr = sanitizeCsv(log.expectedTranscript.orEmpty())
            val recognizedStr = sanitizeCsv(log.recognizedTranscript)
            val confPct = (log.metrics.confidence * 100).toInt()
            val werStr = log.wordErrorRate?.let { String.format(Locale.US, "%.2f", it) } ?: "N/A"
            val modeStr = if (log.isMockMode) "Mock" else "GoogleCloudSTT"

            builder.appendLine(
                "\"$dateStr\",${log.status},\"${log.audioFileName}\",\"$targetStr\",\"$recognizedStr\",${log.metrics.latencyMs},$confPct,$werStr,$modeStr,${log.audioDurationMs}"
            )
        }

        return builder.toString()
    }

    private fun sanitizeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }

    private fun computeSummaryStats(logs: List<SttTestLog>): HistorySummaryStats {
        if (logs.isEmpty()) return HistorySummaryStats()

        val total = logs.size
        val pass = logs.count { it.status == TestStatus.SUCCESS }
        val fail = logs.count { it.status == TestStatus.FAILED }
        val passRate = (pass.toFloat() / total * 100).toInt()

        val avgLatency = logs.map { it.metrics.latencyMs }.average().toLong()
        val avgConfidence = (logs.map { it.metrics.confidence }.average() * 100).toInt()

        return HistorySummaryStats(
            totalCount = total,
            passCount = pass,
            failCount = fail,
            passRatePercentage = passRate,
            averageLatencyMs = avgLatency,
            averageConfidencePercentage = avgConfidence
        )
    }
}
