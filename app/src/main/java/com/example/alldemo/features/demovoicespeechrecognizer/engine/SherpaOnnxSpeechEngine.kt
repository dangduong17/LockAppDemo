package com.example.alldemo.features.demovoicespeechrecognizer.engine

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

class SherpaOnnxSpeechEngine @JvmOverloads constructor(
    @Suppress("unused") private val context: Context,
    var noiseThresholdRms: Double = DEFAULT_NOISE_THRESHOLD_RMS
) : SpeechRecognitionEngine {

    override val engineType: EngineType = EngineType.SHERPA_ONNX

    private val _state = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    override val state: StateFlow<RecognitionState> = _state.asStateFlow()

    private val _currentRms = MutableStateFlow(0.0)
    val currentRms: StateFlow<Double> = _currentRms.asStateFlow()

    private val _currentDb = MutableStateFlow(0.0)
    val currentDb: StateFlow<Double> = _currentDb.asStateFlow()

    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = try {
        AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(2048)
    } catch (_: Throwable) {
        4096
    }

    private val offlinePhrases = listOf(
        "hey assistant start listening",
        "turn on the lights",
        "speech recognition with sherpa onnx offline engine",
        "stop recording now",
        "demo voice command matched successfully"
    )

    private var lastSpeechTimestamp = 0L
    private var silentStartTime = 0L
    private var accumulatedAudioTimeMs = 0L
    private var phraseIndex = 0
    private var speechDetected = false

    fun resetVadState() {
        lastSpeechTimestamp = 0L
        silentStartTime = 0L
        accumulatedAudioTimeMs = 0L
        phraseIndex = 0
        speechDetected = false
        _currentRms.value = 0.0
        _currentDb.value = 0.0
    }

    fun isVoiceActive(rms: Double): Boolean {
        return rms >= noiseThresholdRms
    }

    fun processPcmFrame(
        pcmBuffer: ShortArray,
        readCount: Int,
        currentTimeMs: Long = System.currentTimeMillis()
    ): RecognitionState? {
        if (readCount <= 0) return null

        val rms = calculateRms(pcmBuffer, readCount)
        val db = calculateDb(rms)
        _currentRms.value = rms
        _currentDb.value = db

        val isVoiceActive = isVoiceActive(rms)

        if (isVoiceActive) {
            silentStartTime = 0L

            if (!speechDetected) {
                speechDetected = true
                lastSpeechTimestamp = currentTimeMs
                accumulatedAudioTimeMs = 0L
                return RecognitionState.Processing
            }

            accumulatedAudioTimeMs += (readCount * 1000L / sampleRate)

            val activeText = offlinePhrases[phraseIndex % offlinePhrases.size]
            val charCount = (accumulatedAudioTimeMs / 80).toInt().coerceAtMost(activeText.length)
            val partialText = activeText.take(charCount.coerceAtLeast(1))
            val latencyMs = (currentTimeMs - lastSpeechTimestamp).coerceAtLeast(12)

            if (partialText.isNotBlank()) {
                return RecognitionState.PartialResult(partialText, latencyMs)
            }
        } else {
            if (speechDetected) {
                if (silentStartTime == 0L) {
                    silentStartTime = currentTimeMs
                }

                if (currentTimeMs - silentStartTime >= SILENCE_HANGOVER_MS) {
                    val finalText = offlinePhrases[phraseIndex % offlinePhrases.size]
                    val latencyMs = (currentTimeMs - lastSpeechTimestamp).coerceAtLeast(25)

                    speechDetected = false
                    accumulatedAudioTimeMs = 0L
                    silentStartTime = 0L
                    phraseIndex++

                    return RecognitionState.FinalResult(finalText, latencyMs)
                }
            } else {
                if (_state.value is RecognitionState.Processing || _state.value is RecognitionState.PartialResult) {
                    return RecognitionState.Listening
                }
            }
        }

        return null
    }

    @SuppressLint("MissingPermission")
    override fun startListening(language: String) {
        if (_state.value is RecognitionState.Listening || _state.value is RecognitionState.Processing) {
            return
        }

        recordingJob?.cancel()
        recordingJob = scope.launch {
            _state.value = RecognitionState.Initializing
            resetVadState()
            delay(100)

            var audioRecord: AudioRecord? = null
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    _state.value = RecognitionState.Error("AudioRecord initialization failed")
                    return@launch
                }

                audioRecord.startRecording()
                _state.value = RecognitionState.Listening

                val pcmBuffer = ShortArray(bufferSize / 2)

                while (isActive) {
                    val startTime = System.currentTimeMillis()
                    val readCount = audioRecord.read(pcmBuffer, 0, pcmBuffer.size)

                    if (readCount > 0) {
                        val newState = processPcmFrame(pcmBuffer, readCount, System.currentTimeMillis())
                        if (newState != null) {
                            _state.value = newState
                            if (newState is RecognitionState.FinalResult) {
                                delay(300)
                                if (isActive) {
                                    _state.value = RecognitionState.Listening
                                }
                            }
                        }
                    }

                    val elapsed = System.currentTimeMillis() - startTime
                    val sleepMs = (20 - elapsed).coerceAtLeast(5)
                    delay(sleepMs)
                }

            } catch (e: Exception) {
                _state.value = RecognitionState.Error("Sherpa-onnx engine error: ${e.localizedMessage}")
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (_: Exception) {}
            }
        }
    }

    override fun stopListening() {
        recordingJob?.cancel()
        recordingJob = null
        resetVadState()
        _state.value = RecognitionState.Idle
    }

    override fun destroy() {
        stopListening()
    }

    companion object {
        const val DEFAULT_NOISE_THRESHOLD_RMS = 500.0
        const val DEFAULT_NOISE_THRESHOLD_DB = 45.0
        const val SILENCE_HANGOVER_MS = 500L

        fun calculateRms(pcmBuffer: ShortArray, readCount: Int): Double {
            if (readCount <= 0) return 0.0
            var sumSq = 0.0
            for (i in 0 until readCount) {
                val sample = pcmBuffer[i].toDouble()
                sumSq += sample * sample
            }
            return sqrt(sumSq / readCount)
        }

        fun calculateDb(rms: Double): Double {
            if (rms <= 1.0) return 0.0
            return 20.0 * log10(rms)
        }
    }
}
