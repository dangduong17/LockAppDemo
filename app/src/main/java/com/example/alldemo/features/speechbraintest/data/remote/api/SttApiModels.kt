package com.example.alldemo.features.speechbraintest.data.remote.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RecognizeRequest(
    @param:Json(name = "config") val config: RecognitionConfig,
    @param:Json(name = "audio") val audio: RecognitionAudio,
)

@JsonClass(generateAdapter = true)
data class RecognitionConfig(
    @param:Json(name = "encoding") val encoding: String = "LINEAR16",
    @param:Json(name = "sampleRateHertz") val sampleRateHertz: Int = 16000,
    @param:Json(name = "languageCode") val languageCode: String = "en-US",
    @param:Json(name = "enableAutomaticPunctuation") val enableAutomaticPunctuation: Boolean = true,
    @param:Json(name = "enableWordTimeOffsets") val enableWordTimeOffsets: Boolean = true,
    @param:Json(name = "model") val model: String = "default",
)

@JsonClass(generateAdapter = true)
data class RecognitionAudio(
    @param:Json(name = "content") val content: String,
)

@JsonClass(generateAdapter = true)
data class RecognizeResponse(
    @param:Json(name = "results") val results: List<SpeechRecognitionResult>? = null,
    @param:Json(name = "totalBilledTime") val totalBilledTime: String? = null,
    @param:Json(name = "requestId") val requestId: String? = null,
    @param:Json(name = "error") val error: ApiErrorDetail? = null,
)

@JsonClass(generateAdapter = true)
data class SpeechRecognitionResult(
    @param:Json(name = "alternatives") val alternatives: List<SpeechRecognitionAlternative>? = null,
    @param:Json(name = "channelTag") val channelTag: Int? = null,
    @param:Json(name = "resultEndTime") val resultEndTime: String? = null,
    @param:Json(name = "languageCode") val languageCode: String? = null,
)

@JsonClass(generateAdapter = true)
data class SpeechRecognitionAlternative(
    @param:Json(name = "transcript") val transcript: String? = null,
    @param:Json(name = "confidence") val confidence: Float? = null,
    @param:Json(name = "words") val words: List<WordInfoApi>? = null,
)

@JsonClass(generateAdapter = true)
data class WordInfoApi(
    @param:Json(name = "startTime") val startTime: String? = null,
    @param:Json(name = "endTime") val endTime: String? = null,
    @param:Json(name = "word") val word: String? = null,
    @param:Json(name = "speakerTag") val speakerTag: Int? = null,
)

@JsonClass(generateAdapter = true)
data class ApiErrorDetail(
    @param:Json(name = "code") val code: Int? = null,
    @param:Json(name = "message") val message: String? = null,
    @param:Json(name = "status") val status: String? = null,
)
