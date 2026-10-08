package com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.streaming

interface VoiceStreamingClient {
    fun connect(apiKey: String, listener: StreamingClientListener)
    fun sendAudioChunk(bytes: ByteArray)
    fun stop()
}
