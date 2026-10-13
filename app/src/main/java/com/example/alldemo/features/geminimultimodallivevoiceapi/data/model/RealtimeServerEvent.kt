package com.example.alldemo.features.geminimultimodallivevoiceapi.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
sealed class RealtimeServerEvent {
    abstract val eventId: String?
    abstract val type: String

    @Serializable
    @SerialName("error")
    data class Error(
        @SerialName("event_id") override val eventId: String? = null,
        override val type: String = "error",
        val error: RealtimeErrorDetails
    ) : RealtimeServerEvent()

    @Serializable
    @SerialName("session.created")
    data class SessionCreated(
        @SerialName("event_id") override val eventId: String? = null,
        override val type: String = "session.created",
        val session: RealtimeSession? = null
    ) : RealtimeServerEvent()

    @Serializable
    @SerialName("session.updated")
    data class SessionUpdated(
        @SerialName("event_id") override val eventId: String? = null,
        override val type: String = "session.updated",
        val session: RealtimeSession? = null
    ) : RealtimeServerEvent()

    @Serializable
    @SerialName("input_audio_buffer.committed")
    data class InputAudioBufferCommitted(
        @SerialName("event_id") override val eventId: String? = null,
        override val type: String = "input_audio_buffer.committed",
        @SerialName("item_id") val itemId: String? = null
    ) : RealtimeServerEvent()

    @Serializable
    @SerialName("input_audio_buffer.cleared")
    data class InputAudioBufferCleared(
        @SerialName("event_id") override val eventId: String? = null,
        override val type: String = "input_audio_buffer.cleared"
    ) : RealtimeServerEvent()

    @Serializable
    @SerialName("input_audio_buffer.speech_started")
    data class InputAudioBufferSpeechStarted(
        @SerialName("event_id") override val eventId: String? = null,
        override val type: String = "input_audio_buffer.speech_started",
        @SerialName("audio_start_ms") val audioStartMs: Long = 0,
        @SerialName("item_id") val itemId: String? = null
    ) : RealtimeServerEvent()

    @Serializable
    @SerialName("input_audio_buffer.speech_stopped")
    data class InputAudioBufferSpeechStopped(
        @SerialName("event_id") override val eventId: String? = null,
        override val type: String = "input_audio_buffer.speech_stopped",
        @SerialName("audio_end_ms") val audioEndMs: Long = 0,
        @SerialName("item_id") val itemId: String? = null
    ) : RealtimeServerEvent()

    @Serializable
    @SerialName("conversation.item.created")
    data class ConversationItemCreated(
        @SerialName("event_id") override val eventId: String? = null,
        override val type: String = "conversation.item.created",
        @SerialName("previous_item_id") val previousItemId: String? = null,
        val item: ConversationItem? = null
    ) : RealtimeServerEvent()

    @Serializable
    @SerialName("conversation.item.input_audio_transcription.completed")
    data class InputAudioTranscriptionCompleted(
        @SerialName("event_id") override val eventId: String? = null,
        override val type: String = "conversation.item.input_audio_transcription.completed",
        @SerialName("item_id") val itemId: String,
        @SerialName("content_index") val contentIndex: Int = 0,
        val transcript: String = ""
    ) : RealtimeServerEvent()

    @Serializable
    @SerialName("conversation.item.input_audio_transcription.failed")
    data class InputAudioTranscriptionFailed(
        @SerialName("event_id") override val eventId: String? = null,
        override val type: String = "conversation.item.input_audio_transcription.failed",
        @SerialName("item_id") val itemId: String,
        @SerialName("content_index") val contentIndex: Int = 0,
        val error: RealtimeErrorDetails? = null
    ) : RealtimeServerEvent()

    @Serializable
    @SerialName("response.created")
    data class ResponseCreated(
        @SerialName("event_id") override val eventId: String? = null,
        override val type: String = "response.created",
        val response: RealtimeResponse? = null
    ) : RealtimeServerEvent()

