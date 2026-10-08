package com.example.alldemo.features.wakewordvoiceprint.ui.enrollment

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.alldemo.features.wakewordvoiceprint.audio.AudioFeatureExtractor
import com.example.alldemo.features.wakewordvoiceprint.audio.AudioRecordManager
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

/**
 * ViewModel managing voiceprint enrollment flow.
 */
class EnrollmentViewModel(
    private val voiceprintRepository: VoiceprintRepository,
    private val audioRecordManager: AudioRecordManager = AudioRecordManager(),
    private val featureExtractor: AudioFeatureExtractor = AudioFeatureExtractor()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EnrollmentUiState())
    val uiState: StateFlow<EnrollmentUiState> = _uiState.asStateFlow()

    val amplitude: StateFlow<Float> = audioRecordManager.amplitude

    private var recordingJob: Job? = null
    private val currentPcmChunks = mutableListOf<ShortArray>()

    companion object {
        const val MIN_SAMPLES_FOR_ENROLLMENT = 3
        const val MAX_SAMPLES_FOR_ENROLLMENT = 5
        const val MIN_AUDIO_SAMPLES_THRESHOLD = 4800
    }

    fun startRecording() {
        val currentState = _uiState.value
        if (currentState.status is EnrollmentStatus.Recording || currentState.status is EnrollmentStatus.Processing) {
            return
        }

        synchronized(currentPcmChunks) {
            currentPcmChunks.clear()
        }

        _uiState.update { state ->
            state.copy(
                status = EnrollmentStatus.Recording(
                    sampleIndex = state.currentSampleIndex,
                    totalRequired = state.totalRequiredSamples
                ),
                errorMessage = null
            )
        }

        recordingJob?.cancel()
        recordingJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                audioRecordManager.startRecordingFlow()
                    .catch { e -> e.printStackTrace() }
                    .collect { pcmChunk ->
                        try {
                            synchronized(currentPcmChunks) {
                                currentPcmChunks.add(pcmChunk)
                            }
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun stopRecording() {
        if (_uiState.value.status !is EnrollmentStatus.Recording) return

        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecordManager.stopRecording()
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        _uiState.update { it.copy(status = EnrollmentStatus.Processing) }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val combinedPcm = withContext(Dispatchers.Default) {
                    try {
                        synchronized(currentPcmChunks) {
                            val totalSize = currentPcmChunks.sumOf { it.size }
                            val result = ShortArray(totalSize)
                            var offset = 0
                            for (chunk in currentPcmChunks) {
                                System.arraycopy(chunk, 0, result, offset, chunk.size)
                                offset += chunk.size
                            }
                            result
                        }
                    } catch (e: Throwable) {
                        ShortArray(0)
                    }
                }

                if (combinedPcm.size < MIN_AUDIO_SAMPLES_THRESHOLD) {
                    _uiState.update { state ->
                        val errorMsg = "Âm thanh quá ngắn (${combinedPcm.size} mẫu). Vui lòng nói từ khóa rõ ràng hơn (tối thiểu 0.5s)."
                        state.copy(
                            status = EnrollmentStatus.Error(errorMsg),
                            errorMessage = errorMsg
                        )
                    }
                    return@launch
                }

                val featureVector = withContext(Dispatchers.Default) {
                    try {
                        featureExtractor.extractFeatureVector(combinedPcm)
                    } catch (e: Throwable) {
                        FloatArray(0)
                    }
                }

                if (featureVector.isEmpty() || featureVector.all { it == 0f }) {
                    _uiState.update { state ->
                        val errorMsg = "Không thể trích xuất đặc trưng âm thanh. Vui lòng thử lại."
                        state.copy(
                            status = EnrollmentStatus.Error(errorMsg),
                            errorMessage = errorMsg
                        )
                    }
                    return@launch
                }

                val updatedVectors = _uiState.value.recordedSampleVectors + featureVector
                val recordedCount = updatedVectors.size
                val targetRequired = _uiState.value.totalRequiredSamples

                if (recordedCount >= targetRequired) {
                    val avgVector = withContext(Dispatchers.Default) {
                        computeAverageVector(updatedVectors)
                    }

                    withContext(Dispatchers.IO) {
                        try {
                            voiceprintRepository.saveVoiceprint(avgVector)
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    }

                    _uiState.update { state ->
                        state.copy(
                            recordedSampleVectors = updatedVectors,
                            recordedSamplesCount = recordedCount,
                            status = EnrollmentStatus.Success(recordedCount, avgVector)
                        )
                    }
                } else {
                    _uiState.update { state ->
                        state.copy(
                            recordedSampleVectors = updatedVectors,
                            recordedSamplesCount = recordedCount,
                            currentSampleIndex = recordedCount + 1,
                            status = EnrollmentStatus.Idle
                        )
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                _uiState.update { state ->
                    val errorMsg = "Lỗi xử lý đăng ký: ${e.message}"
                    state.copy(
                        status = EnrollmentStatus.Error(errorMsg),
                        errorMessage = errorMsg
                    )
                }
            }
        }
    }

    fun finishEarlyAndSave() {
        val currentState = _uiState.value
        if (currentState.recordedSamplesCount < MIN_SAMPLES_FOR_ENROLLMENT) return

        _uiState.update { it.copy(status = EnrollmentStatus.Processing) }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val avgVector = withContext(Dispatchers.Default) {
                    computeAverageVector(currentState.recordedSampleVectors)
                }

                withContext(Dispatchers.IO) {
                    try {
                        voiceprintRepository.saveVoiceprint(avgVector)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }

                _uiState.update { state ->
                    state.copy(
                        status = EnrollmentStatus.Success(state.recordedSamplesCount, avgVector)
                    )
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun resetEnrollment() {
        try {
            recordingJob?.cancel()
            recordingJob = null
            audioRecordManager.stopRecording()
            synchronized(currentPcmChunks) {
                currentPcmChunks.clear()
            }

            _uiState.update {
                EnrollmentUiState(
                    status = EnrollmentStatus.Idle,
                    currentSampleIndex = 1,
                    totalRequiredSamples = it.totalRequiredSamples,
                    recordedSamplesCount = 0,
                    recordedSampleVectors = emptyList(),
                    errorMessage = null
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun setTotalRequiredSamples(count: Int) {
        if (count !in MIN_SAMPLES_FOR_ENROLLMENT..MAX_SAMPLES_FOR_ENROLLMENT) return
        _uiState.update { state ->
            if (state.recordedSamplesCount == 0 && state.status is EnrollmentStatus.Idle) {
                state.copy(totalRequiredSamples = count)
            } else {
                state
            }
        }
    }

    fun computeAverageVector(vectors: List<FloatArray>): FloatArray {
        if (vectors.isEmpty()) return FloatArray(26)
        return try {
            val dim = vectors[0].size
            val avg = FloatArray(dim)
            for (vec in vectors) {
                for (i in 0 until minOf(dim, vec.size)) {
                    avg[i] += vec[i]
                }
            }
            val countFloat = vectors.size.toFloat()
            for (i in 0 until dim) {
                avg[i] /= countFloat
            }
            featureExtractor.normalizeL2(avg)
        } catch (e: Throwable) {
            FloatArray(26)
        }
    }

    override fun onCleared() {
        try {
            recordingJob?.cancel()
            audioRecordManager.stopRecording()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}

class EnrollmentViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EnrollmentViewModel::class.java)) {
            val repository = VoiceprintRepository(context.applicationContext)
            val recordManager = AudioRecordManager(context.applicationContext)
            val featureExtractor = AudioFeatureExtractor()
            return EnrollmentViewModel(repository, recordManager, featureExtractor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
