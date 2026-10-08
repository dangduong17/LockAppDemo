package com.example.alldemo.features.demovoicespeechrecognizer.engine

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class AndroidSpeechRecognizerEngine(
    private val context: Context
) : SpeechRecognitionEngine {

    override val engineType: EngineType = EngineType.ANDROID_NATIVE

    private val _state = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    override val state: StateFlow<RecognitionState> = _state.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var speechStartTime: Long = 0L
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    override fun startListening(language: String) {
        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _state.value = RecognitionState.Error("Speech recognition is not available on this device")
                return@post
            }

            try {
                destroyRecognizer()
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer = recognizer

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        speechStartTime = System.currentTimeMillis()
                        _state.value = RecognitionState.Listening
                    }

                    override fun onBeginningOfSpeech() {
                        speechStartTime = System.currentTimeMillis()
                        _state.value = RecognitionState.Processing
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _state.value = RecognitionState.Processing
                    }

                    override fun onError(error: Int) {
                        val latencyMs = calculateLatency()
                        val errorMessage = getErrorMessage(error)
                        _state.value = RecognitionState.Error("Android SpeechRecognizer error ($error): $errorMessage [Latency: ${latencyMs}ms]")
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull().orEmpty()
                        val latencyMs = calculateLatency()
                        _state.value = RecognitionState.FinalResult(text, latencyMs)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull().orEmpty()
                        if (text.isNotBlank()) {
                            val latencyMs = calculateLatency()
                            _state.value = RecognitionState.PartialResult(text, latencyMs)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val resolvedLanguage = if (language.equals("auto", ignoreCase = true) || language.isBlank()) {
                    Locale.getDefault().toLanguageTag().ifBlank { Locale.getDefault().toString() }
                } else {
                    language
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, resolvedLanguage)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, resolvedLanguage)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                _state.value = RecognitionState.Initializing
                recognizer.startListening(intent)
            } catch (e: Exception) {
                _state.value = RecognitionState.Error("Failed to start SpeechRecognizer: ${e.message}")
            }
        }
    }

    override fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                _state.value = RecognitionState.Idle
            } catch (e: Exception) {
                _state.value = RecognitionState.Error("Failed to stop SpeechRecognizer: ${e.message}")
            }
        }
    }

    override fun destroy() {
        mainHandler.post {
            destroyRecognizer()
            _state.value = RecognitionState.Idle
        }
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}
    }

    private fun calculateLatency(): Long {
        if (speechStartTime == 0L) return 0L
        return (System.currentTimeMillis() - speechStartTime).coerceAtLeast(0)
    }

    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
            else -> "Unknown error"
        }
    }
}