    @Serializable
    @SerialName("response.done")
    data class ResponseDone(
        @SerialName("event_id") override val eventId: String? = null,
        override val type: String = "response.done",
        val response: RealtimeResponse? = null
    ) : RealtimeServerEvent()

    @Serializable
    @SerialName("response.output_item.added")
    data class ResponseOutputItemAdded(
        @SerialName("event_id") override val eventId: String? = null,
        override val type: String = "response.output_item.added",
        @SerialName("response_id") val responseId: String? = null,
        @SerialName("output_index") val outputIndex: Int = 0,
        val item: ConversationItem? = null
    ) : RealtimeServerEvent()

    @Serializable
    @SerialName("response.output_item.done")
    data class ResponseOutputItemDone(
        @SerialName("event_id") override val eventId: String? = null,
        override val type: String = "response.output_item.done",
        @SerialName("response_id") val responseId: String? = null,
        @SerialName("output_index") val outputIndex: Int = 0,
        val item: ConversationItem? = null
    ) : RealtimeServerEvent()

    @Serializable
    @SerialName("response.content_part.added")
    data class ResponseContentPartAdded(
        @SerialName("event_id") override val eventId: String? = null,
        override val type: String = "response.content_part.added",
        @SerialName("response_id") val responseId: String? = null,
        @SerialName("item_id") val itemId: String? = null,
        @SerialName("output_index") val outputIndex: Int = 0,
        @SerialName("content_index") val contentIndex: Int = 0,
        val part: ContentPart? = null
    ) : RealtimeServerEvent()

    @Serializable
    @SerialName("response.content_part.done")
    data class ResponseContentPartDone(
        @SerialName("event_id") override val eventId: String? = null,
        override val type: String = "response.content_part.done",
        @SerialName("response_id") val responseId: String? = null,
        @SerialName("item_id") val itemId: String? = null,
        @SerialName("output_index") val outputIndex: Int = 0,
        @SerialName("content_index") val contentIndex: Int = 0,
        val part: ContentPart? = null
    ) : RealtimeServerEvent()

    @Serializable
    @SerialName("response.text.delta")
    data class ResponseTextDelta(
        @SerialName("event_id") override val eventId: String? = null,
        override val type: String = "response.text.delta",
        @SerialName("response_id") val responseId: String? = null,
        @SerialName("item_id") val itemId: String? = null,
        @SerialName("output_index") val outputIndex: Int = 0,
        @SerialName("content_index") val contentIndex: Int = 0,
        val delta: String = ""
    ) : RealtimeServerEvent()

    @Serializable
    @SerialName("response.text.done")
    data class ResponseTextDone(
        @SerialName("event_id") override val eventId: String? = null,
        override val type: String = "response.text.done",
        @SerialName("response_id") val responseId: String? = null,
        @SerialName("item_id") val itemId: String? = null,
        @SerialName("output_index") val outputIndex: Int = 0,
        @SerialName("content_index") val contentIndex: Int = 0,
        val text: String = ""
    ) : RealtimeServerEvent()

    @Serializable
    @SerialName("response.audio_transcript.delta")
    data class ResponseAudioTranscriptDelta(
        @SerialName("event_id") override val eventId: String? = null,
        override val type: String = "response.audio_transcript.delta",
        @SerialName("response_id") val responseId: String? = null,
        @SerialName("item_id") val itemId: String? = null,
        @SerialName("output_index") val outputIndex: Int = 0,
        @SerialName("content_index") val contentIndex: Int = 0,
        val delta: String = ""
    ) : RealtimeServerEvent()

    @Serializable
    @SerialName("response.audio_transcript.done")
    data class ResponseAudioTranscriptDone(
        @SerialName("event_id") override val eventId: String? = null,
        override val type: String = "response.audio_transcript.done",
        @SerialName("response_id") val responseId: String? = null,
        @SerialName("item_id") val itemId: String? = null,
        @SerialName("output_index") val outputIndex: Int = 0,
        @SerialName("content_index") val contentIndex: Int = 0,
        val transcript: String = ""
    ) : RealtimeServerEvent()

