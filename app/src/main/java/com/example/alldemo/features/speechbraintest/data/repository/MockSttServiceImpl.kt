package com.example.alldemo.features.speechbraintest.data.repository

import com.example.alldemo.features.speechbraintest.data.model.AudioPayload
import com.example.alldemo.features.speechbraintest.data.model.SttMetrics
import com.example.alldemo.features.speechbraintest.data.model.SttResult
import com.example.alldemo.features.speechbraintest.data.model.TranscriptAlternative
import com.example.alldemo.features.speechbraintest.data.model.WordInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MockSttServiceImpl(
    private var simulatedLatencyMs: Long = 420L,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SttService {

    fun setSimulatedLatency(latencyMs: Long) {
        this.simulatedLatencyMs = latencyMs
    }

    override suspend fun recognize(payload: AudioPayload): Result<SttResponseWithMetrics> = withContext(ioDispatcher) {
        val startTimeMs = System.currentTimeMillis()

        if (simulatedLatencyMs > 0) {
            delay(simulatedLatencyMs)
        }

        val actualLatencyMs = System.currentTimeMillis() - startTimeMs

        val (transcript, confidence) = getMockTranscriptForAudio(payload.audioFileName)

        val words = transcript.split(" ")
        val wordDurationMs = if (words.isNotEmpty() && payload.durationMs > 0) {
            payload.durationMs / words.size
        } else {
            300L
        }

        var currentStartMs = 100L
        val wordInfoList = words.map { word ->
            val start = currentStartMs
            val end = currentStartMs + wordDurationMs - 20
            currentStartMs = end + 20
            WordInfo(
                word = word.replace(Regex("[^a-zA-Z0-9']"), ""),
                startTimeMs = start,
                endTimeMs = end
            )
        }

        val rawJson = buildMockRawJson(transcript, confidence, wordInfoList, payload.languageCode)

        val sttResult = SttResult(
            transcript = transcript,
            confidence = confidence,
            alternatives = listOf(
                TranscriptAlternative(transcript, confidence),
                TranscriptAlternative(transcript.lowercase(), (confidence * 0.95f).coerceAtMost(1f))
            ),
            wordInfoList = wordInfoList,
            rawJson = rawJson
        )

        val metrics = SttMetrics(
            latencyMs = actualLatencyMs,
            audioDurationMs = if (payload.durationMs > 0) payload.durationMs else (words.size * 300L),
            confidence = confidence,
            rawJsonSizeBytes = rawJson.toByteArray().size,
            rawJson = rawJson,
            httpStatusCode = 200,
            isSuccess = true
        )

        Result.success(SttResponseWithMetrics(sttResult, metrics))
    }

    private fun getMockTranscriptForAudio(fileName: String): Pair<String, Float> {
        val lowerName = fileName.lowercase()
        return when {
            lowerName.contains("command") -> Pair("Turn on the living room lights and set temperature to 72 degrees", 0.98f)
            lowerName.contains("medical") -> Pair("Patient exhibits normal cardiovascular rhythm with mild hypertension", 0.94f)
            lowerName.contains("noisy") -> Pair("The quick brown fox jumps over the lazy dog in background noise", 0.82f)
            lowerName.contains("vietnamese") || lowerName.contains("vi") -> Pair("Xin chào đây là bài kiểm tra nhận dạng giọng nói", 0.96f)
            else -> Pair("SpeechBrain test sample audio recognition completed successfully", 0.96f)
        }
    }

    private fun buildMockRawJson(
        transcript: String,
        confidence: Float,
        words: List<WordInfo>,
        languageCode: String
    ): String {
        val wordsJson = words.joinToString(",\n        ") { wordInfo ->
            """
            {
              "startTime": "${wordInfo.startTimeMs / 1000.0}s",
              "endTime": "${wordInfo.endTimeMs / 1000.0}s",
              "word": "${wordInfo.word}"
            }
            """.trimIndent()
        }

        return """
        {
          "results": [
            {
              "alternatives": [
                {
                  "transcript": "$transcript",
                  "confidence": $confidence,
                  "words": [
                    $wordsJson
                  ]
                }
              ],
              "languageCode": "$languageCode"
            }
          ]
        }
        """.trimIndent()
    }
}
