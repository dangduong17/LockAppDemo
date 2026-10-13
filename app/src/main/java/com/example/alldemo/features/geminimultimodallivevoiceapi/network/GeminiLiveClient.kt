package com.example.alldemo.features.geminimultimodallivevoiceapi.network

import android.util.Log
import com.example.alldemo.features.geminimultimodallivevoiceapi.data.model.RealtimeErrorDetails
import com.example.alldemo.features.geminimultimodallivevoiceapi.data.model.RealtimeServerEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

@Serializable
data class GeminiSetupFrame(
    val setup: GeminiSetupData
)

@Serializable
data class GeminiSetupData(
    val model: String = "models/gemini-3.1-flash-live-preview",
    val generationConfig: GeminiGenerationConfig = GeminiGenerationConfig()
)

@Serializable
data class GeminiGenerationConfig(
    val responseModalities: List<String> = listOf("AUDIO"),
    val speechConfig: GeminiSpeechConfig = GeminiSpeechConfig()
)

@Serializable
data class GeminiSpeechConfig(
    val voiceConfig: GeminiVoiceConfig = GeminiVoiceConfig()
)

@Serializable
data class GeminiVoiceConfig(
    val prebuiltVoiceConfig: GeminiPrebuiltVoiceConfig = GeminiPrebuiltVoiceConfig()
)

@Serializable
data class GeminiPrebuiltVoiceConfig(
    val voiceName: String = "Puck"
)

@Serializable
data class GeminiRealtimeInputFrame(
    val realtimeInput: GeminiRealtimeInputData
)

@Serializable
data class GeminiRealtimeInputData(
    val mediaChunks: List<GeminiMediaChunk>
)

@Serializable
data class GeminiMediaChunk(
    val mimeType: String = "audio/pcm;rate=24000",
    val data: String
)

@Serializable
data class GeminiClientContentFrame(
    val clientContent: GeminiClientContentData
)

@Serializable
data class GeminiClientContentData(
    val turns: List<GeminiTurn>,
    val turnComplete: Boolean = true
)

@Serializable
data class GeminiTurn(
    val role: String = "user",
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String
)

