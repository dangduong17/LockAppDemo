package com.example.alldemo.features.huggingfacetransformersjsonnxwebruntime

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RecordingState {
    IDLE,
    RECORDING,
    PROCESSING
}

enum class AuthStatus {
    IDLE,
    SUCCESS,
    MISMATCH
}

class VoicePasswordViewModel @JvmOverloads constructor(
    application: Application,
    val audioCaptureManager: AudioCaptureManager = AudioCaptureManager(),
    val javascriptBridge: JavascriptBridge = JavascriptBridge(),
    val webViewEngine: WebViewEngine = WebViewEngine(application.applicationContext, javascriptBridge),
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "VoicePasswordViewModel"
    }

    private val _targetPassword = MutableStateFlow("mở khóa")
    val targetPassword: StateFlow<String> = _targetPassword.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("auto")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _webModelStatus = MutableStateFlow("Đang khởi tạo mô hình AI...")
    val webModelStatus: StateFlow<String> = _webModelStatus.asStateFlow()

    private val _recognizedTranscript = MutableStateFlow("")
    val recognizedTranscript: StateFlow<String> = _recognizedTranscript.asStateFlow()

    private val _authStatus = MutableStateFlow(AuthStatus.IDLE)
    val authStatus: StateFlow<AuthStatus> = _authStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        setupJavascriptBridgeListeners()
    }

    private fun setupJavascriptBridgeListeners() {
        javascriptBridge.setOnStatusChangeListener { status ->
            Log.d(TAG, "JS Status changed: $status")
            _webModelStatus.value = mapJsStatusToVietnamese(status)
        }

        javascriptBridge.setOnSpeechResultListener { recognizedText, _ ->
            Log.d(TAG, "Speech result received: '$recognizedText'")
            _recognizedTranscript.value = recognizedText
            _recordingState.value = RecordingState.IDLE

            if (recognizedText.isNotBlank()) {
                val isMatch = comparePassword(_targetPassword.value, recognizedText)
                _authStatus.value = if (isMatch) AuthStatus.SUCCESS else AuthStatus.MISMATCH
            } else {
                _authStatus.value = AuthStatus.MISMATCH
            }
        }

        javascriptBridge.setOnErrorListener { error ->
            Log.e(TAG, "JS Error received: $error")
            _webModelStatus.value = "Lỗi: $error"
            _errorMessage.value = error
            _recordingState.value = RecordingState.IDLE
        }
    }

    fun setTargetPassword(password: String) {
        _targetPassword.value = password
    }

    fun setSelectedLanguage(language: String) {
        _selectedLanguage.value = language
    }

    fun startRecording(): Boolean {
        _authStatus.value = AuthStatus.IDLE
        _recognizedTranscript.value = ""
        _errorMessage.value = null

        val started = audioCaptureManager.startRecording()
        if (started) {
            _recordingState.value = RecordingState.RECORDING
        } else {
            _errorMessage.value = "Không thể mở thiết bị ghi âm."
            _recordingState.value = RecordingState.IDLE
        }
        return started
    }

    fun stopRecordingAndProcess() {
        if (_recordingState.value != RecordingState.RECORDING) return

        _recordingState.value = RecordingState.PROCESSING
        val pcmBytes = audioCaptureManager.stopRecording()

        if (pcmBytes.isEmpty()) {
            _errorMessage.value = "Không thu được âm thanh."
            _recordingState.value = RecordingState.IDLE
            _authStatus.value = AuthStatus.MISMATCH
            return
        }

        webViewEngine.processAudioBytes(pcmBytes, _selectedLanguage.value) { result ->
            Log.d(TAG, "processAudioBytes JS evaluate result: $result")
        }
    }

    fun resetAuthStatus() {
        _authStatus.value = AuthStatus.IDLE
        _recognizedTranscript.value = ""
        _errorMessage.value = null
    }

    fun comparePassword(target: String, transcript: String): Boolean {
        val cleanTarget = normalizeText(target)
        val cleanTranscript = normalizeText(transcript)

        if (cleanTarget.isEmpty() || cleanTranscript.isEmpty()) return false

        return (cleanTarget == cleanTranscript) ||
                cleanTranscript.contains(cleanTarget) ||
                cleanTarget.contains(cleanTranscript)
    }

    private fun normalizeText(text: String): String {
        return text.trim()
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), "") // Strip punctuation
            .replace(Regex("\\s+"), " ") // Normalize spaces
            .trim()
    }

    private fun mapJsStatusToVietnamese(status: String): String {
        return when {
            status == "INITIALIZING" -> "Đang khởi tạo hệ thống..."
            status == "LOADING_MODEL" -> "Đang tải mô hình Whisper AI..."
            status == "READY" -> "Mô hình AI đã sẵn sàng"
            status == "PROCESSING" -> "Đang nhận diện giọng nói..."
            status.startsWith("ERROR") -> "Lỗi mô hình AI: ${status.removePrefix("ERROR: ")}"
            else -> status
        }
    }

    override fun onCleared() {
        try {
            audioCaptureManager.stopRecording()
            webViewEngine.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up ViewModel resources", e)
        }
    }
}

class VoicePasswordViewModelFactory(
    private val application: Application,
    private val audioCaptureManager: AudioCaptureManager = AudioCaptureManager(),
    private val javascriptBridge: JavascriptBridge = JavascriptBridge(),
    private val webViewEngine: WebViewEngine? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VoicePasswordViewModel::class.java)) {
            val bridge = javascriptBridge
            val engine = webViewEngine ?: WebViewEngine(application.applicationContext, bridge)
            return VoicePasswordViewModel(application, audioCaptureManager, bridge, engine) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
