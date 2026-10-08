package com.example.alldemo.features.androidnativespeechrecognizer

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val speechRecognizerManager = SpeechRecognizerManager(application.applicationContext)

    val isListening: StateFlow<Boolean> = speechRecognizerManager.isListening
    val recognizedText: StateFlow<String> = speechRecognizerManager.recognizedText
    val partialText: StateFlow<String> = speechRecognizerManager.partialText
    val errorMessage: StateFlow<String?> = speechRecognizerManager.errorMessage
    val soundLevel: StateFlow<Float> = speechRecognizerManager.rmsChanged

    private val _passphrase = MutableStateFlow("mở khóa")
    val passphrase: StateFlow<String> = _passphrase.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    init {
        viewModelScope.launch {
            speechRecognizerManager.recognizedText.collect { text ->
                checkPassphraseMatch(text)
            }
        }
        viewModelScope.launch {
            speechRecognizerManager.partialText.collect { text ->
                checkPassphraseMatch(text)
            }
        }
    }

    fun startListening(context: Context, language: String = "vi-VN") {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(TAG, "RECORD_AUDIO permission not granted")
            return
        }
        speechRecognizerManager.startListening(language)
    }

    fun startListening(language: String = "vi-VN") {
        val context = getApplication<Application>().applicationContext
        startListening(context, language)
    }

    fun stopListening() {
        speechRecognizerManager.stopListening()
    }

    fun resetState() {
        _isUnlocked.value = false
        speechRecognizerManager.cancel()
    }

    fun setPassphrase(newPassphrase: String) {
        _passphrase.value = newPassphrase
        checkPassphraseMatch(speechRecognizerManager.recognizedText.value)
        checkPassphraseMatch(speechRecognizerManager.partialText.value)
    }

    fun matchesPassphrase(recognized: String, targetPassphrase: String): Boolean {
        return PassphraseMatcher.matches(recognized, targetPassphrase)
    }

    private fun checkPassphraseMatch(text: String) {
        if (_isUnlocked.value) return
        if (matchesPassphrase(text, _passphrase.value)) {
            _isUnlocked.value = true
            Log.d(TAG, "Passphrase matched! Recognized: '$text', Passphrase: '${_passphrase.value}'")
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerManager.destroy()
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
