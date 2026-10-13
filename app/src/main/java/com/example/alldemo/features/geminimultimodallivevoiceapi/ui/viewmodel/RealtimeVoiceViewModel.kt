package com.example.alldemo.features.geminimultimodallivevoiceapi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alldemo.features.geminimultimodallivevoiceapi.data.model.MessageSender
import com.example.alldemo.features.geminimultimodallivevoiceapi.data.repository.RealtimeRepository
import com.example.alldemo.features.geminimultimodallivevoiceapi.ui.state.RealtimeUiState
import com.example.alldemo.features.geminimultimodallivevoiceapi.ui.state.VerificationStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RealtimeVoiceViewModel(
    val repository: RealtimeRepository = RealtimeRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RealtimeUiState())
    val uiState: StateFlow<RealtimeUiState> = _uiState.asStateFlow()

    init {
        observeRepositoryState()
    }

    private fun observeRepositoryState() {
        viewModelScope.launch {
            repository.connectionState.collect { connState ->
                _uiState.update { it.copy(connectionState = connState) }
            }
        }

        viewModelScope.launch {
            repository.isRecording.collect { recording ->
                _uiState.update { currentState ->
                    val newStatus = if (recording && currentState.verificationStatus != VerificationStatus.SUCCESS) {
                        VerificationStatus.LISTENING
                    } else {
                        currentState.verificationStatus
                    }
                    currentState.copy(isRecording = recording, verificationStatus = newStatus)
                }
            }
        }

        viewModelScope.launch {
            repository.audioLevel.collect { level ->
                _uiState.update { it.copy(audioLevel = level) }
            }
        }

        viewModelScope.launch {
            repository.messages.collect { msgList ->
                _uiState.update { it.copy(messages = msgList) }
                val lastUserMsg = msgList.lastOrNull { it.sender == MessageSender.USER }
                if (lastUserMsg != null && lastUserMsg.text.isNotBlank() && lastUserMsg.text != _uiState.value.lastVerifiedTranscript) {
                    verifyPassword(lastUserMsg.text)
                }
            }
        }

        viewModelScope.launch {
            repository.errorState.collect { errorMsg ->
                _uiState.update { it.copy(errorMessage = errorMsg) }
            }
        }
    }

    fun updateApiKey(apiKey: String) {
        _uiState.update { it.copy(apiKey = apiKey.trim()) }
    }

    fun updateSelectedModel(model: String) {
        _uiState.update { it.copy(selectedModel = model.trim()) }
    }

    fun updateTargetPassword(password: String) {
        _uiState.update { it.copy(targetPassword = password) }
    }

    fun normalizeText(text: String): String {
        val normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
        return normalized
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .trim()
    }

    fun verifyPassword(transcript: String) {
        if (transcript.isBlank()) return
        val currentState = _uiState.value
        val normTarget = normalizeText(currentState.targetPassword)
        val normTranscript = normalizeText(transcript)

        if (normTarget.isBlank() || normTranscript.isBlank()) return

        val isMatch = normTranscript.contains(normTarget) || normTarget.contains(normTranscript)
        val newStatus = if (isMatch) VerificationStatus.SUCCESS else VerificationStatus.FAILED

        _uiState.update {
            it.copy(
                verificationStatus = newStatus,
                lastVerifiedTranscript = transcript
            )
        }
    }

    fun resetVerification() {
        _uiState.update {
            it.copy(
                verificationStatus = VerificationStatus.IDLE,
                lastVerifiedTranscript = ""
            )
        }
    }

    fun connect() {
        val currentState = _uiState.value
        if (currentState.apiKey.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập Google AI Studio API Key.") }
            return
        }
        repository.connect(
            apiKey = currentState.apiKey,
            model = currentState.selectedModel
        )
    }

    fun disconnect() {
        repository.disconnect()
    }

    fun startRecording(): Boolean {
        val started = repository.startStreamingAudio(viewModelScope)
        if (started) {
            _uiState.update {
                if (it.verificationStatus != VerificationStatus.SUCCESS) {
                    it.copy(verificationStatus = VerificationStatus.LISTENING)
                } else it
            }
        }
        return started
    }

    fun stopRecording() {
        repository.stopStreamingAudio(autoCommitAndRespond = true)
    }

    fun toggleRecording() {
        if (_uiState.value.isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    fun sendTextMessage(text: String) {
        repository.sendTextMessage(text)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearMessages() {
        repository.clearMessages()
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnect()
    }
}
