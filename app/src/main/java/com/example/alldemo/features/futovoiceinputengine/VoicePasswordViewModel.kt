package com.example.alldemo.features.futovoiceinputengine

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel managing Voice Password logic, state management, and SpeechRecognizer integration.
 */
class VoicePasswordViewModel : ViewModel() {

    companion object {
        private const val TAG = "VoicePasswordViewModel"
        val FUTO_COMPONENT = ComponentName(
            "org.futo.voiceinput",
            "org.futo.voiceinput.VoiceInputRecognitionService"
        )
    }

    private val _uiState = MutableStateFlow(VoicePasswordUiState())
    val uiState: StateFlow<VoicePasswordUiState> = _uiState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    fun updateTargetPassword(password: String) {
        _uiState.update { currentState ->
            val isMatched = checkMatch(currentState.recognizedText, password)
            currentState.copy(
                targetPassword = password,
                isMatched = isMatched
            )
        }
    }

    fun startListening(context: Context, serviceComponent: ComponentName? = null) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _uiState.update {
                it.copy(
                    isListening = false,
                    errorMessage = "Speech recognition is not available on this device."
                )
            }
            return
        }

        stopListening()

        val targetComponent = serviceComponent ?: FUTO_COMPONENT
        var engineName = ""

        val recognizer: SpeechRecognizer? = try {
            val isComponentAvailable = try {
                context.packageManager.getServiceInfo(targetComponent, 0)
                true
            } catch (_: Exception) {
                false
            }

            if (!isComponentAvailable) {
                throw IllegalStateException("Service component $targetComponent is not installed.")
            }

            engineName = if (targetComponent == FUTO_COMPONENT) {
                "FUTO Voice Input Engine (Offline Mode)"
            } else {
                targetComponent.shortClassName
            }
            SpeechRecognizer.createSpeechRecognizer(context, targetComponent)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create SpeechRecognizer with $targetComponent, falling back to system default engine", e)
            null
        }

        var startedSuccessfully = false
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }

        if (recognizer != null) {
            try {
                recognizer.setRecognitionListener(createRecognitionListener())
                recognizer.startListening(intent)
                speechRecognizer = recognizer
                startedSuccessfully = true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start listening with $engineName, falling back to system default engine", e)
                try {
                    recognizer.destroy()
                } catch (_: Exception) {}
            }
        }

        if (!startedSuccessfully) {
            try {
                val fallbackRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                engineName = "System Default Engine"
                fallbackRecognizer.setRecognitionListener(createRecognitionListener())
                fallbackRecognizer.startListening(intent)
                speechRecognizer = fallbackRecognizer
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start listening with system default SpeechRecognizer", e)
                _uiState.update {
                    it.copy(
                        isListening = false,
                        errorMessage = e.localizedMessage ?: "Failed to start speech recognition"
                    )
                }
                return
            }
        }

        Log.d(TAG, "SpeechRecognizer started using engine: $engineName")

        val finalEngineName = engineName
        _uiState.update {
            it.copy(
                isListening = true,
                errorMessage = null,
                recognizedText = "",
                partialResults = "",
                isMatched = false,
                activeEngine = finalEngineName
            )
        }
    }

    fun stopListening() {
        speechRecognizer?.let {
            try {
                it.stopListening()
                it.destroy()
            } catch (_: Exception) {
            }
        }
        speechRecognizer = null
        _uiState.update { it.copy(isListening = false) }
    }

    fun processResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val topMatch = matches?.firstOrNull() ?: ""

        _uiState.update { currentState ->
            val target = currentState.targetPassword
            val hasCandidateMatch = matches?.any { checkMatch(it, target) } ?: false
            val isMatched = hasCandidateMatch || checkMatch(topMatch, target)

            val matchedText = matches?.firstOrNull { checkMatch(it, target) } ?: topMatch

            currentState.copy(
                recognizedText = matchedText,
                isListening = false,
                isMatched = isMatched,
                errorMessage = null
            )
        }
    }

    fun processPartialResults(partialResultsBundle: Bundle?) {
        val matches = partialResultsBundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partialText = matches?.firstOrNull() ?: ""
        _uiState.update { currentState ->
            currentState.copy(partialResults = partialText)
        }
    }

    fun processError(errorCode: Int) {
        val message = getErrorMessage(errorCode)
        _uiState.update {
            it.copy(
                isListening = false,
                errorMessage = message
            )
        }
    }

    fun onRecognizedText(text: String) {
        _uiState.update { currentState ->
            val isMatched = checkMatch(text, currentState.targetPassword)
            currentState.copy(
                recognizedText = text,
                isListening = false,
                isMatched = isMatched,
                errorMessage = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetRecognition() {
        _uiState.update {
            it.copy(
                recognizedText = "",
                partialResults = "",
                isMatched = false,
                errorMessage = null
            )
        }
    }

    fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _uiState.update { it.copy(errorMessage = null) }
            }

            override fun onBeginningOfSpeech() {
                _uiState.update { it.copy(isListening = true, errorMessage = null) }
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _uiState.update { it.copy(isListening = false) }
            }

            override fun onError(error: Int) {
                processError(error)
            }

            override fun onResults(results: Bundle?) {
                processResults(results)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                processPartialResults(partialResults)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun checkMatch(recognized: String, target: String): Boolean {
        val cleanRecognized = recognized.trim()
        val cleanTarget = target.trim()
        return cleanTarget.isNotEmpty() && cleanRecognized.equals(cleanTarget, ignoreCase = true)
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }

    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech input recognized"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
            else -> "Speech recognition error: $errorCode"
        }
    }
}
