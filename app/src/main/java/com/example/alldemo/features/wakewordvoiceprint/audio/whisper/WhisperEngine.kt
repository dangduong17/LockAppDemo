package com.example.alldemo.features.wakewordvoiceprint.audio.whisper

import com.example.alldemo.features.wakewordvoiceprint.audio.AudioFeatureExtractor
import com.example.alldemo.features.wakewordvoiceprint.audio.KeywordSpotter
import com.example.alldemo.features.wakewordvoiceprint.audio.KwsResult

/**
 * Result of Keyword Spotting from WhisperEngine (Pure Kotlin or Native).
 */
data class WhisperEngineResult(
    val detected: Boolean,
    val score: Float,
    val energy: Float,
    val keyword: String,
    val usedNativeEngine: Boolean,
    val statusMessage: String = ""
)

/**
 * High-Level Safe Audio Keyword Spotting Engine.
 */
class WhisperEngine(
    private val keywordSpotter: KeywordSpotter = KeywordSpotter(),
    private val featureExtractor: AudioFeatureExtractor = AudioFeatureExtractor()
) {

    private var activeContext: WhisperContext? = null
    private var modelPathInternal: String? = null

    val isNativeAvailable: Boolean
        get() = WhisperNativeBridge.isNativeLoaded && activeContext?.isValid == true && (activeContext?.ptr ?: 0L) != 0L

    val isReady: Boolean
        get() = true // Pure Kotlin engine is always ready

    /**
     * Attempts loading Whisper model from file path if native bridge is enabled.
     */
    fun loadModel(modelPath: String): Boolean {
        if (!WhisperNativeBridge.isNativeLoaded) {
            closeModel()
            return false
        }

        val validation = ModelValidator.validateModelFile(modelPath)
        if (!validation.isValid) {
            closeModel()
            return false
        }

        closeModel()

        return try {
            val context = WhisperNativeBridge.initFromFile(modelPath)
            if (context != null && context.isValid) {
                activeContext = context
                modelPathInternal = modelPath
                true
            } else {
                closeModel()
                false
            }
        } catch (e: Throwable) {
            WhisperNativeBridge.disableNativeEngine("loadModel exception: ${e.message}")
            closeModel()
            false
        }
    }

    /**
     * Evaluates audio chunk for wake-word using Pure Kotlin KeywordSpotter (MFCC & Signal Analysis).
     */
    fun detectKeyword(
        pcmShorts: ShortArray,
        keyword: String = KeywordSpotter.WAKE_WORD_HEY_ASSISTANT,
        keywordTemplate: FloatArray? = null,
        kwsThreshold: Float = KeywordSpotter.DEFAULT_KWS_THRESHOLD
    ): WhisperEngineResult {
        if (pcmShorts.isEmpty()) {
            return WhisperEngineResult(
                detected = false,
                score = 0f,
                energy = 0f,
                keyword = keyword,
                usedNativeEngine = false,
                statusMessage = "PCM buffer rỗng"
            )
        }

        val energy = try {
            keywordSpotter.calculateEnergy(pcmShorts)
        } catch (_: Throwable) {
            0f
        }

        // If audio energy is below VAD threshold, skip heavy feature extraction
        if (energy < KeywordSpotter.MIN_SPEECH_ENERGY_THRESHOLD) {
            return WhisperEngineResult(
                detected = false,
                score = 0f,
                energy = energy,
                keyword = keyword,
                usedNativeEngine = false,
                statusMessage = "Âm lượng quá nhỏ"
            )
        }

        // Native C++ Execution Guard
        val context = activeContext
        if (WhisperNativeBridge.isNativeLoaded && context != null && context.isValid && context.ptr != 0L) {
            try {
                val floatPcm = pcmShortToFloat(pcmShorts)

                val nativeScore = WhisperNativeBridge.calculateKeywordScore(
                    context = context,
                    pcmData = floatPcm,
                    targetKeyword = keyword
                )

                if (WhisperNativeBridge.isNativeLoaded) {
                    val detected = nativeScore >= kwsThreshold

                    return WhisperEngineResult(
                        detected = detected,
                        score = nativeScore,
                        energy = energy,
                        keyword = keyword,
                        usedNativeEngine = true,
                        statusMessage = if (detected) "Đã phát hiện bằng Native libwhisper" else "Native libwhisper: Điểm không đủ"
                    )
                }
            } catch (e: Throwable) {
                WhisperNativeBridge.disableNativeEngine("Inference error: ${e.message}")
            }
        }

        // 100% Pure Kotlin Signal Engine Execution (MFCC & Spectral Analysis)
        val fallbackResult: KwsResult = try {
            keywordSpotter.detectKeyword(
                pcm = pcmShorts,
                keyword = keyword,
                keywordTemplate = keywordTemplate,
                kwsThreshold = kwsThreshold
            )
        } catch (e: Throwable) {
            KwsResult(
                detected = false,
                score = 0f,
                energy = energy,
                keyword = keyword,
                isNativeEngineUsed = false,
                statusMessage = "Lỗi xử lý Pure Java/Kotlin Engine: ${e.message}"
            )
        }

        return WhisperEngineResult(
            detected = fallbackResult.detected,
            score = fallbackResult.score,
            energy = fallbackResult.energy,
            keyword = fallbackResult.keyword,
            usedNativeEngine = false,
            statusMessage = "Fallback 100% Pure Java/Kotlin Engine (MFCC + Energy VAD)"
        )
    }

    /**
     * Converts 16-bit PCM ShortArray to FloatArray normalized [-1.0f, 1.0f] range safely.
     */
    fun pcmShortToFloat(pcmShorts: ShortArray): FloatArray {
        if (pcmShorts.isEmpty()) return FloatArray(0)
        val floats = FloatArray(pcmShorts.size)
        for (i in pcmShorts.indices) {
            floats[i] = (pcmShorts[i] / 32768.0f).coerceIn(-1.0f, 1.0f)
        }
        return floats
    }

    /**
     * Closes active native context safely.
     */
    fun closeModel() {
        try {
            activeContext?.close()
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            activeContext = null
            modelPathInternal = null
        }
    }
}
