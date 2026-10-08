package com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.streaming

interface StreamingClientListener {
    fun onConnected()
    fun onPartialTranscript(text: String)
    fun onFinalTranscript(text: String)
    fun onError(error: String)
    fun onClosed()
}
