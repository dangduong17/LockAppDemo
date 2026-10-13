package com.example.alldemo.features.geminimultimodallivevoiceapi.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SessionConfig(
    val modalities: List<String>? = listOf("text", "audio"),
    val instructions: String? = null,
    val voice: String? = "alloy",
    @SerialName("input_audio_format")
    val inputAudioFormat: String? = "pcm16",
    @SerialName("output_audio_format")
    val outputAudioFormat: String? = "pcm16",
    @SerialName("input_audio_transcription")
    val inputAudioTranscription: InputAudioTranscriptionConfig? = InputAudioTranscriptionConfig(model = "whisper-1"),
    @SerialName("turn_detection")
    val turnDetection: TurnDetectionConfig? = TurnDetectionConfig(),
    val tools: List<ToolConfig>? = null,
    @SerialName("tool_choice")
    val toolChoice: String? = "auto",
    val temperature: Double? = 0.8,
    @SerialName("max_response_output_tokens")
    val maxResponseOutputTokens: String? = "inf"
)

@Serializable
data class InputAudioTranscriptionConfig(
    val model: String = "whisper-1"
)

@Serializable
data class TurnDetectionConfig(
    val type: String = "server_vad",
    val threshold: Double = 0.5,
    @SerialName("prefix_padding_ms")
    val prefixPaddingMs: Int = 300,
    @SerialName("silence_duration_ms")
    val silenceDurationMs: Int = 500
)

@Serializable
data class ToolConfig(
    val type: String = "function",
    val name: String,
    val description: String? = null,
    val parameters: String? = null
)

@Serializable
data class RealtimeSession(
    val id: String? = null,
    @SerialName("object")
    val objectType: String? = null,
    val model: String? = null,
    val modalities: List<String>? = null,
    val instructions: String? = null,
    val voice: String? = null,
    @SerialName("input_audio_format")
    val inputAudioFormat: String? = null,
    @SerialName("output_audio_format")
    val outputAudioFormat: String? = null,
    @SerialName("input_audio_transcription")
    val inputAudioTranscription: InputAudioTranscriptionConfig? = null,
    @SerialName("turn_detection")
    val turnDetection: TurnDetectionConfig? = null,
    val tools: List<ToolConfig>? = null,
    @SerialName("tool_choice")
    val toolChoice: String? = null,
    val temperature: Double? = null
)
