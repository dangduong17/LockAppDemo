package com.example.alldemo.features.speechbraintest.ui.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alldemo.features.speechbraintest.audio.AudioRecorder
import com.example.alldemo.features.speechbraintest.audio.PcmAudioRecorder
import com.example.alldemo.features.speechbraintest.audio.SimulatedAudioRecorder
import com.example.alldemo.features.speechbraintest.data.model.AudioPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class AudioRecorderUiState(
    val isRecording: Boolean = false,
    val currentAmplitude: Float = 0f,
    val amplitudeHistory: List<Float> = emptyList(),
    val elapsedTimeMs: Long = 0L,
    val targetDurationMs: Long = 3000L,
    val isMockMode: Boolean = false,
    val lastRecordedPayload: AudioPayload? = null,
    val errorMessage: String? = null
)

class AudioRecorderViewModel(
    private var activeRecorder: AudioRecorder = PcmAudioRecorder()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AudioRecorderUiState())
    val uiState: StateFlow<AudioRecorderUiState> = _uiState.asStateFlow()

    private val pcmRecorder = activeRecorder
    private val simulatedRecorder = SimulatedAudioRecorder()

    init {
        observeRecorder(activeRecorder)
    }

    private fun observeRecorder(recorder: AudioRecorder) {
        viewModelScope.launch {
            launch {
                recorder.isRecording.collectLatest { recording ->
                    _uiState.value = _uiState.value.copy(isRecording = recording)
                }
            }
            launch {
                recorder.currentAmplitude.collectLatest { amplitude ->
                    _uiState.value = _uiState.value.copy(currentAmplitude = amplitude)
                }
            }
            launch {
                recorder.amplitudeHistory.collectLatest { history ->
                    _uiState.value = _uiState.value.copy(amplitudeHistory = history)
                }
            }
            launch {
                recorder.elapsedTimeMs.collectLatest { elapsed ->
                    _uiState.value = _uiState.value.copy(elapsedTimeMs = elapsed)
                }
            }
            launch {
                recorder.targetDurationMs.collectLatest { target ->
                    _uiState.value = _uiState.value.copy(targetDurationMs = target)
                }
            }
        }
    }

    fun setTargetDuration(durationMs: Long) {
        val clamped = durationMs.coerceIn(1000L, 3000L)
        _uiState.value = _uiState.value.copy(targetDurationMs = clamped)
    }

    fun toggleMockMode(enabled: Boolean) {
        if (_uiState.value.isRecording) {
            cancelRecording()
        }
        activeRecorder = if (enabled) simulatedRecorder else pcmRecorder
        _uiState.value = _uiState.value.copy(isMockMode = enabled)
        observeRecorder(activeRecorder)
    }

    fun startRecording() {
        val duration = _uiState.value.targetDurationMs
        _uiState.value = _uiState.value.copy(errorMessage = null)

        activeRecorder.startRecording(
            durationMs = duration,
            onComplete = { payload ->
                _uiState.value = _uiState.value.copy(
                    lastRecordedPayload = payload,
                    isRecording = false
                )
            }
        )
    }

    fun stopRecording(): AudioPayload? {
        val payload = activeRecorder.stopRecording()
        if (payload != null) {
            _uiState.value = _uiState.value.copy(
                lastRecordedPayload = payload,
                isRecording = false
            )
        }
        return payload
    }

    fun cancelRecording() {
        activeRecorder.cancelRecording()
        _uiState.value = _uiState.value.copy(
            isRecording = false,
            elapsedTimeMs = 0L,
            currentAmplitude = 0f,
            amplitudeHistory = emptyList()
        )
    }

    fun clearLastPayload() {
        _uiState.value = _uiState.value.copy(lastRecordedPayload = null)
    }
}
