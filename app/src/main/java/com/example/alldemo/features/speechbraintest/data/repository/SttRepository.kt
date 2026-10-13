package com.example.alldemo.features.speechbraintest.data.repository

import com.example.alldemo.features.speechbraintest.data.model.AudioPayload
import com.example.alldemo.features.speechbraintest.data.model.SttConfig
import com.example.alldemo.features.speechbraintest.data.model.SttMetrics
import com.example.alldemo.features.speechbraintest.data.model.SttTestLog
import com.example.alldemo.features.speechbraintest.data.model.TestStatus
import com.example.alldemo.features.speechbraintest.domain.MatchingStrategy
import com.example.alldemo.features.speechbraintest.domain.PasswordMatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SttRepository(
    initialConfig: SttConfig = SttConfig(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val _configState = MutableStateFlow(initialConfig)
    val configState: StateFlow<SttConfig> = _configState.asStateFlow()

    private val _testLogsState = MutableStateFlow<List<SttTestLog>>(emptyList())
    val testLogsState: StateFlow<List<SttTestLog>> = _testLogsState.asStateFlow()

    private val mockSttService = MockSttServiceImpl(ioDispatcher = ioDispatcher)
    private val googleCloudSttService = GoogleCloudSttServiceImpl(initialConfig, ioDispatcher = ioDispatcher)

    fun setMockSimulatedLatency(latencyMs: Long) {
        mockSttService.setSimulatedLatency(latencyMs)
    }

    fun updateConfig(newConfig: SttConfig) {
        _configState.value = newConfig
        googleCloudSttService.updateConfig(newConfig)
    }

    fun setMockMode(enabled: Boolean) {
        _configState.update { it.copy(isMockMode = enabled) }
    }

    fun setApiKey(apiKey: String) {
        _configState.update { it.copy(apiKey = apiKey) }
        googleCloudSttService.updateConfig(_configState.value)
    }

    fun setLanguageCode(languageCode: String) {
        _configState.update { it.copy(languageCode = languageCode) }
        googleCloudSttService.updateConfig(_configState.value)
    }

    fun setModel(model: String) {
        _configState.update { it.copy(model = model) }
        googleCloudSttService.updateConfig(_configState.value)
    }

    fun setMatchingStrategy(strategy: MatchingStrategy) {
        _configState.update { it.copy(matchingStrategy = strategy) }
    }

    fun setMatchingThreshold(threshold: Float) {
        _configState.update { it.copy(matchingThreshold = threshold) }
    }

    fun setDefaultTargetPassword(password: String) {
        _configState.update { it.copy(defaultTargetPassword = password) }
    }

    suspend fun recognizeSpeech(
        payload: AudioPayload,
        expectedTranscript: String? = null
    ): Result<SttResponseWithMetrics> {
        val currentConfig = _configState.value
        val activeService: SttService = if (currentConfig.isMockMode) {
            mockSttService
        } else {
            googleCloudSttService
        }

        val result = activeService.recognize(payload)

        result.onSuccess { response ->
            val matchResult = expectedTranscript?.let { target ->
                PasswordMatcher.match(
                    target = target,
                    transcribed = response.result.transcript,
                    strategy = currentConfig.matchingStrategy,
                    threshold = currentConfig.matchingThreshold
                )
            }

            val testStatus = when {
                matchResult != null -> if (matchResult.isMatch) TestStatus.SUCCESS else TestStatus.FAILED
                else -> TestStatus.SUCCESS
            }

            val testLog = SttTestLog(
                audioFileName = payload.audioFileName,
                audioDurationMs = payload.durationMs,
                audioEncoding = payload.encoding.value,
                sampleRateHertz = payload.sampleRateHertz,
                expectedTranscript = expectedTranscript,
                recognizedTranscript = response.result.transcript,
                wordErrorRate = matchResult?.wordErrorRate,
                metrics = response.metrics,
                isMockMode = currentConfig.isMockMode,
                languageCode = payload.languageCode.ifBlank { currentConfig.languageCode },
                model = payload.model.ifBlank { currentConfig.model },
                status = testStatus,
                notes = matchResult?.let {
                    "Match Strategy: ${it.matchingStrategy.name}, Sim Score: ${(it.similarityScore * 100).toInt()}%"
                }
            )
            addTestLog(testLog)

        }.onFailure { exception ->
            val metrics = (exception as? SttApiException)?.metrics
                ?: SttMetrics(
                    latencyMs = 0L,
                    audioDurationMs = payload.durationMs,
                    confidence = 0f,
                    httpStatusCode = 500,
                    isSuccess = false,
                    errorMessage = exception.localizedMessage ?: exception.toString()
                )

            val testLog = SttTestLog(
                audioFileName = payload.audioFileName,
                audioDurationMs = payload.durationMs,
                audioEncoding = payload.encoding.value,
                sampleRateHertz = payload.sampleRateHertz,
                expectedTranscript = expectedTranscript,
                recognizedTranscript = "",
                metrics = metrics,
                isMockMode = currentConfig.isMockMode,
                languageCode = payload.languageCode.ifBlank { currentConfig.languageCode },
                model = payload.model.ifBlank { currentConfig.model },
                status = TestStatus.FAILED,
                notes = exception.localizedMessage
            )
            addTestLog(testLog)
        }

        return result
    }

    private fun addTestLog(log: SttTestLog) {
        _testLogsState.update { currentList ->
            listOf(log) + currentList
        }
    }

    fun clearLogs() {
        _testLogsState.value = emptyList()
    }

    fun calculateWordErrorRate(reference: String, hypothesis: String): Float {
        return PasswordMatcher.calculateWer(reference, hypothesis)
    }
}
