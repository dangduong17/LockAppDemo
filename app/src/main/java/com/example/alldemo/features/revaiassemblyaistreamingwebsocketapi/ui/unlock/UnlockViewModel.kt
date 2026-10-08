package com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.ui.unlock

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.data.ApiProvider
import com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.data.AppSettings
import com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.data.SettingsRepository
import com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.streaming.StreamingStatus
import com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.streaming.VoiceStreamingManager
import com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.util.PasswordVerifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UnlockViewModel(
    private val settingsRepository: SettingsRepository,
    private val voiceStreamingManager: VoiceStreamingManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnlockUiState())
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    private var currentSettings: AppSettings = AppSettings()

    init {
        // Collect settings
        viewModelScope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                currentSettings = settings
                _uiState.update { current ->
                    current.copy(
                        targetPassword = settings.targetPassword,
                        selectedProvider = settings.selectedProvider,
                        hasApiKey = getApiKeyForProvider(settings.selectedProvider, settings).isNotBlank()
                    )
                }
            }
        }

        // Collect voice streaming state
        viewModelScope.launch {
            voiceStreamingManager.state.collectLatest { streamingState ->
                val fullTranscript = streamingState.fullTranscript
                val targetPass = _uiState.value.targetPassword

                // Check for instant password unlock
                val isMatched = PasswordVerifier.verifyPassword(fullTranscript, targetPass)

                if (isMatched && _uiState.value.status != UnlockStatus.UNLOCKED) {
                    _uiState.update {
                        it.copy(
                            status = UnlockStatus.UNLOCKED,
                            currentTranscript = fullTranscript,
                            errorMessage = null
                        )
                    }
                    voiceStreamingManager.stopStreaming()
                    return@collectLatest
                }

                // If already unlocked, keep UNLOCKED state
                if (_uiState.value.status == UnlockStatus.UNLOCKED) {
                    return@collectLatest
                }

                when (streamingState.status) {
                    StreamingStatus.CONNECTING -> {
                        _uiState.update {
                            it.copy(
                                status = UnlockStatus.CONNECTING,
                                currentTranscript = fullTranscript,
                                errorMessage = null
                            )
                        }
                    }
                    StreamingStatus.RECORDING, StreamingStatus.CONNECTED -> {
                        _uiState.update {
                            it.copy(
                                status = UnlockStatus.LISTENING,
                                currentTranscript = fullTranscript,
                                errorMessage = null
                            )
                        }
                    }
                    StreamingStatus.ERROR -> {
                        _uiState.update {
                            it.copy(
                                status = UnlockStatus.ERROR,
                                currentTranscript = fullTranscript,
                                errorMessage = streamingState.errorMessage ?: "Lỗi kết nối WebSocket"
                            )
                        }
                    }
                    StreamingStatus.IDLE -> {
                        val prevStatus = _uiState.value.status
                        if (prevStatus == UnlockStatus.LISTENING || prevStatus == UnlockStatus.CONNECTING) {
                            val statusAfterStop = if (fullTranscript.isNotBlank() && !isMatched) {
                                UnlockStatus.FAILED
                            } else {
                                UnlockStatus.IDLE
                            }
                            _uiState.update {
                                it.copy(
                                    status = statusAfterStop,
                                    currentTranscript = fullTranscript
                                )
                            }
                        }
                    }
                    StreamingStatus.STOPPING -> {
                        // In transition, retain transcript
                        _uiState.update { it.copy(currentTranscript = fullTranscript) }
                    }
                }
            }
        }
    }

    fun startListening() {
        val apiKey = getApiKeyForProvider(currentSettings.selectedProvider, currentSettings)

        if (apiKey.isBlank()) {
            _uiState.update {
                it.copy(
                    status = UnlockStatus.ERROR,
                    hasApiKey = false,
                    errorMessage = "Chưa cấu hình API Key cho ${currentSettings.selectedProvider.displayName}. Vui lòng mở Cài đặt để cập nhật."
                )
            }
            return
        }

        voiceStreamingManager.clearTranscripts()
        _uiState.update {
            it.copy(
                status = UnlockStatus.CONNECTING,
                currentTranscript = "",
                errorMessage = null,
                hasApiKey = true
            )
        }

        voiceStreamingManager.startStreaming(currentSettings.selectedProvider, apiKey)
    }

    fun stopListening() {
        voiceStreamingManager.stopStreaming()
        if (_uiState.value.status == UnlockStatus.LISTENING || _uiState.value.status == UnlockStatus.CONNECTING) {
            val isMatched = PasswordVerifier.verifyPassword(_uiState.value.currentTranscript, _uiState.value.targetPassword)
            val newStatus = if (isMatched) {
                UnlockStatus.UNLOCKED
            } else if (_uiState.value.currentTranscript.isNotBlank()) {
                UnlockStatus.FAILED
            } else {
                UnlockStatus.IDLE
            }
            _uiState.update { it.copy(status = newStatus) }
        }
    }

    fun reset() {
        voiceStreamingManager.stopStreaming()
        voiceStreamingManager.clearTranscripts()
        _uiState.update {
            it.copy(
                status = UnlockStatus.IDLE,
                currentTranscript = "",
                errorMessage = null
            )
        }
    }

    private fun getApiKeyForProvider(provider: ApiProvider, settings: AppSettings): String {
        return when (provider) {
            ApiProvider.ASSEMBLY_AI -> settings.assemblyAiApiKey
            ApiProvider.REV_AI -> settings.revAiApiKey
        }
    }

    class Factory(
        private val application: Application,
        private val voiceStreamingManager: VoiceStreamingManager = VoiceStreamingManager()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repository = SettingsRepository(application.applicationContext)
            return UnlockViewModel(repository, voiceStreamingManager) as T
        }
    }
}
