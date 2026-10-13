package com.example.alldemo.features.speechbraintest.ui.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alldemo.features.speechbraintest.data.model.AudioPayload
import com.example.alldemo.features.speechbraintest.data.model.SttConfig
import com.example.alldemo.features.speechbraintest.data.repository.SttRepository
import com.example.alldemo.features.speechbraintest.data.repository.SttResponseWithMetrics
import com.example.alldemo.features.speechbraintest.domain.MatchingStrategy
import com.example.alldemo.features.speechbraintest.domain.PasswordMatchResult
import com.example.alldemo.features.speechbraintest.domain.PasswordMatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VerificationUiState(
    val config: SttConfig = SttConfig(),
    val targetPassword: String = "Open Sesame",
    val matchingStrategy: MatchingStrategy = MatchingStrategy.FUZZY,
    val matchingThreshold: Float = 0.80f,
    val isRecognizing: Boolean = false,
    val lastResponse: SttResponseWithMetrics? = null,
    val lastMatchResult: PasswordMatchResult? = null,
    val errorMessage: String? = null
)

class VerificationViewModel(
    private val repository: SttRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerificationUiState())
    val uiState: StateFlow<VerificationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.configState.collect { config ->
                _uiState.update { state ->
                    state.copy(
                        config = config,
                        targetPassword = if (state.targetPassword.isBlank()) config.defaultTargetPassword else state.targetPassword,
                        matchingStrategy = config.matchingStrategy,
                        matchingThreshold = config.matchingThreshold
                    )
                }
            }
        }
    }

    fun setTargetPassword(password: String) {
        _uiState.update { it.copy(targetPassword = password) }
        reevaluateMatch()
    }

    fun setMatchingStrategy(strategy: MatchingStrategy) {
        _uiState.update { it.copy(matchingStrategy = strategy) }
        repository.setMatchingStrategy(strategy)
        reevaluateMatch()
    }

    fun setMatchingThreshold(threshold: Float) {
        _uiState.update { it.copy(matchingThreshold = threshold) }
        repository.setMatchingThreshold(threshold)
        reevaluateMatch()
    }

    fun processRecordedAudio(payload: AudioPayload) {
        val currentTarget = _uiState.value.targetPassword
        _uiState.update {
            it.copy(
                isRecognizing = true,
                errorMessage = null,
                lastResponse = null,
                lastMatchResult = null
            )
        }

        viewModelScope.launch {
            val result = repository.recognizeSpeech(payload, expectedTranscript = currentTarget)
            result.onSuccess { response ->
                val matchResult = PasswordMatcher.match(
                    target = currentTarget,
                    transcribed = response.result.transcript,
                    strategy = _uiState.value.matchingStrategy,
                    threshold = _uiState.value.matchingThreshold
                )
                _uiState.update {
                    it.copy(
                        isRecognizing = false,
                        lastResponse = response,
                        lastMatchResult = matchResult
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isRecognizing = false,
                        errorMessage = exception.localizedMessage ?: "Failed to perform speech recognition"
                    )
                }
            }
        }
    }

    private fun reevaluateMatch() {
        val currentState = _uiState.value
        val lastTranscribed = currentState.lastResponse?.result?.transcript ?: return
        val matchResult = PasswordMatcher.match(
            target = currentState.targetPassword,
            transcribed = lastTranscribed,
            strategy = currentState.matchingStrategy,
            threshold = currentState.matchingThreshold
        )
        _uiState.update { it.copy(lastMatchResult = matchResult) }
    }

    fun clearResults() {
        _uiState.update {
            it.copy(
                lastResponse = null,
                lastMatchResult = null,
                errorMessage = null
            )
        }
    }
}
