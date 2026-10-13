package com.example.alldemo.features.geminimultimodallivevoiceapi.data.model

import java.util.UUID

enum class MessageSender {
    USER,
    ASSISTANT,
    SYSTEM
}

data class TranscribedMessage(
    val id: String = UUID.randomUUID().toString(),
    val itemId: String? = null,
    val sender: MessageSender,
    val text: String,
    val isFinal: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
