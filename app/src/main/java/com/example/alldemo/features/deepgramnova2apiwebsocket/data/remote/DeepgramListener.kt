package com.example.alldemo.features.deepgramnova2apiwebsocket.data.remote

/**
 * Listener interface for Deepgram WebSocket events.
 */
interface DeepgramListener {
    fun onOpen() {}
    fun onResults(transcript: String, isFinal: Boolean)
    fun onError(error: Throwable)
    fun onClose(code: Int, reason: String) {}
}
