package com.example.alldemo.features.demovoicespeechrecognizer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.alldemo.features.demovoicespeechrecognizer.engine.AndroidSpeechRecognizerEngine
import com.example.alldemo.features.demovoicespeechrecognizer.engine.EngineType
import com.example.alldemo.features.demovoicespeechrecognizer.engine.RecognitionLanguage
import com.example.alldemo.features.demovoicespeechrecognizer.engine.RecognitionLog
import com.example.alldemo.features.demovoicespeechrecognizer.engine.RecognitionState
import com.example.alldemo.features.demovoicespeechrecognizer.engine.SherpaOnnxSpeechEngine
import com.example.alldemo.features.demovoicespeechrecognizer.engine.SpeechRecognitionEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VoiceRecognizerUiState(
    val selectedEngine: EngineType = EngineType.ANDROID_NATIVE,
    val selectedLanguage: RecognitionLanguage = RecognitionLanguage.VIETNAMESE,
    val recognitionState: RecognitionState = RecognitionState.Idle,
    val isListening: Boolean = false,
    val currentText: String = "",
    val partialText: String = "",
    val targetKeyword: String = "assistant",
    val isKeywordMatched: Boolean = false,
    val matchedCount: Int = 0,
    val lastLatencyMs: Long = 0L,
    val avgLatencyMs: Long = 0L,
    val minLatencyMs: Long = 0L,
    val maxLatencyMs: Long = 0L,
    val totalResultsCount: Int = 0,
    val historyList: List<RecognitionLog> = emptyList(),
    val errorMessage: String? = null,
    val hasRecordAudioPermission: Boolean = false
)

