package com.example.alldemo.features.geminimultimodallivevoiceapi.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContentPart(
    val type: String = "text",
    val text: String? = null,
    val audio: String? = null,
    val transcript: String? = null
)

@Serializable
data class ConversationItem(
    val id: String? = null,
    @SerialName("object")
    val objectType: String? = "realtime.item",
    val type: String? = "message",
    val status: String? = null,
    val role: String? = "user",
    val content: List<ContentPart>? = null,
    val name: String? = null,
    @SerialName("call_id")
    val callId: String? = null,
    val arguments: String? = null,
    val output: String? = null
)

@Serializable
data class RealtimeResponse(
    val id: String? = null,
    @SerialName("object")
    val objectType: String? = null,
    val status: String? = null,
    @SerialName("status_details")
    val statusDetails: String? = null,
    val output: List<ConversationItem>? = null,
    val usage: ResponseUsage? = null
)

@Serializable
data class ResponseUsage(
    @SerialName("total_tokens")
    val totalTokens: Int? = null,
    @SerialName("input_tokens")
    val inputTokens: Int? = null,
    @SerialName("output_tokens")
    val outputTokens: Int? = null
)

@Serializable
data class RealtimeErrorDetails(
    val type: String? = null,
    val code: String? = null,
    val message: String? = null,
    val param: String? = null,
    @SerialName("event_id")
    val eventId: String? = null
)