    @Serializable
    @SerialName("response.audio.delta")
    data class ResponseAudioDelta(
        @SerialName("event_id") override val eventId: String? = null,
        override val type: String = "response.audio.delta",
        @SerialName("response_id") val responseId: String? = null,
        @SerialName("item_id") val itemId: String? = null,
        @SerialName("output_index") val outputIndex: Int = 0,
        @SerialName("content_index") val contentIndex: Int = 0,
        val delta: String = ""
    ) : RealtimeServerEvent()

    @Serializable
    @SerialName("response.audio.done")
    data class ResponseAudioDone(
        @SerialName("event_id") override val eventId: String? = null,
        override val type: String = "response.audio.done",
        @SerialName("response_id") val responseId: String? = null,
        @SerialName("item_id") val itemId: String? = null,
        @SerialName("output_index") val outputIndex: Int = 0,
        @SerialName("content_index") val contentIndex: Int = 0
    ) : RealtimeServerEvent()

    @Serializable
    data class Unknown(
        @SerialName("event_id") override val eventId: String? = null,
        override val type: String = "unknown",
        val rawJson: String = ""
    ) : RealtimeServerEvent()

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        fun parse(text: String): RealtimeServerEvent {
            return runCatching {
                val jsonObject = json.parseToJsonElement(text).jsonObject
                val type = jsonObject["type"]?.jsonPrimitive?.content ?: return Unknown(rawJson = text)
                when (type) {
                    "error" -> json.decodeFromString<Error>(text)
                    "session.created" -> json.decodeFromString<SessionCreated>(text)
                    "session.updated" -> json.decodeFromString<SessionUpdated>(text)
                    "input_audio_buffer.committed" -> json.decodeFromString<InputAudioBufferCommitted>(text)
                    "input_audio_buffer.cleared" -> json.decodeFromString<InputAudioBufferCleared>(text)
                    "input_audio_buffer.speech_started" -> json.decodeFromString<InputAudioBufferSpeechStarted>(text)
                    "input_audio_buffer.speech_stopped" -> json.decodeFromString<InputAudioBufferSpeechStopped>(text)
                    "conversation.item.created" -> json.decodeFromString<ConversationItemCreated>(text)
                    "conversation.item.input_audio_transcription.completed" -> json.decodeFromString<InputAudioTranscriptionCompleted>(text)
                    "conversation.item.input_audio_transcription.failed" -> json.decodeFromString<InputAudioTranscriptionFailed>(text)
                    "response.created" -> json.decodeFromString<ResponseCreated>(text)
                    "response.done" -> json.decodeFromString<ResponseDone>(text)
                    "response.output_item.added" -> json.decodeFromString<ResponseOutputItemAdded>(text)
                    "response.output_item.done" -> json.decodeFromString<ResponseOutputItemDone>(text)
                    "response.content_part.added" -> json.decodeFromString<ResponseContentPartAdded>(text)
                    "response.content_part.done" -> json.decodeFromString<ResponseContentPartDone>(text)
                    "response.text.delta" -> json.decodeFromString<ResponseTextDelta>(text)
                    "response.text.done" -> json.decodeFromString<ResponseTextDone>(text)
                    "response.audio_transcript.delta" -> json.decodeFromString<ResponseAudioTranscriptDelta>(text)
                    "response.audio_transcript.done" -> json.decodeFromString<ResponseAudioTranscriptDone>(text)
                    "response.audio.delta" -> json.decodeFromString<ResponseAudioDelta>(text)
                    "response.audio.done" -> json.decodeFromString<ResponseAudioDone>(text)
                    else -> Unknown(eventId = jsonObject["event_id"]?.jsonPrimitive?.content, type = type, rawJson = text)
                }
            }.getOrElse {
                Unknown(rawJson = text)
            }
        }
    }
}
