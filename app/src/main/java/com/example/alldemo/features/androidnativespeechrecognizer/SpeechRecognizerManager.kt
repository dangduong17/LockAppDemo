package com.example.alldemo.features.androidnativespeechrecognizer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpeechRecognizerManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _rmsChanged = MutableStateFlow(0f)
    val rmsChanged: StateFlow<Float> = _rmsChanged.asStateFlow()

    init {
        initSpeechRecognizer()
    }

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    private fun initSpeechRecognizer() {
        if (speechRecognizer == null && isRecognitionAvailable()) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createRecognitionListener())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create SpeechRecognizer", e)
                _errorMessage.value = "Failed to initialize SpeechRecognizer: ${e.localizedMessage}"
            }
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "onReadyForSpeech")
                _isListening.value = true
                _errorMessage.value = null
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech")
                _isListening.value = true
            }

            override fun onRmsChanged(rmsdB: Float) {
                _rmsChanged.value = rmsdB
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                Log.d(TAG, "onBufferReceived")
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech")
                _isListening.value = false
            }

            override fun onError(error: Int) {
                val errorMsg = getErrorText(error)
                Log.e(TAG, "onError: $error ($errorMsg)")
                _errorMessage.value = errorMsg
                _isListening.value = false

                if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
                    recreateRecognizer()
                }
            }

            override fun onResults(results: Bundle?) {
                Log.d(TAG, "onResults")
                _isListening.value = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    _recognizedText.value = text
                    Log.d(TAG, "Recognized text: $text")
                }
            }

            override fun onPartialResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    _partialText.value = text
                    Log.d(TAG, "Partial text: $text")
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.d(TAG, "onEvent: $eventType")
            }
        }
    }

    fun startListening(language: String = "vi-VN") {
        if (!isRecognitionAvailable()) {
            _errorMessage.value = "Speech recognition is not available on this device."
            return
        }

        if (speechRecognizer == null) {
            initSpeechRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayOf("vi-VN", "en-US"))
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            _recognizedText.value = ""
            _partialText.value = ""
            _errorMessage.value = null
            speechRecognizer?.startListening(intent)
            _isListening.value = true
            Log.d(TAG, "Started listening with language: $language")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
            _errorMessage.value = "Failed to start listening: ${e.localizedMessage}"
            _isListening.value = false
            recreateRecognizer()
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping listening", e)
        } finally {
            _isListening.value = false
        }
    }

    fun cancel() {
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling listening", e)
        } finally {
            _isListening.value = false
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying speech recognizer", e)
        } finally {
            speechRecognizer = null
            _isListening.value = false
        }
    }

    private fun recreateRecognizer() {
        destroy()
        initSpeechRecognizer()
    }

    private fun getErrorText(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error ($errorCode)"
        }
    }

    companion object {
        private const val TAG = "SpeechRecognizerManager"
    }
}