class GeminiLiveClient(
    private val client: OkHttpClient = defaultOkHttpClient()
) {
    companion object {
        const val DEFAULT_MODEL = "models/gemini-3.1-flash-live-preview"
        const val DEFAULT_BASE_URL = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"
        private const val TAG = "GeminiLiveClient"

        fun defaultOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .pingInterval(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }

    private var clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var webSocket: WebSocket? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _serverEvents = MutableSharedFlow<RealtimeServerEvent>(extraBufferCapacity = 128)
    val serverEvents: SharedFlow<RealtimeServerEvent> = _serverEvents.asSharedFlow()

    fun connect(
        apiKey: String,
        model: String = DEFAULT_MODEL,
        baseUrl: String = DEFAULT_BASE_URL
    ) {
        val cleanApiKey = apiKey.trim()
        if (cleanApiKey.isBlank()) {
            _connectionState.value = ConnectionState.Error("API Key Google AI Studio không được để trống")
            return
        }

        if (_connectionState.value is ConnectionState.Connecting || _connectionState.value is ConnectionState.Connected) {
            Log.w(TAG, "Already connecting or connected.")
            return
        }

        disconnect()

        clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        _connectionState.value = ConnectionState.Connecting

        val formattedModel = if (model.startsWith("models/")) model else "models/$model"
        
        val wsUrl = "$baseUrl?key=$cleanApiKey"
        val request = Request.Builder()
            .url(wsUrl)
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Gemini WebSocket connected successfully.")
                _connectionState.value = ConnectionState.Connected

                try {
                    val setupFrame = GeminiSetupFrame(
                        setup = GeminiSetupData(
                            model = formattedModel
                        )
                    )
                    val jsonSetup = json.encodeToString(setupFrame)
                    Log.d(TAG, "Sending setup frame: $jsonSetup")
                    webSocket.send(jsonSetup)
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending setup frame", e)
                    _connectionState.value = ConnectionState.Error("Lỗi khởi tạo Gemini setup: ${e.localizedMessage}")
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received message from Gemini: $text")
                parseGeminiMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code / $reason")
                webSocket.close(1000, null)
                val msg = if (reason.isNotBlank()) "Gemini ngắt kết nối (code $code): $reason" else null
                if (msg != null) {
                    _connectionState.value = ConnectionState.Error(msg)
                    clientScope.launch {
                        _serverEvents.emit(
                            RealtimeServerEvent.Error(
                                error = RealtimeErrorDetails(message = msg)
                            )
                        )
                    }
                } else {
                    _connectionState.value = ConnectionState.Disconnected
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                val msg = if (reason.isNotBlank()) "Gemini ngắt kết nối (code $code): $reason" else null
                if (msg != null && _connectionState.value !is ConnectionState.Error) {
                    _connectionState.value = ConnectionState.Error(msg)
                } else if (_connectionState.value !is ConnectionState.Error) {
                    _connectionState.value = ConnectionState.Disconnected
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                var errorMessage = response?.message ?: t.localizedMessage ?: "Gemini WebSocket connection failed"
                response?.let { resp ->
                    runCatching {
                        val bodyText = resp.body?.string()
                        errorMessage = if (!bodyText.isNullOrBlank()) {
                            "HTTP ${resp.code}: $bodyText"
                        } else {
                            "HTTP ${resp.code}: ${resp.message}"
                        }
                    }
                }
                Log.e(TAG, "Gemini WebSocket failure: $errorMessage", t)
                _connectionState.value = ConnectionState.Error(errorMessage, t)
                clientScope.launch {
                    _serverEvents.emit(
                        RealtimeServerEvent.Error(
                            error = RealtimeErrorDetails(message = errorMessage)
                        )
                    )
                }
            }
        }

        webSocket = client.newWebSocket(request, listener)
    }

    fun sendAudioChunk(base64PcmChunk: String): Boolean {
        val ws = webSocket ?: return false
        val frame = GeminiRealtimeInputFrame(
            realtimeInput = GeminiRealtimeInputData(
                mediaChunks = listOf(
                    GeminiMediaChunk(
                        mimeType = "audio/pcm;rate=24000",
                        data = base64PcmChunk
                    )
                )
            )
        )
        val jsonText = json.encodeToString(frame)
        return ws.send(jsonText)
    }

    fun sendTextMessage(text: String): Boolean {
        val ws = webSocket ?: return false
        val frame = GeminiClientContentFrame(
            clientContent = GeminiClientContentData(
                turns = listOf(
                    GeminiTurn(
                        role = "user",
                        parts = listOf(GeminiPart(text = text))
                    )
                ),
                turnComplete = true
            )
        )
        val jsonText = json.encodeToString(frame)
        return ws.send(jsonText)
    }

    private fun parseGeminiMessage(jsonText: String) {
        clientScope.launch {
            runCatching {
                val jsonObject = json.parseToJsonElement(jsonText).jsonObject

                val errorObj = jsonObject["error"]?.jsonObject
                if (errorObj != null) {
                    val errMsg = errorObj["message"]?.jsonPrimitive?.contentOrNull ?: "Unknown Gemini Error"
                    Log.e(TAG, "Gemini Server Error: $errMsg")
                    _connectionState.value = ConnectionState.Error("Gemini Error: $errMsg")
                    _serverEvents.emit(
                        RealtimeServerEvent.Error(
                            error = RealtimeErrorDetails(message = errMsg)
                        )
                    )
                    return@launch
                }

                val serverContent = jsonObject["serverContent"]?.jsonObject
                if (serverContent != null) {
                    val modelTurn = serverContent["modelTurn"]?.jsonObject
                    if (modelTurn != null) {
                        val parts = modelTurn["parts"]?.let { json.decodeFromJsonElement<List<JsonObject>>(it) }
                        parts?.forEach { partObj ->
                            val text = partObj["text"]?.jsonPrimitive?.contentOrNull
                            if (!text.isNullOrBlank()) {
                                _serverEvents.emit(
                                    RealtimeServerEvent.ResponseTextDelta(
                                        itemId = "gemini_response",
                                        delta = text
                                    )
                                )
                                _serverEvents.emit(
                                    RealtimeServerEvent.InputAudioTranscriptionCompleted(
                                        itemId = "gemini_transcription",
                                        transcript = text
                                    )
                                )
                            }
                            val inlineData = partObj["inlineData"]?.jsonObject
                            if (inlineData != null) {
                                val audioData = inlineData["data"]?.jsonPrimitive?.contentOrNull
                                if (!audioData.isNullOrBlank()) {
                                    _serverEvents.emit(
                                        RealtimeServerEvent.ResponseAudioDelta(
                                            itemId = "gemini_audio",
                                            delta = audioData
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }.onFailure { err ->
                Log.e(TAG, "Error parsing Gemini message", err)
            }
        }
    }

    fun disconnect() {
        runCatching {
            webSocket?.close(1000, "User disconnected")
        }
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
        clientScope.cancel()
    }
}
