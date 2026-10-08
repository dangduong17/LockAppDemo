package com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.streaming

import android.util.Base64
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class AssemblyAiStreamingClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()
) : VoiceStreamingClient {

    private companion object {
        private const val TAG = "AssemblyAiStreaming"
    }

    private var webSocket: WebSocket? = null
    private var tokenCall: Call? = null
    private var listener: StreamingClientListener? = null
    @Volatile private var isConnected = false

    override fun connect(apiKey: String, listener: StreamingClientListener) {
        this.listener = listener
        val cleanApiKey = apiKey.trim()

        if (cleanApiKey.isBlank()) {
            Log.e(TAG, "API Key is blank")
            this.listener?.onError("AssemblyAI API Key không được để trống")
            return
        }

        Log.d(TAG, "Initiating connection to AssemblyAI with API Key")
        requestTemporaryToken(cleanApiKey)
    }

    private fun requestTemporaryToken(apiKey: String) {
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = JSONObject().apply {
            put("expires_in", 480)
        }.toString().toRequestBody(jsonMediaType)

        val tokenRequest = Request.Builder()
            .url("https://api.assemblyai.com/v2/realtime/token")
            .post(requestBody)
            .addHeader("Authorization", apiKey)
            .addHeader("Content-Type", "application/json")
            .build()

        tokenCall = client.newCall(tokenRequest)
        tokenCall?.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.w(TAG, "Failed to fetch temporary token over HTTP: ${e.message}. Attempting direct WebSocket fallback...")
                connectDirectWebSocket(apiKey)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseCode = response.code
                val responseBody = try { response.body?.string() } catch (_: Exception) { null }

                Log.d(TAG, "Temp token response code: $responseCode, body: $responseBody")

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    try {
                        val json = JSONObject(responseBody)
                        val token = json.optString("token")
                        if (token.isNotBlank()) {
                            Log.i(TAG, "Obtained temporary token, connecting WebSocket with token...")
                            connectWebSocketWithToken(token)
                            return
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse token response JSON: ${e.message}")
                    }
                }

                if (responseCode == 404 || responseCode == 401 || responseCode == 403) {
                    val formattedError = formatAssemblyAiHttpError(responseCode, responseBody)
                    Log.e(TAG, "Token request returned HTTP $responseCode: $formattedError")
                    connectDirectWebSocket(apiKey, fallbackError = formattedError)
                } else {
                    Log.w(TAG, "Token request returned HTTP $responseCode. Trying direct WebSocket connection...")
                    connectDirectWebSocket(apiKey)
                }
            }
        })
    }

    private fun connectWebSocketWithToken(tempToken: String) {
        val wsUrl = "wss://api.assemblyai.com/v2/realtime/ws?sample_rate=16000&token=${tempToken}"
        val request = Request.Builder()
            .url(wsUrl)
            .build()

        startWebSocketConnection(request, initialError = null)
    }

    private fun connectDirectWebSocket(apiKey: String, fallbackError: String? = null) {
        val authHeader = if (apiKey.startsWith("Bearer ", ignoreCase = true)) apiKey else apiKey
        val wsUrl = "wss://api.assemblyai.com/v2/realtime/ws?sample_rate=16000"

        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("Authorization", authHeader)
            .build()

        startWebSocketConnection(request, initialError = fallbackError)
    }

    private fun startWebSocketConnection(request: Request, initialError: String? = null) {
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket connected successfully. Awaiting SessionBegins frame...")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "WebSocket message received: $text")
                handleJsonMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                val errorMsg = parseWebSocketFailureMessage(t, response, initialError)
                Log.e(TAG, "WebSocket failure: $errorMsg", t)
                this@AssemblyAiStreamingClient.listener?.onError(errorMsg)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket closed code=$code, reason=$reason")
                isConnected = false
                this@AssemblyAiStreamingClient.listener?.onClosed()
            }
        })
    }

    private fun formatAssemblyAiHttpError(code: Int, body: String?): String {
        return when (code) {
            404, 401 -> "Lỗi AssemblyAI (404/401): API Key hợp lệ nhưng tính năng Real-Time cần tài khoản đã liên kết thẻ thanh toán trên AssemblyAI Dashboard. Hoặc hãy chuyển sang dùng Rev AI API Key."
            403 -> "Lỗi AssemblyAI (403): Dịch vụ Real-Time bị giới hạn. Cần tài khoản đã liên kết thẻ thanh toán trên AssemblyAI Dashboard. Hoặc hãy chuyển sang dùng Rev AI API Key."
            else -> "Lỗi AssemblyAI (HTTP $code): ${body ?: "Không thể kết nối dịch vụ Real-Time."}"
        }
    }

    internal fun parseWebSocketFailureMessage(
        t: Throwable,
        response: Response?,
        initialError: String? = null
    ): String {
        if (initialError != null) {
            return initialError
        }

        if (response != null) {
            val code = response.code
            val bodyString = try { response.body?.string() } catch (_: Exception) { null }
            if (code == 404 || code == 401 || code == 403) {
                return formatAssemblyAiHttpError(code, bodyString)
            }
            return "Lỗi kết nối AssemblyAI (HTTP $code ${response.message}): ${bodyString ?: t.localizedMessage}"
        }

        val msg = t.localizedMessage ?: t.message ?: ""
        return when {
            msg.contains("404") || msg.contains("401") -> "Lỗi AssemblyAI (404/401): API Key hợp lệ nhưng tính năng Real-Time cần tài khoản đã liên kết thẻ thanh toán trên AssemblyAI Dashboard. Hoặc hãy chuyển sang dùng Rev AI API Key."
            msg.contains("403") -> "Lỗi AssemblyAI (403): Dịch vụ Real-Time bị giới hạn. Cần tài khoản đã liên kết thẻ thanh toán trên AssemblyAI Dashboard. Hoặc hãy chuyển sang dùng Rev AI API Key."
            else -> "Lỗi kết nối WebSocket AssemblyAI: $msg"
        }
    }

    private fun handleJsonMessage(text: String) {
        try {
            val json = JSONObject(text)
            val messageType = json.optString("message_type")

            when (messageType) {
                "SessionBegins" -> {
                    Log.i(TAG, "AssemblyAI SessionBegins received")
                    isConnected = true
                    listener?.onConnected()
                }
                "PartialTranscript" -> {
                    val transcript = json.optString("text", "")
                    listener?.onPartialTranscript(transcript)
                }
                "FinalTranscript" -> {
                    val transcript = json.optString("text", "")
                    listener?.onFinalTranscript(transcript)
                }
                "Error" -> {
                    val errorMsg = json.optString("error", "AssemblyAI server error")
                    Log.e(TAG, "AssemblyAI server error message: $errorMsg")
                    listener?.onError(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "AssemblyAI parse error: ${e.message}")
            listener?.onError("AssemblyAI parse error: ${e.message}")
        }
    }

    override fun sendAudioChunk(bytes: ByteArray) {
        if (!isConnected || webSocket == null) return
        try {
            val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val jsonPayload = JSONObject().apply {
                put("audio_data", base64Data)
            }
            webSocket?.send(jsonPayload.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send audio chunk: ${e.message}")
            listener?.onError("Failed to send audio chunk: ${e.message}")
        }
    }

    override fun stop() {
        Log.d(TAG, "Stopping AssemblyAiStreamingClient...")
        tokenCall?.cancel()
        tokenCall = null
        if (webSocket != null) {
            try {
                if (isConnected) {
                    val terminatePayload = JSONObject().apply {
                        put("terminate_session", true)
                    }
                    webSocket?.send(terminatePayload.toString())
                }
                webSocket?.close(1000, "User stopped recording")
            } catch (_: Exception) {}
            webSocket = null
        }
        isConnected = false
        listener?.onClosed()
    }
}
