package com.example.alldemo.features.wakewordvoiceprint.ui.verification

import androidx.lifecycle.ViewModel
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

open class VerificationViewModel(
    val voiceprintRepository: VoiceprintRepository,
    val settingsRepository: SettingsRepository,
    val audioRecordManager: AudioRecordManager = AudioRecordManager(),
    val keywordSpotter: KeywordSpotter = KeywordSpotter(),
    val featureExtractor: AudioFeatureExtractor = AudioFeatureExtractor(),
    val voiceprintMatcher: VoiceprintMatcher = VoiceprintMatcher()
) : ViewModel() {

    companion object {
        private const val MAX_BUFFER_SAMPLES = 32000
        private const val SPEECH_DENIAL_ENERGY_THRESHOLD = 0.05f
        private const val KWS_EVALUATION_INTERVAL_MS = 120L
    }

    private val _uiState = MutableStateFlow(VerificationUiState())
    val uiState: StateFlow<VerificationUiState> = _uiState.asStateFlow()

    private var recordingJob: Job? = null
    private var amplitudeJob: Job? = null
    private val pcmAccumulator = ArrayList<Short>()
    private var lastEvaluationTimeMs = 0L

    private var cachedMasterVector: FloatArray? = null

    init {
        viewModelScope.launch {
            try {
                voiceprintRepository.masterVoiceprintFlow.collect { masterVector ->
                    cachedMasterVector = masterVector
                    _uiState.update { it.copy(hasOwnerProfile = masterVector != null) }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        viewModelScope.launch {
            try {
                settingsRepository.kwsThresholdFlow.collect { threshold ->
                    _uiState.update { it.copy(kwsThreshold = threshold) }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        viewModelScope.launch {
            try {
                settingsRepository.voiceprintThresholdFlow.collect { threshold ->
                    _uiState.update { it.copy(voiceprintThreshold = threshold) }
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
    }

    fun startListening() {
        if (_uiState.value.isListening) return

        lastEvaluationTimeMs = 0L

        _uiState.update {
            it.copy(
                isListening = true,
                pipelineStage = PipelineStage.LISTENING_KWS,
                result = VerificationResult.Idle,
                isUnlocked = false,
                statusMessage = "Đang lắng nghe từ khóa: ${it.selectedWakeword}..."
            )
        }

        amplitudeJob?.cancel()
        amplitudeJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                audioRecordManager.amplitude.collect { amp ->
                    _uiState.update { it.copy(currentAmplitude = amp) }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        recordingJob?.cancel()
        synchronized(pcmAccumulator) {
            pcmAccumulator.clear()
        }

        recordingJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                audioRecordManager.startRecordingFlow()
                    .catch { e ->
                        stopListening()
                        _uiState.update { state ->
                            state.copy(statusMessage = "Lỗi ghi âm: ${e.message}")
                        }
                    }
                    .collect { chunk ->
                        if (!_uiState.value.isListening) return@collect

                        try {
                            val now = System.currentTimeMillis()
                            val shouldEvaluate = (now - lastEvaluationTimeMs >= KWS_EVALUATION_INTERVAL_MS)

                            val currentAudio = withContext(Dispatchers.Default) {
                                synchronized(pcmAccumulator) {
                                    for (sample in chunk) {
                                        pcmAccumulator.add(sample)
                                    }
                                    if (pcmAccumulator.size > MAX_BUFFER_SAMPLES) {
                                        val dropCount = (pcmAccumulator.size - MAX_BUFFER_SAMPLES).coerceIn(0, pcmAccumulator.size)
                                        if (dropCount > 0) {
                                            pcmAccumulator.subList(0, dropCount).clear()
                                        }
                                    }
                                    if (shouldEvaluate) pcmAccumulator.toShortArray() else null
                                }
                            }

                            if (currentAudio != null) {
                                lastEvaluationTimeMs = now
                                evaluateAudioSegment(currentAudio)
                            }
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            _uiState.update { state ->
                                state.copy(statusMessage = "Lỗi xử lý tín hiệu: ${e.message}")
                            }
                        }
                    }
            } catch (e: Throwable) {
                stopListening()
                _uiState.update { state ->
                    state.copy(statusMessage = "Không thể mở micro: ${e.message}")
                }
            }
        }
    }

    fun stopListening() {
        try {
            audioRecordManager.stopRecording()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        recordingJob?.cancel()
        recordingJob = null
        amplitudeJob?.cancel()
        amplitudeJob = null

        try {
            _uiState.update {
                it.copy(
                    isListening = false,
                    currentAmplitude = 0f,
                    pipelineStage = if (it.result is VerificationResult.Idle) PipelineStage.IDLE else it.pipelineStage
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun toggleListening() {
        if (_uiState.value.isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    suspend fun evaluateAudioSegment(pcmData: ShortArray): VerificationResult = withContext(Dispatchers.Default) {
        val currentState = _uiState.value
        if (pcmData.isEmpty()) return@withContext currentState.result

        val kwsResult = try {
            keywordSpotter.spotKeyword(
                pcm = pcmData,
                keyword = currentState.selectedWakeword,
                keywordTemplate = null,
                kwsThreshold = currentState.kwsThreshold
            )
        } catch (e: Throwable) {
            e.printStackTrace()
            KwsResult(
                detected = false,
                score = 0f,
                energy = keywordSpotter.calculateEnergy(pcmData),
                keyword = currentState.selectedWakeword,
                isNativeEngineUsed = false,
                statusMessage = "Lỗi KWS: ${e.message}"
            )
        }

        val result: VerificationResult = try {
            if (kwsResult.detected) {
                _uiState.update {
                    it.copy(
                        pipelineStage = PipelineStage.VERIFYING_VOICEPRINT,
                        kwsScore = kwsResult.score,
                        statusMessage = "Đã phát hiện từ khóa! Đang xác thực vân giọng..."
                    )
                }

                val masterVector = cachedMasterVector

                if (masterVector == null) {
                    _uiState.update {
                        it.copy(
                            pipelineStage = PipelineStage.IDLE,
                            statusMessage = "Chưa đăng ký vân giọng chủ nhân"
                        )
                    }
                    VerificationResult.NoVoiceprintProfile()
                } else {
                    val currentFeatures = featureExtractor.extractFeatureVector(pcmData)
                    val similarity = voiceprintMatcher.calculateSimilarity(currentFeatures, masterVector)

                    if (similarity >= currentState.voiceprintThreshold) {
                        _uiState.update {
                            it.copy(
                                pipelineStage = PipelineStage.IDLE,
                                voiceprintScore = similarity,
                                isUnlocked = true,
                                statusMessage = "Mở khóa thành công!"
                            )
                        }
                        VerificationResult.Granted(
                            kwsScore = kwsResult.score,
                            voiceprintScore = similarity,
                            detectedKeyword = currentState.selectedWakeword
                        )
                    } else {
                        _uiState.update {
                            it.copy(
                                pipelineStage = PipelineStage.IDLE,
                                voiceprintScore = similarity,
                                isUnlocked = false,
                                statusMessage = "Từ chối: Không phải giọng chủ nhân"
                            )
                        }
                        VerificationResult.DeniedVoiceprint(
                            kwsScore = kwsResult.score,
                            voiceprintScore = similarity,
                            detectedKeyword = currentState.selectedWakeword
                        )
                    }
                }
            } else {
                if (kwsResult.energy >= SPEECH_DENIAL_ENERGY_THRESHOLD) {
                    _uiState.update {
                        it.copy(
                            pipelineStage = PipelineStage.IDLE,
                            kwsScore = kwsResult.score,
                            isUnlocked = false,
                            statusMessage = "Từ chối: Sai từ khóa kích hoạt"
                        )
                    }
                    VerificationResult.DeniedKeyword(
                        energy = kwsResult.energy,
                        kwsScore = kwsResult.score
                    )
                } else {
                    currentState.result
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            currentState.result
        }

        if (result !is VerificationResult.Idle) {
            try {
                synchronized(pcmAccumulator) {
                    pcmAccumulator.clear()
                }
                _uiState.update { it.copy(result = result) }
                stopListening()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        return@withContext result
    }

    fun resetVerification() {
        stopListening()
        try {
            synchronized(pcmAccumulator) {
                pcmAccumulator.clear()
            }
            _uiState.update {
                it.copy(
                    isListening = false,
                    pipelineStage = PipelineStage.IDLE,
                    result = VerificationResult.Idle,
                    isUnlocked = false,
                    kwsScore = 0f,
                    voiceprintScore = 0f,
                    currentAmplitude = 0f,
                    statusMessage = "Sẵn sàng nhận diện 2 tầng"
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        stopListening()
    }
}