class VoiceRecognizerViewModel @JvmOverloads constructor(
    application: Application,
    private val androidEngineProvider: ((Application) -> SpeechRecognitionEngine)? = null,
    private val sherpaEngineProvider: ((Application) -> SpeechRecognitionEngine)? = null
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VoiceRecognizerUiState())
    val uiState: StateFlow<VoiceRecognizerUiState> = _uiState.asStateFlow()

    private val androidEngine: SpeechRecognitionEngine by lazy {
        androidEngineProvider?.invoke(getApplication()) ?: AndroidSpeechRecognizerEngine(getApplication())
    }

    private val sherpaEngine: SpeechRecognitionEngine by lazy {
        sherpaEngineProvider?.invoke(getApplication()) ?: SherpaOnnxSpeechEngine(getApplication())
    }

    private var activeEngine: SpeechRecognitionEngine = androidEngine
    private var engineStateJob: Job? = null

    init {
        observeActiveEngine()
    }

    fun selectEngine(engineType: EngineType) {
        if (_uiState.value.selectedEngine == engineType) return

        stopListening()
        activeEngine = when (engineType) {
            EngineType.ANDROID_NATIVE -> androidEngine
            EngineType.SHERPA_ONNX -> sherpaEngine
        }

        _uiState.update {
            it.copy(
                selectedEngine = engineType,
                recognitionState = RecognitionState.Idle,
                isListening = false,
                currentText = "",
                partialText = "",
                errorMessage = null
            )
        }

        observeActiveEngine()
    }

    fun selectLanguage(language: RecognitionLanguage) {
        if (_uiState.value.selectedLanguage == language) return

        val wasListening = _uiState.value.isListening
        if (wasListening) {
            stopListening()
        }

        _uiState.update {
            it.copy(selectedLanguage = language)
        }

        if (wasListening) {
            startListening(language.code)
        }
    }

    fun setTargetKeyword(keyword: String) {
        val currentText = _uiState.value.currentText
        val matched = checkKeywordMatch(currentText, keyword)

        _uiState.update {
            it.copy(
                targetKeyword = keyword,
                isKeywordMatched = matched
            )
        }
    }

    fun startListening(language: String? = null) {
        if (!_uiState.value.hasRecordAudioPermission) {
            _uiState.update {
                it.copy(errorMessage = "Microphone permission is required to start speech recognition.")
            }
            return
        }

        val targetLanguage = language ?: _uiState.value.selectedLanguage.code

        _uiState.update {
            it.copy(
                isListening = true,
                errorMessage = null,
                partialText = ""
            )
        }
        activeEngine.startListening(targetLanguage)
    }

    fun stopListening() {
        activeEngine.stopListening()
        _uiState.update {
            it.copy(isListening = false)
        }
    }

    fun toggleListening(language: String? = null) {
        if (_uiState.value.isListening) {
            stopListening()
        } else {
            startListening(language)
        }
    }

    fun setPermissionGranted(isGranted: Boolean) {
        _uiState.update {
            it.copy(
                hasRecordAudioPermission = isGranted,
                errorMessage = if (isGranted && it.errorMessage?.contains("Microphone permission") == true) null else it.errorMessage
            )
        }
    }

    fun clearHistory() {
        _uiState.update {
            it.copy(
                historyList = emptyList(),
                lastLatencyMs = 0L,
                avgLatencyMs = 0L,
                minLatencyMs = 0L,
                maxLatencyMs = 0L,
                totalResultsCount = 0,
                matchedCount = 0
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun observeActiveEngine() {
        engineStateJob?.cancel()
        engineStateJob = viewModelScope.launch {
            activeEngine.state.collect { state ->
                handleEngineState(state)
            }
        }
    }

    private fun handleEngineState(state: RecognitionState) {
        when (state) {
            is RecognitionState.Idle -> {
                _uiState.update { it.copy(recognitionState = state, isListening = false) }
            }
            is RecognitionState.Initializing,
            is RecognitionState.Listening,
            is RecognitionState.Processing -> {
                _uiState.update { it.copy(recognitionState = state) }
            }
            is RecognitionState.PartialResult -> {
                val matched = checkKeywordMatch(state.text, _uiState.value.targetKeyword)
                updateLatencyMetrics(state.latencyMs)
                _uiState.update {
                    it.copy(
                        recognitionState = state,
                        partialText = state.text,
                        isKeywordMatched = matched
                    )
                }
            }
            is RecognitionState.FinalResult -> {
                val keyword = _uiState.value.targetKeyword
                val isMatched = checkKeywordMatch(state.text, keyword)
                updateLatencyMetrics(state.latencyMs)

                val newLog = RecognitionLog(
                    engineType = _uiState.value.selectedEngine,
                    languageCode = _uiState.value.selectedLanguage.code,
                    text = state.text,
                    latencyMs = state.latencyMs,
                    isKeywordMatched = isMatched,
                    matchedKeyword = if (isMatched) keyword.trim() else null
                )

                _uiState.update { current ->
                    val newHistory = (listOf(newLog) + current.historyList).take(50)
                    val newMatchedCount = if (isMatched) current.matchedCount + 1 else current.matchedCount
                    current.copy(
                        recognitionState = state,
                        currentText = state.text,
                        partialText = "",
                        isKeywordMatched = isMatched,
                        matchedCount = newMatchedCount,
                        historyList = newHistory,
                        isListening = false
                    )
                }
            }
            is RecognitionState.Error -> {
                _uiState.update {
                    it.copy(
                        recognitionState = state,
                        errorMessage = state.message,
                        isListening = false
                    )
                }
            }
        }
    }

    private fun checkKeywordMatch(text: String, keyword: String): Boolean {
        val trimmedKeyword = keyword.trim()
        if (text.isBlank() || trimmedKeyword.isBlank()) return false
        return text.lowercase().contains(trimmedKeyword.lowercase())
    }

    private fun updateLatencyMetrics(latencyMs: Long) {
        if (latencyMs <= 0) return
        _uiState.update { current ->
            val count = current.totalResultsCount + 1
            val min = if (current.minLatencyMs == 0L) latencyMs else minOf(current.minLatencyMs, latencyMs)
            val max = maxOf(current.maxLatencyMs, latencyMs)
            val totalLatencySum = (current.avgLatencyMs * current.totalResultsCount) + latencyMs
            val avg = totalLatencySum / count

            current.copy(
                lastLatencyMs = latencyMs,
                minLatencyMs = min,
                maxLatencyMs = max,
                avgLatencyMs = avg,
                totalResultsCount = count
            )
        }
    }

    override fun onCleared() {
        engineStateJob?.cancel()
        androidEngine.destroy()
        sherpaEngine.destroy()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
                    "VoiceRecognizerViewModel requires Application in CreationExtras"
                }
                VoiceRecognizerViewModel(application)
            }
        }
    }
}
