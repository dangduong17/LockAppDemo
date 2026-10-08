package com.example.alldemo.features.cloudwhisperdemo

import android.app.Application
import android.content.Context
import android.media.MediaRecorder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

data class CloudWhisperUiState(
    val keyword: String = "mở khóa",
    val spokenText: String = "",
    val isRecording: Boolean = false,
    val apiKey: String = "",
    val audioFilePath: String? = null,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
) {
    val matches: Boolean
        get() {
            if (spokenText.isBlank() || keyword.isBlank()) return false
            val normalizedKeyword = keyword.trim().lowercase()
            val normalizedSpoken = spokenText.trim().lowercase()
            return normalizedSpoken.contains(normalizedKeyword) || normalizedSpoken == normalizedKeyword
        }
}

class CloudWhisperViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(CloudWhisperUiState())
    val uiState: StateFlow<CloudWhisperUiState> = _uiState.asStateFlow()

    private val sharedPreferences = application.getSharedPreferences("cloud_whisper_prefs", Context.MODE_PRIVATE)
    private var mediaRecorder: MediaRecorder? = null

    init {
        val savedApiKey = sharedPreferences.getString("api_key", "") ?: ""
        _uiState.update { it.copy(apiKey = savedApiKey) }
    }

    fun updateKeyword(newKeyword: String) {
        _uiState.update { state ->
            val updatedState = state.copy(keyword = newKeyword)
            updatedState.copy(isSuccess = updatedState.matches)
        }
    }

    fun updateApiKey(newApiKey: String) {
        sharedPreferences.edit().putString("api_key", newApiKey).apply()
        _uiState.update { state ->
            val updatedState = state.copy(apiKey = newApiKey)
            updatedState.copy(isSuccess = updatedState.matches)
        }
    }

    fun setSpokenText(text: String) {
        _uiState.update { state ->
            val updatedState = state.copy(spokenText = text)
            updatedState.copy(isSuccess = updatedState.matches)
        }
    }

    fun resetState() {
        _uiState.update { state ->
            state.copy(spokenText = "", isSuccess = false, errorMessage = null, isRecording = false, audioFilePath = null)
        }
    }

    fun setErrorMessage(message: String?) {
        _uiState.update { it.copy(errorMessage = message, isRecording = false) }
    }

    fun startRecording(context: Context) {
        try {
            val outputDir = context.cacheDir
            val audioFile = File.createTempFile("whisper_audio_", ".m4a", outputDir)
            val filePath = audioFile.absolutePath

            mediaRecorder?.release()
            mediaRecorder = (if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(filePath)
                prepare()
                start()
            }

            _uiState.update {
                it.copy(
                    isRecording = true,
                    audioFilePath = filePath,
                    errorMessage = null
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isRecording = false,
                    errorMessage = e.localizedMessage ?: "Error starting audio recording"
                )
            }
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // handle or ignore release exceptions
        } finally {
            mediaRecorder = null
        }

        _uiState.update { it.copy(isRecording = false) }

        val currentApiKey = _uiState.value.apiKey.trim()
        if (currentApiKey.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Please enter your Groq API Key (gsk-...)"
                )
            }
            return
        }

        val filePath = _uiState.value.audioFilePath
        if (filePath.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Audio file path is missing"
                )
            }
            return
        }

        val audioFile = File(filePath)
        if (!audioFile.exists()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Recorded audio file does not exist"
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("model", "whisper-large-v3-turbo")
                    .addFormDataPart("language", "vi")
                    .addFormDataPart(
                        "file",
                        audioFile.name,
                        audioFile.asRequestBody("audio/m4a".toMediaTypeOrNull() ?: "audio/mp4".toMediaType())
                    )
                    .build()

                val request = Request.Builder()
                    .url("https://api.groq.com/openai/v1/audio/transcriptions")
                    .addHeader("Authorization", "Bearer $currentApiKey")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBodyString = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        val errorMatch = Regex("\"message\"\\s*:\\s*\"([^\"]*)\"").find(responseBodyString)
                        val errorMsg = errorMatch?.groupValues?.get(1) ?: "API Error: ${response.code}"
                        _uiState.update { it.copy(errorMessage = errorMsg) }
                    } else {
                        val textMatch = Regex("\"text\"\\s*:\\s*\"([^\"]*)\"").find(responseBodyString)
                        val text = textMatch?.groupValues?.get(1)?.replace("\\\"", "\"") ?: responseBodyString.trim()

                        _uiState.update { state ->
                            val updated = state.copy(spokenText = text, errorMessage = null)
                            updated.copy(isSuccess = updated.matches)
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage ?: "Transcription network error") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
    }
}
