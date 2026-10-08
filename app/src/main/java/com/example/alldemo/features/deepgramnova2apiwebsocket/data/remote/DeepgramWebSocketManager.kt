package com.example.alldemo.features.deepgramnova2apiwebsocket.data.remote

import android.util.Log
import com.example.alldemo.features.deepgramnova2apiwebsocket.data.model.DeepgramTranscriptResult
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONObject

/**
 * Manager class for handling real-time audio streaming and transcript receiving
 * via Deepgram Nova-2 WebSocket API.
 */
class DeepgramWebSocketManager(
    private val client: OkHttpClient = OkHttpClient()
) {
    companion object {
        private const val TAG = "DeepgramWSManager"
        const val DEFAULT_ENDPOINT =
            "wss://api.deepgram.com/v1/listen?model=nova-2&language=vi&encoding=linear16&sample_rate=16000&interim_results=true&punctuate=true&smart_format=true"
    }

    private var webSocket: WebSocket? = null

    @Volatile
    var isConnected: Boolean = false
        private set

    fun connect(
        apiKey: String,
        endpointUrl: String = DEFAULT_ENDPOINT,
        listener: DeepgramListener
    ) {
        val rawKey = apiKey.trim()
        if (rawKey.isBlank()) {
            listener.onError(IllegalArgumentException("Deepgram API Key cannot be blank"))
            return
        }

        disconnect()

        val authHeader = when {
            rawKey.startsWith("Token ", ignoreCase = true) || rawKey.startsWith("Bearer ", ignoreCase = true) -> rawKey
            else -> "Token $rawKey"
        }

        Log.d(TAG, "Connecting to WebSocket: $endpointUrl")

        val request = Request.Builder()
            .url(endpointUrl)
            .addHeader("Authorization", authHeader)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                Log.d(TAG, "WebSocket connection opened successfully")
                listener.onOpen()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleJsonMessage(text, listener)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                Log.d(TAG, "WebSocket closing: code=$code, reason=$reason")
                listener.onClose(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                Log.d(TAG, "WebSocket closed: code=$code, reason=$reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.e(TAG, "WebSocket onFailure: ${t.message}", t)

                val errorMessage = if (response != null) {
                    val code = response.code
                    val responseBodyStr = try {
                        response.body?.string()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to read response body on failure", e)
                        null
                    }
                    val parsedError = responseBodyStr?.let { parseDeepgramErrorBody(it) }

                    when {
                        parsedError != null -> "Lỗi Deepgram (HTTP $code): $parsedError"
                        code == 401 -> "Lỗi xác thực (401 Unauthorized): API Key không hợp lệ hoặc hết hạn."
                        code == 400 -> "Lỗi yêu cầu (400 Bad Request): Cấu hình URL hoặc tham số không hợp lệ."
                        code == 403 -> "Lỗi truy cập (403 Forbidden): API Key bị từ chối truy cập."
                        code == 404 -> "Lỗi địa chỉ (404 Not Found): Endpoint URL không đúng."
                        else -> "Lỗi kết nối WebSocket (HTTP $code): ${response.message}"
                    }
                } else {
                    val msg = t.localizedMessage ?: t.message
                    if (msg.isNullOrBlank()) "Lỗi kết nối Deepgram WebSocket (Kiểm tra mạng Internet)" else msg
                }

                listener.onError(Exception(errorMessage, t))
            }
        })
    }

    private fun parseDeepgramErrorBody(jsonStr: String): String? {
        return try {
            val json = JSONObject(jsonStr)
            val errCode = json.optString("err_code", "")
            val errMsg = json.optString("err_msg", json.optString("error", json.optString("message", "")))
            val reason = json.optString("reason", "")
            when {
                errCode.isNotBlank() && errMsg.isNotBlank() -> "[$errCode] $errMsg"
                errMsg.isNotBlank() -> errMsg
                reason.isNotBlank() -> reason
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun connect(
        apiKey: String,
        endpointUrl: String = DEFAULT_ENDPOINT,
        onResults: (transcript: String, isFinal: Boolean) -> Unit,
        onError: (Throwable) -> Unit,
        onOpen: (() -> Unit)? = null,
        onClose: ((Int, String) -> Unit)? = null
    ) {
        connect(
            apiKey = apiKey,
            endpointUrl = endpointUrl,
            listener = object : DeepgramListener {
                override fun onOpen() {
                    onOpen?.invoke()
                }

                override fun onResults(transcript: String, isFinal: Boolean) {
                    onResults(transcript, isFinal)
                }

                override fun onError(error: Throwable) {
                    onError(error)
                }

                override fun onClose(code: Int, reason: String) {
                    onClose?.invoke(code, reason)
                }
            }
        )
    }

    fun sendAudio(data: ByteArray, offset: Int = 0, length: Int = data.size): Boolean {
        val ws = webSocket
        if (ws == null || !isConnected) {
            Log.w(TAG, "sendAudio failed: webSocket is null or not connected (isConnected=$isConnected)")
            return false
        }
        val byteString = data.toByteString(offset, length)
        val sent = ws.send(byteString)
        if (!sent) {
            Log.w(TAG, "ws.send returned false for $length bytes")
        }
        return sent
    }

    fun closeStream() {
        val ws = webSocket
        if (ws != null && isConnected) {
            Log.d(TAG, "closeStream: sending empty ByteString frame to finish stream")
            ws.send(ByteString.EMPTY)
        }
    }

    fun disconnect(code: Int = 1000, reason: String = "Normal closure") {
        webSocket?.let { ws ->
            Log.d(TAG, "disconnect: closing socket code=$code, reason=$reason")
            if (isConnected) {
                ws.close(code, reason)
            } else {
                ws.cancel()
            }
        }
        webSocket = null
        isConnected = false
    }

    internal fun handleJsonMessage(text: String, listener: DeepgramListener) {
        try {
            Log.d(TAG, "Received message: $text")
            val json = JSONObject(text)

            val type = json.optString("type")
            if (json.has("err_code") || json.has("err_msg") || json.has("error") || type.equals("Error", ignoreCase = true)) {
                val errCode = json.optString("err_code", "ERROR")
                val errMsg = json.optString("err_msg",
                    json.optString("error",
                        json.optString("message",
                            json.optString("description", "Unknown Deepgram Error")
                        )
                    )
                )
                Log.e(TAG, "Deepgram Error received: [$errCode] $errMsg")
                listener.onError(Exception("Deepgram Error [$errCode]: $errMsg"))
                return
            }

            if (type == "KeepAlive" || type == "Metadata") {
                Log.d(TAG, "Received message type: $type")
                return
            }

            val channel = json.optJSONObject("channel") ?: return
            val alternatives = channel.optJSONArray("alternatives") ?: return
            if (alternatives.length() == 0) return

            val firstAlternative = alternatives.optJSONObject(0) ?: return
            val transcript = firstAlternative.optString("transcript", "")
            val isFinal = json.optBoolean("is_final", false)
            val confidence = firstAlternative.optDouble("confidence", 0.0)
            val speechFinal = json.optBoolean("speech_final", false)

            val result = DeepgramTranscriptResult(
                transcript = transcript,
                isFinal = isFinal,
                confidence = confidence,
                speechFinal = speechFinal
            )

            Log.d(TAG, "Parsed transcript: '${result.transcript}', isFinal=${result.isFinal}")
            listener.onResults(result.transcript, result.isFinal)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling JSON message: $text", e)
            listener.onError(e)
        }
    }
}
