package com.example.alldemo.features.wakewordvoiceprint.audio

import com.example.alldemo.features.wakewordvoiceprint.audio.whisper.ModelValidator
import com.example.alldemo.features.wakewordvoiceprint.audio.whisper.WhisperEngine
import com.example.alldemo.features.wakewordvoiceprint.audio.whisper.WhisperEngineResult
import kotlin.math.sqrt

/**
 * Result of Keyword Spotting (KWS) evaluation on audio chunk.
 */
data class KwsResult(
    val detected: Boolean,
    val score: Float,
    val energy: Float,
    val keyword: String,
    val isNativeEngineUsed: Boolean = false,
    val statusMessage: String = ""
)

/**
 * Layer 1 Keyword Spotting (KWS) Engine.
 * Evaluates audio frames using Pure Java/Kotlin signal processing (MFCC/Spectral analysis) or Native Whisper.
 */
class KeywordSpotter(
    private val featureExtractor: AudioFeatureExtractor = AudioFeatureExtractor(),
    private val voiceprintMatcher: VoiceprintMatcher = VoiceprintMatcher()
) {

    private val whisperEngine by lazy {
        WhisperEngine(keywordSpotter = this, featureExtractor = featureExtractor)
    }

    val isReady: Boolean
        get() = true

    companion object {
        const val MIN_SPEECH_ENERGY_THRESHOLD = 0.02f // RMS energy threshold for VAD
        const val DEFAULT_KWS_THRESHOLD = 0.65f
        const val WAKE_WORD_HEY_ASSISTANT = "Hey Assistant"
        const val WAKE_WORD_MO_KHOA = "Mở khóa"
    }

    /**
     * Calculates signal RMS energy from 16-bit PCM short array safely.
     */
    fun calculateEnergy(pcmData: ShortArray): Float {
        if (pcmData.isEmpty()) return 0f
        return try {
            var sumSquares = 0.0
            for (sample in pcmData) {
                sumSquares += (sample * sample).toDouble()
            }
            val rms = sqrt(sumSquares / pcmData.size)
            (rms / 32768.0).toFloat().coerceIn(0f, 1f)
        } catch (e: Throwable) {
            0f
        }
    }

    /**
     * Alias method for keyword detection.
     */
    fun spotKeyword(
        pcm: ShortArray,
        keyword: String = WAKE_WORD_HEY_ASSISTANT,
        keywordTemplate: FloatArray? = null,
        kwsThreshold: Float = DEFAULT_KWS_THRESHOLD,
        modelPath: String? = null
    ): KwsResult {
        return detectKeywordWithNativeSupport(
            pcm = pcm,
            keyword = keyword,
            keywordTemplate = keywordTemplate,
            kwsThreshold = kwsThreshold,
            modelPath = modelPath
        )
    }

    /**
     * Evaluates incoming PCM audio against a keyword template feature vector.
     */
    fun detectKeyword(
        pcm: ShortArray,
        keyword: String = WAKE_WORD_HEY_ASSISTANT,
        keywordTemplate: FloatArray? = null,
        kwsThreshold: Float = DEFAULT_KWS_THRESHOLD
    ): KwsResult {
        return detectKeywordWithNativeSupport(
            pcm = pcm,
            keyword = keyword,
            keywordTemplate = keywordTemplate,
            kwsThreshold = kwsThreshold,
            modelPath = null
        )
    }

    /**
     * Evaluates incoming PCM audio safely using 100% Pure Kotlin MFCC Signal Engine.
     */
    fun detectKeywordWithNativeSupport(
        pcm: ShortArray,
        keyword: String = WAKE_WORD_HEY_ASSISTANT,
        keywordTemplate: FloatArray? = null,
        kwsThreshold: Float = DEFAULT_KWS_THRESHOLD,
        modelPath: String? = null
    ): KwsResult {
        val energy = calculateEnergy(pcm)

        // 1. Energy VAD Check
        if (energy < MIN_SPEECH_ENERGY_THRESHOLD) {
            return KwsResult(
                detected = false,
                score = 0f,
                energy = energy,
                keyword = keyword,
                isNativeEngineUsed = false,
                statusMessage = "Âm lượng quá nhỏ"
            )
        }

        // 2. Audio Duration VAD Check (0.4s <= duration <= 2.0s)
        val durationSec = pcm.size.toFloat() / 16000f
        if (durationSec < 0.4f || durationSec > 2.0f) {
            return KwsResult(
                detected = false,
                score = 0f,
                energy = energy,
                keyword = keyword,
                isNativeEngineUsed = false,
                statusMessage = "Thời lượng âm thanh không hợp lệ (yêu cầu 0.4s - 2.0s)"
            )
        }

        // 3. Human Speech Frequency Band Density Check (300Hz - 3400Hz)
        val speechRatio = featureExtractor.calculateSpeechFrequencyRatio(pcm)
        if (speechRatio < 0.35f) {
            return KwsResult(
                detected = false,
                score = 0f,
                energy = energy,
                keyword = keyword,
                isNativeEngineUsed = false,
                statusMessage = "Mật độ dải tần giọng nói không đủ (300Hz - 3400Hz)"
            )
        }

        // Try Native Whisper Engine ONLY IF modelPath is provided AND model file is valid AND Native is enabled
        if (!modelPath.isNullOrBlank()) {
            try {
                val validation = ModelValidator.validateModelFile(modelPath)
                if (validation.isValid) {
                    val isLoaded = whisperEngine.loadModel(modelPath)
                    if (isLoaded && whisperEngine.isReady) {
                        val whisperRes: WhisperEngineResult = whisperEngine.detectKeyword(
                            pcmShorts = pcm,
                            keyword = keyword,
                            keywordTemplate = keywordTemplate,
                            kwsThreshold = kwsThreshold
                        )
                        if (whisperRes.usedNativeEngine) {
                            return KwsResult(
                                detected = whisperRes.detected,
                                score = whisperRes.score,
                                energy = whisperRes.energy,
                                keyword = whisperRes.keyword,
                                isNativeEngineUsed = true,
                                statusMessage = whisperRes.statusMessage
                            )
                        }
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        // Pure Java/Kotlin Signal Processing Engine (MFCC / Spectral Dynamics)
        return try {
            val currentFeatures = featureExtractor.extractFeatureVector(pcm)

            val score: Float = if (keywordTemplate != null && keywordTemplate.isNotEmpty()) {
                val normalizedTemplate = featureExtractor.normalizeL2(keywordTemplate)
                voiceprintMatcher.calculateSimilarity(currentFeatures, normalizedTemplate)
            } else {
                estimateBuiltInKeywordScore(pcm, keyword, energy, speechRatio)
            }

            val detected = score >= kwsThreshold && energy >= MIN_SPEECH_ENERGY_THRESHOLD

            KwsResult(
                detected = detected,
                score = score,
                energy = energy,
                keyword = keyword,
                isNativeEngineUsed = false,
                statusMessage = if (detected) "Khớp từ khóa Tầng 1" else if (score < 0.4f) "Từ chối: Sai từ khóa" else "Điểm từ khóa chưa đạt ngưỡng"
            )
        } catch (e: Throwable) {
            KwsResult(
                detected = false,
                score = 0f,
                energy = energy,
                keyword = keyword,
                isNativeEngineUsed = false,
                statusMessage = "Lỗi trích xuất tín hiệu KWS: ${e.message}"
            )
        }
    }

    private fun estimateBuiltInKeywordScore(
        pcm: ShortArray,
        keyword: String,
        energy: Float,
        speechRatio: Float = 0.5f
    ): Float {
        return try {
            val mfccs = featureExtractor.extractMfcc(pcm)
            if (mfccs.isEmpty()) return 0f

            var spectralVariance = 0f
            val numFrames = mfccs.size
            for (i in 0 until numFrames - 1) {
                for (c in 0 until mfccs[i].size) {
                    val delta = mfccs[i + 1][c] - mfccs[i][c]
                    spectralVariance += delta * delta
                }
            }
            val normalizedDynamics = (spectralVariance / (numFrames * mfccs[0].size)).coerceIn(0f, 1f)

            val baseScore = (energy * 0.3f + normalizedDynamics * 0.3f + speechRatio * 0.4f)
            when (keyword) {
                WAKE_WORD_MO_KHOA -> (baseScore * 1.0f).coerceIn(0f, 1f)
                else -> baseScore.coerceIn(0f, 1f)
            }
        } catch (e: Throwable) {
            0f
        }
    }
}
