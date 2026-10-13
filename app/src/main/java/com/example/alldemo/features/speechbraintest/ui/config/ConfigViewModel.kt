package com.example.alldemo.features.speechbraintest.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alldemo.features.speechbraintest.data.model.AudioPayload
import com.example.alldemo.features.speechbraintest.data.model.SttConfig
import com.example.alldemo.features.speechbraintest.data.repository.SttRepository
import com.example.alldemo.features.speechbraintest.domain.MatchingStrategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConfigUiState(
    val config: SttConfig = SttConfig(),
    val isTestingApiKey: Boolean = false,
    val apiKeyTestMessage: String? = null,
    val isApiKeyTestSuccess: Boolean? = null
)

class ConfigViewModel(
    private val repository: SttRepository
) : ViewModel() {

    val configState: StateFlow<SttConfig> = repository.configState

    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.configState.collect { config ->
                _uiState.value = _uiState.value.copy(config = config)
            }
        }
    }

    fun setApiKey(apiKey: String) {
        repository.setApiKey(apiKey)
        _uiState.value = _uiState.value.copy(apiKeyTestMessage = null, isApiKeyTestSuccess = null)
    }

    fun setMockMode(enabled: Boolean) {
        repository.setMockMode(enabled)
    }

    fun setLanguageCode(languageCode: String) {
        repository.setLanguageCode(languageCode)
    }

    fun setModel(model: String) {
        repository.setModel(model)
    }

    fun setMatchingStrategy(strategy: MatchingStrategy) {
        repository.setMatchingStrategy(strategy)
    }

    fun setMatchingThreshold(threshold: Float) {
        repository.setMatchingThreshold(threshold)
    }

    fun setDefaultTargetPassword(password: String) {
        repository.setDefaultTargetPassword(password)
    }

    fun testApiKeyConnection() {
        val currentConfig = repository.configState.value
        if (currentConfig.apiKey.isBlank()) {
            _uiState.value = _uiState.value.copy(
                apiKeyTestMessage = "API Key is empty. Please enter a valid Google Cloud Speech-to-Text API key.",
                isApiKeyTestSuccess = false
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingApiKey = true, apiKeyTestMessage = null)
            val testPayload = AudioPayload(
                audioFileName = "test_connection.wav",
                durationMs = 1000L,
                languageCode = currentConfig.languageCode
            )
            val result = repository.recognizeSpeech(testPayload, expectedTranscript = null)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isTestingApiKey = false,
                    apiKeyTestMessage = "API Key Connection Success! Latency: ${it.metrics.latencyMs}ms",
                    isApiKeyTestSuccess = true
                )
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isTestingApiKey = false,
                    apiKeyTestMessage = "API Key Test Failed: ${exception.localizedMessage}",
                    isApiKeyTestSuccess = false
                )
            }
        }
    }

    fun clearApiKeyTestMessage() {
        _uiState.value = _uiState.value.copy(apiKeyTestMessage = null, isApiKeyTestSuccess = null)
    }
}
