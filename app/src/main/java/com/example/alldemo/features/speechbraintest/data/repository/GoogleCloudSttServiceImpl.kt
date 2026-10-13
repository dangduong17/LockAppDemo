package com.example.alldemo.features.speechbraintest.data.repository

import com.example.alldemo.features.speechbraintest.data.model.AudioPayload
import com.example.alldemo.features.speechbraintest.data.model.SttConfig
import com.example.alldemo.features.speechbraintest.data.model.SttMetrics
import com.example.alldemo.features.speechbraintest.data.model.SttResult
import com.example.alldemo.features.speechbraintest.data.model.TranscriptAlternative
import com.example.alldemo.features.speechbraintest.data.model.WordInfo
import com.example.alldemo.features.speechbraintest.data.remote.ApiClient
import com.example.alldemo.features.speechbraintest.data.remote.api.GoogleCloudSttApi
import com.example.alldemo.features.speechbraintest.data.remote.api.RecognitionAudio
import com.example.alldemo.features.speechbraintest.data.remote.api.RecognitionConfig
import com.example.alldemo.features.speechbraintest.data.remote.api.RecognizeRequest
import com.example.alldemo.features.speechbraintest.data.remote.api.RecognizeResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleCloudSttServiceImpl(
    private var config: SttConfig,
    private val api: GoogleCloudSttApi = ApiClient.createSttApi(config.baseUrl),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SttService {

    fun updateConfig(newConfig: SttConfig) {
        this.config = newConfig
    }

    override suspend fun recognize(payload: AudioPayload): Result<SttResponseWithMetrics> = withContext(ioDispatcher) {
        if (config.apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Google Cloud API Key is empty. Please configure API Key or enable Mock Mode.")
            )
        }

        val startTimeMs = System.currentTimeMillis()
        val request = RecognizeRequest(
            config = RecognitionConfig(
                encoding = payload.encoding.value,
                sampleRateHertz = payload.sampleRateHertz,
                languageCode = payload.languageCode.ifBlank { config.languageCode },
                enableAutomaticPunctuation = payload.enableAutomaticPunctuation,
                enableWordTimeOffsets = payload.enableWordTimeOffsets,
                model = payload.model.ifBlank { config.model }
            ),
            audio = RecognitionAudio(
                content = payload.getBase64Content()
            )
        )

        try {
            val response = api.recognizeSpeech(
                apiKey = config.apiKey,
                request = request
            )
            val endTimeMs = System.currentTimeMillis()
            val latencyMs = endTimeMs - startTimeMs

            val rawResponseBody = response.body()
            val rawErrorBodyStr = response.errorBody()?.string()

            val rawJsonString = if (response.isSuccessful && rawResponseBody != null) {
                try {
                    ApiClient.moshi.adapter(RecognizeResponse::class.java).toJson(rawResponseBody)
                } catch (e: Exception) {
                    rawResponseBody.toString()
                }
            } else {
                rawErrorBodyStr ?: "HTTP Error ${response.code()}"
            }

            if (!response.isSuccessful || rawResponseBody == null) {
                val errorMsg = rawResponseBody?.error?.message 
                    ?: rawErrorBodyStr 
                    ?: "HTTP Error ${response.code()}: ${response.message()}"

                val metrics = SttMetrics(
                    latencyMs = latencyMs,
                    audioDurationMs = payload.durationMs,
                    confidence = 0f,
                    rawJsonSizeBytes = rawJsonString.toByteArray().size,
                    rawJson = rawJsonString,
                    httpStatusCode = response.code(),
                    isSuccess = false,
                    errorMessage = errorMsg
                )

                return@withContext Result.failure(
                    SttApiException(errorMsg, metrics)
                )
            }

            val results = rawResponseBody.results
            val firstResult = results?.firstOrNull()
            val topAlternative = firstResult?.alternatives?.firstOrNull()

            val transcript = topAlternative?.transcript.orEmpty()
            val confidence = topAlternative?.confidence ?: 0f

            val alternatives = topAlternative?.let {
                firstResult.alternatives?.map { alt ->
                    TranscriptAlternative(
                        transcript = alt.transcript.orEmpty(),
                        confidence = alt.confidence ?: 0f
                    )
                }
            } ?: emptyList()

            val wordInfoList = topAlternative?.words?.map { wordApi ->
                WordInfo(
                    word = wordApi.word.orEmpty(),
                    startTimeMs = parseSecondsToMs(wordApi.startTime),
                    endTimeMs = parseSecondsToMs(wordApi.endTime),
                    speakerTag = wordApi.speakerTag ?: 0
                )
            } ?: emptyList()

            val sttResult = SttResult(
                transcript = transcript,
                confidence = confidence,
                alternatives = alternatives,
                wordInfoList = wordInfoList,
                rawJson = rawJsonString
            )

            val metrics = SttMetrics(
                latencyMs = latencyMs,
                audioDurationMs = payload.durationMs,
                confidence = confidence,
                rawJsonSizeBytes = rawJsonString.toByteArray().size,
                rawJson = rawJsonString,
                httpStatusCode = response.code(),
                isSuccess = true
            )

            Result.success(SttResponseWithMetrics(sttResult, metrics))

        } catch (e: Exception) {
            val endTimeMs = System.currentTimeMillis()
            val latencyMs = endTimeMs - startTimeMs

            val metrics = SttMetrics(
                latencyMs = latencyMs,
                audioDurationMs = payload.durationMs,
                confidence = 0f,
                rawJsonSizeBytes = 0,
                rawJson = "",
                httpStatusCode = -1,
                isSuccess = false,
                errorMessage = e.localizedMessage ?: e.toString()
            )

            Result.failure(SttApiException(e.localizedMessage ?: "Network error", metrics))
        }
    }

    private fun parseSecondsToMs(timeStr: String?): Long {
        if (timeStr == null) return 0L
        return try {
            val cleanStr = timeStr.trim().removeSuffix("s")
            (cleanStr.toDouble() * 1000).toLong()
        } catch (e: Exception) {
            0L
        }
    }
}

class SttApiException(
    message: String,
    val metrics: SttMetrics
) : Exception(message)
