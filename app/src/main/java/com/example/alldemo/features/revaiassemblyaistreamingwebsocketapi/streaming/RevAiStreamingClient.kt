package com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.streaming

import android.util.Log
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class RevAiStreamingClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()
) : VoiceStreamingClient {

    private companion object {
        private const val TAG = "RevAiStreaming"
    }

    private var webSocket: WebSocket? = null
    private var listener: StreamingClientListener? = null
    @Volatile private var isConnected = false

    override fun connect(apiKey: String, listener: StreamingClientListener) {
        this.listener = listener
        val cleanApiKey = apiKey.trim()

        if (cleanApiKey.isBlank()) {
            Log.e(TAG, "Rev AI Access Token is blank")
            this.listener?.onError("Rev AI Access Token không được để trống")
            return
        }

        Log.d(TAG, "Initiating Rev AI streaming WebSocket connection")

        val httpUrl = "https://api.rev.ai/speechtotext/v1/stream".toHttpUrl().newBuilder()
            .addQueryParameter("access_token", cleanApiKey)
            .addQueryParameter("content_type", "audio/x-raw;layout=interleaved;rate=16000;format=S16LE;channels=1")
            .build()

        val wssUrl = httpUrl.toString().replace("https://", "wss://")
        val request = Request.Builder()
            .url(wssUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "Rev AI WebSocket connection opened")
                isConnected = true
                this@RevAiStreamingClient.listener?.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Rev AI message received: $text")
                handleJsonMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                val errorMsg = parseWebSocketFailureMessage(t, response)
                Log.e(TAG, "Rev AI WebSocket failure: $errorMsg", t)
                this@RevAiStreamingClient.listener?.onError(errorMsg)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "Rev AI WebSocket closed code=$code, reason=$reason")
                isConnected = false
                this@RevAiStreamingClient.listener?.onClosed()
            }
        })
    }

    internal fun parseWebSocketFailureMessage(t: Throwable, response: Response?): String {
        if (response != null) {
            val code = response.code
            val bodyString = try { response.body?.string() } catch (_: Exception) { null }
            return when (code) {
                401 -> "Lỗi xác thực Rev AI (HTTP 401 Unauthorized): Access Token Rev AI không hợp lệ hoặc đã hết hạn. Vui lòng kiểm tra lại Access Token trong Cài đặt."
                404 -> "Lỗi kết nối Rev AI (HTTP 404 Not Found): Endpoint Rev AI không khả dụng. Vui lòng kiểm tra lại Access Token."
                403 -> "Lỗi truy cập Rev AI (HTTP 403 Forbidden): Access Token không đủ quyền truy cập."
                else -> "Lỗi kết nối Rev AI (HTTP $code ${response.message}): ${bodyString ?: t.localizedMessage}"
            }
        }

        val msg = t.localizedMessage ?: t.message ?: ""
        return when {
            msg.contains("401") -> "Lỗi xác thực Rev AI (HTTP 401 Unauthorized): Access Token Rev AI không hợp lệ hoặc đã hết hạn."
            msg.contains("404") -> "Lỗi kết nối Rev AI (HTTP 404 Not Found): Vui lòng kiểm tra lại Access Token Rev AI."
            msg.contains("403") -> "Lỗi truy cập Rev AI (HTTP 403 Forbidden): Access Token không đủ quyền truy cập."
            else -> "Lỗi kết nối WebSocket Rev AI: $msg"
        }
    }

    private fun handleJsonMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type")

            when (type) {
                "connected" -> {
                    Log.i(TAG, "Rev AI server confirmed session connected")
                    if (!isConnected) {
                        isConnected = true
                        listener?.onConnected()
                    }
                }
                "partial", "final" -> {
                    val elements = json.optJSONArray("elements")
                    val textBuilder = StringBuilder()
                    if (elements != null) {
                        for (i in 0 until elements.length()) {
                            val element = elements.optJSONObject(i)
                            if (element != null) {
                                textBuilder.append(element.optString("value", ""))
                            }
                        }
                    }
                    val transcript = textBuilder.toString()
                    if (type == "partial") {
                        listener?.onPartialTranscript(transcript)
                    } else {
                        listener?.onFinalTranscript(transcript)
                    }
                }
                "error" -> {
                    val detail = json.optString("detail", "Rev AI server error")
                    Log.e(TAG, "Rev AI error frame received: $detail")
                    listener?.onError("Lỗi Rev AI: $detail")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Rev AI parse error: ${e.message}")
            listener?.onError("Rev AI parse error: ${e.message}")
        }
    }

    override fun sendAudioChunk(bytes: ByteArray) {
        if (!isConnected || webSocket == null) return
        try {
            webSocket?.send(bytes.toByteString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send binary audio chunk to Rev AI: ${e.message}")
            listener?.onError("Failed to send binary audio chunk: ${e.message}")
        }
    }

    override fun stop() {
        Log.d(TAG, "Stopping RevAiStreamingClient...")
        if (webSocket != null) {
            try {
                webSocket?.close(1000, "User stopped recording")
            } catch (_: Exception) {}
            webSocket = null
        }
        isConnected = false
        listener?.onClosed()
    }
}
