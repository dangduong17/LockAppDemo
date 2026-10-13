package com.example.alldemo.features.geminimultimodallivevoiceapi.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
sealed class RealtimeClientEvent {
    abstract val eventId: String
    abstract val type: String

    @Serializable
    @SerialName("session.update")
    data class SessionUpdate(
        @SerialName("event_id")
        override val eventId: String = UUID.randomUUID().toString(),
        override val type: String = "session.update",
        val session: SessionConfig
    ) : RealtimeClientEvent()

    @Serializable
    @SerialName("input_audio_buffer.append")
    data class InputAudioBufferAppend(
        @SerialName("event_id")
        override val eventId: String = UUID.randomUUID().toString(),
        override val type: String = "input_audio_buffer.append",
        val audio: String
    ) : RealtimeClientEvent()

    @Serializable
    @SerialName("input_audio_buffer.commit")
    data class InputAudioBufferCommit(
        @SerialName("event_id")
        override val eventId: String = UUID.randomUUID().toString(),
        override val type: String = "input_audio_buffer.commit"
    ) : RealtimeClientEvent()

    @Serializable
    @SerialName("input_audio_buffer.clear")
    data class InputAudioBufferClear(
        @SerialName("event_id")
        override val eventId: String = UUID.randomUUID().toString(),
        override val type: String = "input_audio_buffer.clear"
    ) : RealtimeClientEvent()

    @Serializable
    @SerialName("response.create")
    data class ResponseCreate(
        @SerialName("event_id")
        override val eventId: String = UUID.randomUUID().toString(),
        override val type: String = "response.create",
        val response: SessionConfig? = null
    ) : RealtimeClientEvent()

    @Serializable
    @SerialName("response.cancel")
    data class ResponseCancel(
        @SerialName("event_id")
        override val eventId: String = UUID.randomUUID().toString(),
        override val type: String = "response.cancel"
    ) : RealtimeClientEvent()

    @Serializable
    @SerialName("conversation.item.create")
    data class ConversationItemCreate(
        @SerialName("event_id")
        override val eventId: String = UUID.randomUUID().toString(),
        override val type: String = "conversation.item.create",
        @SerialName("previous_item_id")
        val previousItemId: String? = null,
        val item: ConversationItem
    ) : RealtimeClientEvent()

    @Serializable
    @SerialName("conversation.item.delete")
    data class ConversationItemDelete(
        @SerialName("event_id")
        override val eventId: String = UUID.randomUUID().toString(),
        override val type: String = "conversation.item.delete",
        @SerialName("item_id")
        val itemId: String
    ) : RealtimeClientEvent()
}
