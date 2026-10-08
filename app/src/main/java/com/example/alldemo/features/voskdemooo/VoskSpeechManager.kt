package com.example.alldemo.features.voskdemooo

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException

class VoskSpeechManager(private val context: Context) : RecognitionListener {

    private var model: Model? = null
    private var speechService: SpeechService? = null

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentModelName: String? = null
    private var initJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun initModel(modelName: String = "model_vi") {
        if (currentModelName == modelName && _isReady.value && model != null) return

        // Properly clean up previous state and model when switching languages
        stopListening()
        speechService?.shutdown()
        speechService = null
        model?.close()
        model = null

        _isReady.value = false
        _isListening.value = false
        _recognizedText.value = ""
        currentModelName = modelName

        initJob?.cancel()
        initJob = coroutineScope.launch {
            _errorMessage.value = null
            try {
                val path = withContext(Dispatchers.IO) {
                    StorageService.sync(context, modelName, modelName)
                }
                
                // Initialize model on IO thread as it might be blocking
                val loadedModel = withContext(Dispatchers.IO) {
                    Model(path)
                }

                model = loadedModel
                _isReady.value = true
                Log.d("VoskSpeechManager", "Model '$modelName' unpacked successfully at path: $path")
            } catch (e: Exception) {
                val errorMsg = "Failed to unpack model '$modelName': ${e.message}"
                _errorMessage.value = errorMsg
                Log.e("VoskSpeechManager", errorMsg, e)
            }
        }
    }

    fun startListening() {
        if (model == null) return
        if (speechService == null) {
            try {
                val recognizer = Recognizer(model, 16000.0f)
                speechService = SpeechService(recognizer, 16000.0f)
                speechService?.startListening(this)
                _isListening.value = true
            } catch (e: IOException) {
                Log.e("VoskSpeechManager", "Failed to start listening", e)
                _errorMessage.value = "Failed to start listening: ${e.message}"
            }
        } else {
            speechService?.startListening(this)
            _isListening.value = true
        }
    }

    fun stopListening() {
        speechService?.stop()
        _isListening.value = false
    }

    fun destroy() {
        coroutineScope.cancel()
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
        model?.close()
        model = null
    }

    override fun onPartialResult(hypothesis: String?) {
        hypothesis?.let { _recognizedText.value = it }
    }

    override fun onResult(hypothesis: String?) {
        hypothesis?.let { _recognizedText.value = it }
    }

    override fun onFinalResult(hypothesis: String?) {
        hypothesis?.let { _recognizedText.value = it }
    }

    override fun onError(exception: Exception?) {
        Log.e("VoskSpeechManager", "Error in recognition", exception)
        _errorMessage.value = "Error: ${exception?.message}"
        _isListening.value = false
    }

    override fun onTimeout() {
        stopListening()
        _isListening.value = false
    }
}
