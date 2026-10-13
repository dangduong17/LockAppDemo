package com.example.alldemo.features.geminimultimodallivevoiceapi.data.repository

import android.util.Log
import com.example.alldemo.features.geminimultimodallivevoiceapi.audio.AudioRecorder
import com.example.alldemo.features.geminimultimodallivevoiceapi.data.model.MessageSender
import com.example.alldemo.features.geminimultimodallivevoiceapi.data.model.RealtimeServerEvent
import com.example.alldemo.features.geminimultimodallivevoiceapi.data.model.SessionConfig
import com.example.alldemo.features.geminimultimodallivevoiceapi.data.model.TranscribedMessage
import com.example.alldemo.features.geminimultimodallivevoiceapi.network.ConnectionState
import com.example.alldemo.features.geminimultimodallivevoiceapi.network.GeminiLiveClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RealtimeRepository(
    val geminiClient: GeminiLiveClient = GeminiLiveClient(),
    val audioRecorder: AudioRecorder = AudioRecorder()
) {
    companion object {
        private const val TAG = "RealtimeRepository"
    }

    private var repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var streamingJob: Job? = null
    private var currentAssistantItemId: String? = null

    val connectionState: StateFlow<ConnectionState> = geminiClient.connectionState
    val isRecording: StateFlow<Boolean> = audioRecorder.isRecording
    val audioLevel: StateFlow<Float> = audioRecorder.audioLevel

    private val _messages = MutableStateFlow<List<TranscribedMessage>>(emptyList())
    val messages: StateFlow<List<TranscribedMessage>> = _messages.asStateFlow()

    private val _latestUserTranscript = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val latestUserTranscript: SharedFlow<String> = _latestUserTranscript.asSharedFlow()

    private val _latestAssistantTranscript = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val latestAssistantTranscript: SharedFlow<String> = _latestAssistantTranscript.asSharedFlow()

    private val _latestAssistantAudioDelta = MutableSharedFlow<String>(extraBufferCapacity = 256)
    val latestAssistantAudioDelta: SharedFlow<String> = _latestAssistantAudioDelta.asSharedFlow()

    private val _errorState = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val errorState: SharedFlow<String> = _errorState.asSharedFlow()

    init {
        observeServerEvents()
    }

    fun connect(
        apiKey: String,
        model: String = GeminiLiveClient.DEFAULT_MODEL,
        sessionConfig: SessionConfig? = null
    ) {
        if (repositoryScope.coroutineContext[Job]?.isActive != true) {
            repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        }
        observeServerEvents()
        geminiClient.connect(apiKey, model)
    }

    fun disconnect() {
        stopStreamingAudio(autoCommitAndRespond = false)
        geminiClient.disconnect()
        repositoryScope.cancel()
    }

    fun startStreamingAudio(scope: CoroutineScope): Boolean {
        if (connectionState.value !is ConnectionState.Connected) {
            Log.w(TAG, "Cannot start streaming: Gemini client is not connected.")
            repositoryScope.launch {
                _errorState.emit("Cannot start recording: Client is not connected.")
            }
            return false
        }

        streamingJob?.cancel()
        streamingJob = scope.launch(Dispatchers.IO) {
            audioRecorder.audioBase64Flow.collect { base64Chunk ->
                geminiClient.sendAudioChunk(base64Chunk)
            }
        }

        val started = audioRecorder.startRecording(scope)
        if (!started) {
            streamingJob?.cancel()
            streamingJob = null
        }
        return started
    }

    fun stopStreamingAudio(autoCommitAndRespond: Boolean = true) {
        if (audioRecorder.isRecording.value) {
            audioRecorder.stopRecording()
            streamingJob?.cancel()
            streamingJob = null
        }
    }

    fun sendTextMessage(text: String, role: String = "user") {
        if (text.isBlank()) return
        if (connectionState.value !is ConnectionState.Connected) {
            repositoryScope.launch {
                _errorState.emit("Chưa kết nối tới Gemini Live API")
            }
            return
        }

        val userMsg = TranscribedMessage(
            sender = MessageSender.USER,
            text = text,
            isFinal = true
        )
        _messages.value = _messages.value + userMsg

        geminiClient.sendTextMessage(text)
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }

    private fun observeServerEvents() {
        repositoryScope.launch {
            geminiClient.serverEvents.collect { event ->
                handleServerEvent(event)
            }
        }
    }

    private fun handleServerEvent(event: RealtimeServerEvent) {
        when (event) {
            is RealtimeServerEvent.InputAudioTranscriptionCompleted -> {
                val transcript = event.transcript
                if (transcript.isNotBlank()) {
                    val userMsg = TranscribedMessage(
                        itemId = event.itemId,
                        sender = MessageSender.USER,
                        text = transcript,
                        isFinal = true
                    )
                    _messages.value = _messages.value + userMsg
                    repositoryScope.launch {
                        _latestUserTranscript.emit(transcript)
                    }
                }
            }

            is RealtimeServerEvent.ResponseAudioTranscriptDelta -> {
                val itemId = event.itemId ?: "gemini_assistant_response"
                currentAssistantItemId = itemId
                val delta = event.delta

                val currentList = _messages.value.toMutableList()
                val existingIndex = currentList.indexOfLast { it.itemId == itemId || (it.sender == MessageSender.ASSISTANT && !it.isFinal) }

                if (existingIndex >= 0) {
                    val existing = currentList[existingIndex]
                    currentList[existingIndex] = existing.copy(
                        itemId = itemId,
                        text = existing.text + delta,
                        isFinal = false
                    )
                } else {
                    currentList.add(
                        TranscribedMessage(
                            itemId = itemId,
                            sender = MessageSender.ASSISTANT,
                            text = delta,
                            isFinal = false
                        )
                    )
                }
                _messages.value = currentList
            }

            is RealtimeServerEvent.ResponseTextDelta -> {
                val itemId = event.itemId ?: "gemini_assistant_text"
                val delta = event.delta

                val currentList = _messages.value.toMutableList()
                val existingIndex = currentList.indexOfLast { it.itemId == itemId || (it.sender == MessageSender.ASSISTANT && !it.isFinal) }

                if (existingIndex >= 0) {
                    val existing = currentList[existingIndex]
                    currentList[existingIndex] = existing.copy(
                        itemId = itemId,
                        text = existing.text + delta,
                        isFinal = false
                    )
                } else {
                    currentList.add(
                        TranscribedMessage(
                            itemId = itemId,
                            sender = MessageSender.ASSISTANT,
                            text = delta,
                            isFinal = false
                        )
                    )
                }
                _messages.value = currentList
            }

            is RealtimeServerEvent.ResponseAudioTranscriptDone -> {
                val itemId = event.itemId ?: currentAssistantItemId
                val currentList = _messages.value.toMutableList()
                val existingIndex = currentList.indexOfLast { it.itemId == itemId }

                if (existingIndex >= 0) {
                    val existing = currentList[existingIndex]
                    currentList[existingIndex] = existing.copy(isFinal = true)
                    _messages.value = currentList
                    repositoryScope.launch {
                        _latestAssistantTranscript.emit(existing.text)
                    }
                }
            }

            is RealtimeServerEvent.ResponseTextDone -> {
                val itemId = event.itemId ?: currentAssistantItemId
                val currentList = _messages.value.toMutableList()
                val existingIndex = currentList.indexOfLast { it.itemId == itemId }

                if (existingIndex >= 0) {
                    val existing = currentList[existingIndex]
                    currentList[existingIndex] = existing.copy(isFinal = true)
                    _messages.value = currentList
                    repositoryScope.launch {
                        _latestAssistantTranscript.emit(existing.text)
                    }
                }
            }

            is RealtimeServerEvent.ResponseAudioDelta -> {
                val pcmBase64Delta = event.delta
                if (pcmBase64Delta.isNotBlank()) {
                    repositoryScope.launch {
                        _latestAssistantAudioDelta.emit(pcmBase64Delta)
                    }
                }
            }

            is RealtimeServerEvent.Error -> {
                val errorMsg = event.error.message ?: "Unknown server error"
                Log.e(TAG, "Server Error: $errorMsg")
                repositoryScope.launch {
                    _errorState.emit(errorMsg)
                }
            }

            else -> {}
        }
    }
}
