package com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.streaming

import com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.audio.AudioRecorderManager
import com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.data.ApiProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VoiceStreamingManager(
    private val audioRecorderManager: AudioRecorderManager = AudioRecorderManager(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {

    private val _state = MutableStateFlow(StreamingState())
    val state: StateFlow<StreamingState> = _state.asStateFlow()

    private var activeClient: VoiceStreamingClient? = null
    private var recordingJob: Job? = null

    fun startStreaming(provider: ApiProvider, apiKey: String) {
        if (apiKey.isBlank()) {
            _state.update {
                it.copy(
                    status = StreamingStatus.ERROR,
                    errorMessage = "API Key không được để trống",
                    activeProvider = provider
                )
            }
            return
        }

        // Stop any existing session
        stopStreaming()

        _state.update {
            StreamingState(
                status = StreamingStatus.CONNECTING,
                activeProvider = provider,
                errorMessage = null
            )
        }

        val client: VoiceStreamingClient = when (provider) {
            ApiProvider.ASSEMBLY_AI -> AssemblyAiStreamingClient()
            ApiProvider.REV_AI -> RevAiStreamingClient()
        }
        activeClient = client

        val listener = object : StreamingClientListener {
            override fun onConnected() {
                _state.update { it.copy(status = StreamingStatus.RECORDING, errorMessage = null) }
                startAudioCollection()
            }

            override fun onPartialTranscript(text: String) {
                _state.update { it.copy(partialTranscript = text) }
            }

            override fun onFinalTranscript(text: String) {
                _state.update { current ->
                    val updatedFinals = if (text.isNotBlank()) {
                        current.finalTranscripts + text
                    } else {
                        current.finalTranscripts
                    }
                    current.copy(
                        finalTranscripts = updatedFinals,
                        partialTranscript = ""
                    )
                }
            }

            override fun onError(error: String) {
                stopAudioCollection()
                _state.update {
                    it.copy(
                        status = StreamingStatus.ERROR,
                        errorMessage = error
                    )
                }
            }

            override fun onClosed() {
                stopAudioCollection()
                _state.update {
                    if (it.status != StreamingStatus.ERROR) {
                        it.copy(status = StreamingStatus.IDLE)
                    } else {
                        it
                    }
                }
            }
        }

        client.connect(apiKey, listener)
    }

    private fun startAudioCollection() {
        recordingJob?.cancel()
        recordingJob = scope.launch(Dispatchers.IO) {
            audioRecorderManager.startRecording(AudioRecorderManager.DEFAULT_CHUNK_SIZE)
                .catch { e ->
                    _state.update {
                        it.copy(
                            status = StreamingStatus.ERROR,
                            errorMessage = "Lỗi thu âm: ${e.localizedMessage}"
                        )
                    }
                }
                .collect { audioChunk ->
                    activeClient?.sendAudioChunk(audioChunk)
                }
        }
    }

    private fun stopAudioCollection() {
        recordingJob?.cancel()
        recordingJob = null
        audioRecorderManager.stopRecording()
    }

    fun stopStreaming() {
        _state.update {
            if (it.status == StreamingStatus.RECORDING || it.status == StreamingStatus.CONNECTING) {
                it.copy(status = StreamingStatus.STOPPING)
            } else {
                it
            }
        }

        stopAudioCollection()
        activeClient?.stop()
        activeClient = null

        _state.update {
            if (it.status != StreamingStatus.ERROR) {
                it.copy(status = StreamingStatus.IDLE)
            } else {
                it
            }
        }
    }

    fun clearTranscripts() {
        _state.update {
            it.copy(
                partialTranscript = "",
                finalTranscripts = emptyList()
            )
        }
    }
}
