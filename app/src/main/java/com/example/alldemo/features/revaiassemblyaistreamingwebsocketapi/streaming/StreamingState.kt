package com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.streaming

import com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.data.ApiProvider

enum class StreamingStatus {
    IDLE,
    CONNECTING,
    CONNECTED,
    RECORDING,
    STOPPING,
    ERROR
}

data class StreamingState(
    val status: StreamingStatus = StreamingStatus.IDLE,
    val partialTranscript: String = "",
    val finalTranscripts: List<String> = emptyList(),
    val errorMessage: String? = null,
    val activeProvider: ApiProvider? = null
) {
    val fullTranscript: String
        get() {
            val list = finalTranscripts.toMutableList()
            if (partialTranscript.isNotBlank()) {
                list.add(partialTranscript)
            }
            return list.joinToString(" ").trim()
        }
}
