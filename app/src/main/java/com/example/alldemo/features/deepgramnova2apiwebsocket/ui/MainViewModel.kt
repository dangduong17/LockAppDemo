package com.example.alldemo.features.deepgramnova2apiwebsocket.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.alldemo.features.deepgramnova2apiwebsocket.data.audio.AudioRecordStreamer
import com.example.alldemo.features.deepgramnova2apiwebsocket.data.pref.AppPreferences
import com.example.alldemo.features.deepgramnova2apiwebsocket.data.pref.DefaultAppPreferences
import com.example.alldemo.features.deepgramnova2apiwebsocket.data.remote.DeepgramWebSocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer

data class MainUiState(
    val apiKey: String = "",
    val password: String = "mở khóa",
    val isListening: Boolean = false,
    val transcript: String = "",
    val isUnlocked: Boolean = false,
    val errorMessage: String? = null
)

class MainViewModel(
    private val audioRecordStreamer: AudioRecordStreamer = AudioRecordStreamer(),
    private val webSocketManager: DeepgramWebSocketManager = DeepgramWebSocketManager(),
    private val appPreferences: AppPreferences? = null
) : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"

        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(
                    appPreferences = DefaultAppPreferences(context.applicationContext)
                ) as T
            }
        }

        fun checkPasswordMatch(transcript: String, password: String): Boolean {
            if (password.isBlank() || transcript.isBlank()) return false

            val lowerTranscript = transcript.lowercase().trim()
            val lowerPassword = password.lowercase().trim()

            if ((lowerTranscript.contains(lowerPassword)) || (lowerTranscript == lowerPassword)) {
                return true
            }

            val normTranscript = normalizeDiacritics(transcript)
            val normPassword = normalizeDiacritics(password)

            return (normTranscript.contains(normPassword)) || (normTranscript == normPassword)
        }

        private fun normalizeDiacritics(input: String): String {
            return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
                .lowercase()
                .replace(Regex("\\s+"), " ")
                .trim()
        }
    }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        appPreferences?.let { prefs ->
            val savedApiKey = prefs.getApiKey()
            val savedPassword = prefs.getPassword().ifBlank { "mở khóa" }
            _uiState.update { currentState ->
                currentState.copy(
                    apiKey = savedApiKey,
                    password = savedPassword
                )
            }
        }
    }

    fun updateApiKey(apiKey: String) {
        _uiState.update { it.copy(apiKey = apiKey, errorMessage = null) }
        appPreferences?.saveApiKey(apiKey)
    }

    fun updatePassword(password: String) {
        _uiState.update { currentState ->
            val newState = currentState.copy(password = password)
            val matched = checkPasswordMatch(newState.transcript, newState.password)
            newState.copy(isUnlocked = matched)
        }
        appPreferences?.savePassword(password)
    }

    fun toggleListening() {
        if (_uiState.value.isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    @Suppress("MissingPermission")
    fun startListening() {
        val currentApiKey = _uiState.value.apiKey
        if (currentApiKey.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập Deepgram API Key") }
            return
        }

        Log.d(TAG, "startListening called with API key length=${currentApiKey.length}")

        _uiState.update {
            it.copy(
                isListening = true,
                errorMessage = null,
                transcript = "",
                isUnlocked = false
            )
        }

        webSocketManager.connect(
            apiKey = currentApiKey,
            onResults = { transcriptText, isFinal ->
                Log.d(TAG, "onResults: '$transcriptText' (isFinal=$isFinal)")
                if (transcriptText.isNotBlank()) {
                    viewModelScope.launch(Dispatchers.Main) {
                        _uiState.update { currentState ->
                            val isMatch = checkPasswordMatch(transcriptText, currentState.password)
                            Log.d(TAG, "Transcript match check against '${currentState.password}': isMatch=$isMatch")
                            currentState.copy(
                                transcript = transcriptText,
                                isUnlocked = currentState.isUnlocked || isMatch
                            )
                        }
                    }
                }
            },
            onError = { error ->
                Log.e(TAG, "WebSocket onError: ${error.message}", error)
                viewModelScope.launch(Dispatchers.Main) {
                    stopListeningInternal()
                    val errorMsg = error.localizedMessage ?: error.message ?: "Lỗi kết nối Deepgram WebSocket"
                    _uiState.update {
                        it.copy(
                            isListening = false,
                            errorMessage = errorMsg
                        )
                    }
                }
            },
            onOpen = {
                Log.d(TAG, "WebSocket onOpen callback received")
                viewModelScope.launch(Dispatchers.Main) {
                    try {
                        audioRecordStreamer.startRecording(
                            coroutineScope = viewModelScope,
                            webSocketManager = webSocketManager,
                            onError = { recordError ->
                                Log.e(TAG, "AudioRecord onError: ${recordError.message}", recordError)
                                viewModelScope.launch(Dispatchers.Main) {
                                    stopListeningInternal()
                                    _uiState.update { state ->
                                        state.copy(
                                            isListening = false,
                                            errorMessage = recordError.localizedMessage ?: recordError.message ?: "Lỗi ghi âm"
                                        )
                                    }
                                }
                            }
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception starting AudioRecord: ${e.message}", e)
                        stopListeningInternal()
                        _uiState.update { state ->
                            state.copy(
                                isListening = false,
                                errorMessage = e.localizedMessage ?: e.message ?: "Không thể khởi động ghi âm"
                            )
                        }
                    }
                }
            },
            onClose = { code, reason ->
                Log.d(TAG, "WebSocket onClose callback: code=$code, reason=$reason")
                viewModelScope.launch(Dispatchers.Main) {
                    if (_uiState.value.isListening) {
                        stopListeningInternal()
                        _uiState.update { state ->
                            val errorMsg = if (code != 1000 && state.errorMessage == null) {
                                "Kết nối bị đóng ($code): ${reason.ifBlank { "Không rõ nguyên nhân" }}"
                            } else {
                                state.errorMessage
                            }
                            state.copy(
                                isListening = false,
                                errorMessage = errorMsg
                            )
                        }
                    }
                }
            }
        )
    }

    fun stopListening() {
        Log.d(TAG, "stopListening requested by user")
        stopListeningInternal()
        _uiState.update { it.copy(isListening = false) }
    }

    private fun stopListeningInternal() {
        try {
            audioRecordStreamer.stopRecording()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping AudioRecordStreamer", e)
        }
        try {
            webSocketManager.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "Error disconnecting DeepgramWebSocketManager", e)
        }
    }

    fun resetUnlockState() {
        _uiState.update { it.copy(isUnlocked = false, transcript = "") }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        stopListeningInternal()
    }
}
