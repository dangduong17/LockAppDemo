package com.example.alldemo.features.wakewordvoiceprint.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.alldemo.features.wakewordvoiceprint.audio.AudioFeatureExtractor
import com.example.alldemo.features.wakewordvoiceprint.audio.AudioRecordManager
import com.example.alldemo.features.wakewordvoiceprint.audio.KeywordSpotter
import com.example.alldemo.features.wakewordvoiceprint.audio.KwsResult
import com.example.alldemo.features.wakewordvoiceprint.audio.VoiceprintMatcher
import com.example.alldemo.features.wakewordvoiceprint.data.SettingsRepository
import com.example.alldemo.features.wakewordvoiceprint.data.VoiceprintRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class SettingsViewModel(
    val settingsRepository: SettingsRepository,
    val voiceprintRepository: VoiceprintRepository,
    val audioRecordManager: AudioRecordManager = AudioRecordManager(),
    val keywordSpotter: KeywordSpotter = KeywordSpotter(),
    val featureExtractor: AudioFeatureExtractor = AudioFeatureExtractor(),
    val voiceprintMatcher: VoiceprintMatcher = VoiceprintMatcher()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var testRecordingJob: Job? = null
    private var testAmplitudeJob: Job? = null
    private val testPcmChunks = mutableListOf<ShortArray>()
    private var lastTestEvaluationTimeMs = 0L

    companion object {
        private const val TEST_EVALUATION_INTERVAL_MS = 120L
    }

    init {
        viewModelScope.launch {
            try {
                settingsRepository.kwsThresholdFlow.collect { kws ->
                    _uiState.update { it.copy(kwsThreshold = kws) }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        viewModelScope.launch {
            try {
                settingsRepository.voiceprintThresholdFlow.collect { vp ->
                    _uiState.update { it.copy(voiceprintThreshold = vp) }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        viewModelScope.launch {
            try {
                settingsRepository.selectedWakewordFlow.collect { wakeword ->
                    _uiState.update { it.copy(selectedWakeword = wakeword) }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        viewModelScope.launch {
            try {
                voiceprintRepository.masterVoiceprintFlow.collect { master ->
                    _uiState.update { state ->
                        state.copy(
                            masterVoiceprintVector = master,
                            isVoiceprintEnrolled = master != null,
                            voiceprintDimensions = master?.size ?: 26
                        )
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun updateKwsThreshold(threshold: Float) {
        val clamped = threshold.coerceIn(0.50f, 0.95f)
        viewModelScope.launch {
            try {
                settingsRepository.setKwsThreshold(clamped)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun updateVoiceprintThreshold(threshold: Float) {
        val clamped = threshold.coerceIn(0.50f, 0.95f)
        viewModelScope.launch {
            try {
                settingsRepository.setVoiceprintThreshold(clamped)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun updateSelectedWakeword(wakeword: String) {
        viewModelScope.launch {
            try {
                settingsRepository.setSelectedWakeword(wakeword)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun deleteMasterVoiceprint() {
        viewModelScope.launch {
            try {
                voiceprintRepository.clearVoiceprint()
                _uiState.update {
                    it.copy(
                        statusMessage = "Đã xóa vân giọng chủ nhân",
                        voiceprintTestResult = null
                    )
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                settingsRepository.setKwsThreshold(SettingsRepository.DEFAULT_KWS_THRESHOLD)
                settingsRepository.setVoiceprintThreshold(SettingsRepository.DEFAULT_VOICEPRINT_THRESHOLD)
                settingsRepository.setSelectedWakeword(SettingsRepository.DEFAULT_WAKEWORD)
                _uiState.update {
                    it.copy(statusMessage = "Đã reset về cấu hình mặc định")
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun selectTestTab(tab: StandaloneTestTab) {
        stopTestRecording()
        _uiState.update {
            it.copy(
                activeTestTab = tab,
                statusMessage = null
            )
        }
    }

    fun runKwsTest() {
        toggleTestRecording()
    }

    fun toggleTestRecording() {
        if (_uiState.value.isTestRecording) {
            stopTestRecording()
        } else {
            startTestRecording()
        }
    }

    fun startTestRecording() {
        if (_uiState.value.isTestRecording) return

        lastTestEvaluationTimeMs = 0L

        synchronized(testPcmChunks) {
            testPcmChunks.clear()
        }

        _uiState.update {
            it.copy(
                isTestRecording = true,
                statusMessage = "Đang ghi âm thử nghiệm..."
            )
        }

        testAmplitudeJob?.cancel()
        testAmplitudeJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                audioRecordManager.amplitude.collect { amp ->
                    _uiState.update { it.copy(testAmplitude = amp) }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        testRecordingJob?.cancel()
        testRecordingJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                audioRecordManager.startRecordingFlow()
                    .catch { e ->
                        stopTestRecording()
                        _uiState.update { it.copy(statusMessage = "Lỗi ghi âm: ${e.message}") }
                    }
                    .collect { chunk ->
                        if (!_uiState.value.isTestRecording) return@collect

                        try {
                            val now = System.currentTimeMillis()
                            val shouldEvaluate = (now - lastTestEvaluationTimeMs >= TEST_EVALUATION_INTERVAL_MS)

                            withContext(Dispatchers.Default) {
                                synchronized(testPcmChunks) {
                                    testPcmChunks.add(chunk)
                                }
                            }

                            if (_uiState.value.activeTestTab == StandaloneTestTab.KWS_ONLY && shouldEvaluate) {
                                lastTestEvaluationTimeMs = now
                                val currentPcm = getCombinedTestPcm()
                                evaluatePcmForKwsTest(currentPcm)
                            }
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            _uiState.update { it.copy(statusMessage = "Lỗi thử nghiệm KWS: ${e.message}") }
                        }
                    }
            } catch (e: Throwable) {
                stopTestRecording()
                _uiState.update { it.copy(statusMessage = "Không thể khởi tạo micro: ${e.message}") }
            }
        }
    }

    fun stopTestRecording() {
        if (!_uiState.value.isTestRecording) return

        try {
            audioRecordManager.stopRecording()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        testRecordingJob?.cancel()
        testRecordingJob = null
        testAmplitudeJob?.cancel()
        testAmplitudeJob = null

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val combinedPcm = getCombinedTestPcm()

                val currentState = _uiState.value
                when (currentState.activeTestTab) {
                    StandaloneTestTab.KWS_ONLY -> {
                        evaluatePcmForKwsTest(combinedPcm)
                    }
                    StandaloneTestTab.VOICEPRINT_ONLY -> {
                        evaluatePcmForVoiceprintTest(combinedPcm)
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }

            try {
                _uiState.update {
                    it.copy(
                        isTestRecording = false,
                        testAmplitude = 0f,
                        statusMessage = "Đã hoàn thành thử nghiệm"
                    )
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    suspend fun evaluatePcmForKwsTest(pcm: ShortArray) = withContext(Dispatchers.Default) {
        val state = _uiState.value
        if (pcm.isEmpty()) return@withContext

        val kwsResult = try {
            keywordSpotter.spotKeyword(
                pcm = pcm,
                keyword = state.selectedWakeword,
                keywordTemplate = null,
                kwsThreshold = state.kwsThreshold
            )
        } catch (e: Throwable) {
            e.printStackTrace()
            KwsResult(
                detected = false,
                score = 0f,
                energy = keywordSpotter.calculateEnergy(pcm),
                keyword = state.selectedWakeword,
                isNativeEngineUsed = false,
                statusMessage = "Lỗi KWS: ${e.message}"
            )
        }

        val testResult = KwsTestResult(
            score = kwsResult.score,
            energy = kwsResult.energy,
            detected = kwsResult.detected,
            keyword = kwsResult.keyword,
            thresholdUsed = state.kwsThreshold
        )

        _uiState.update {
            it.copy(kwsTestResult = testResult)
        }
    }

    suspend fun evaluatePcmForVoiceprintTest(pcm: ShortArray) = withContext(Dispatchers.Default) {
        val state = _uiState.value
        val masterVector = state.masterVoiceprintVector

        if (masterVector == null) {
            _uiState.update {
                it.copy(
                    voiceprintTestResult = VoiceprintTestResult(
                        similarityScore = 0f,
                        isMatch = false,
                        hasMasterProfile = false,
                        thresholdUsed = state.voiceprintThreshold
                    ),
                    statusMessage = "Chưa đăng ký vân giọng chủ nhân"
                )
            }
            return@withContext
        }

        if (pcm.isEmpty()) return@withContext

        try {
            val testFeatureVector = featureExtractor.extractFeatureVector(pcm)
            val similarity = voiceprintMatcher.calculateSimilarity(testFeatureVector, masterVector)
            val isMatch = similarity >= state.voiceprintThreshold

            val testResult = VoiceprintTestResult(
                similarityScore = similarity,
                isMatch = isMatch,
                hasMasterProfile = true,
                thresholdUsed = state.voiceprintThreshold
            )

            _uiState.update {
                it.copy(voiceprintTestResult = testResult)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            _uiState.update {
                it.copy(statusMessage = "Lỗi trích xuất vân giọng: ${e.message}")
            }
        }
    }

    private suspend fun getCombinedTestPcm(): ShortArray = withContext(Dispatchers.Default) {
        return@withContext try {
            synchronized(testPcmChunks) {
                val totalSize = testPcmChunks.sumOf { it.size }
                val combined = ShortArray(totalSize)
                var offset = 0
                for (chunk in testPcmChunks) {
                    System.arraycopy(chunk, 0, combined, offset, chunk.size)
                    offset += chunk.size
                }
                combined
            }
        } catch (e: Throwable) {
            ShortArray(0)
        }
    }

    override fun onCleared() {
        try {
            testRecordingJob?.cancel()
            testAmplitudeJob?.cancel()
            audioRecordManager.stopRecording()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}

class SettingsViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            val settingsRepo = SettingsRepository(context.applicationContext)
            val voiceprintRepo = VoiceprintRepository(context.applicationContext)
            val recordManager = AudioRecordManager(context.applicationContext)
            return SettingsViewModel(
                settingsRepository = settingsRepo,
                voiceprintRepository = voiceprintRepo,
                audioRecordManager = recordManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
