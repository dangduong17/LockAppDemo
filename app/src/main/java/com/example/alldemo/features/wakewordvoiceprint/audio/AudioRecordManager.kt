package com.example.alldemo.features.wakewordvoiceprint.audio

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.math.sqrt

/**
 * Manages real-time audio recording using AudioRecord.
 */
open class AudioRecordManager(private val context: Context? = null) {

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val CHUNK_SIZE_SAMPLES = 1024 // ~64ms per chunk at 16kHz
    }

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private var audioRecord: AudioRecord? = null

    /**
     * Alias for startRecordingFlow() for compatibility.
     */
    @SuppressLint("MissingPermission")
    fun startRecording(): Flow<ShortArray> = startRecordingFlow()

    /**
     * Start recording audio and emit PCM 16-bit ShortArray flow (16kHz resampled).
     */
    @SuppressLint("MissingPermission")
    fun startRecordingFlow(): Flow<ShortArray> = channelFlow {
        try {
            // 1. Permission Check
            if (context != null) {
                try {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!hasPermission) {
                        _isRecording.value = false
                        return@channelFlow
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }

            var record: AudioRecord? = null
            var actualSampleRate = SAMPLE_RATE

            // 2. Hardware Fallback Sample Rates (16000Hz -> 44100Hz -> 8000Hz)
            val candidateRates = intArrayOf(16000, 44100, 8000)

            for (rate in candidateRates) {
                try {
                    val minBufferSize = AudioRecord.getMinBufferSize(
                        rate,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT
                    )

                    if (minBufferSize <= 0) continue

                    val bufferSize = maxOf(minBufferSize, CHUNK_SIZE_SAMPLES * 2 * 2)

                    val tempRecord = try {
                        AudioRecord(
                            MediaRecorder.AudioSource.MIC,
                            rate,
                            CHANNEL_CONFIG,
                            AUDIO_FORMAT,
                            bufferSize
                        )
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        _isRecording.value = false
                        close(e)
                        return@channelFlow
                    }

                    if (tempRecord.state == AudioRecord.STATE_INITIALIZED) {
                        record = tempRecord
                        actualSampleRate = rate
                        break
                    } else {
                        tempRecord.release()
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }

            if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
                record?.release()
                _isRecording.value = false
                return@channelFlow
            }

            // 3. Safe start recording
            try {
                audioRecord = record
                record.startRecording()

                if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    record.release()
                    audioRecord = null
                    _isRecording.value = false
                    return@channelFlow
                }

                _isRecording.value = true
            } catch (e: Throwable) {
                e.printStackTrace()
                _isRecording.value = false
                record.release()
                audioRecord = null
                return@channelFlow
            }

            val rawReadSize = if (actualSampleRate == 16000) CHUNK_SIZE_SAMPLES else (CHUNK_SIZE_SAMPLES * actualSampleRate / 16000)
            val buffer = ShortArray(maxOf(1024, rawReadSize))

            // 4. Safe read loop
            try {
                while (isActive && _isRecording.value) {
                    val activeRecord = audioRecord ?: break

                    val readSize = try {
                        activeRecord.read(buffer, 0, buffer.size)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        break
                    }

                    if (readSize > 0) {
                        val rawData = buffer.copyOf(readSize)
                        val pcm16k = resampleTo16k(rawData, actualSampleRate)

                        if (pcm16k.isNotEmpty()) {
                            val rms = calculateRmsAmplitude(pcm16k)
                            _amplitude.value = rms

                            send(pcm16k)
                        }
                    } else if (readSize < 0) {
                        break
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                stopRecordingInternal(record)
                audioRecord = null
            }
        } catch (e: Exception) {
            close(e)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Stop recording and release AudioRecord resources safely.
     */
    fun stopRecording() {
        _isRecording.value = false
        val currentRecord = audioRecord
        audioRecord = null
        if (currentRecord != null) {
            stopRecordingInternal(currentRecord)
        }
    }

    private fun stopRecordingInternal(record: AudioRecord) {
        try {
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                record.stop()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            try {
                record.release()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            _isRecording.value = false
            _amplitude.value = 0f
        }
    }

    private fun resampleTo16k(input: ShortArray, inputSampleRate: Int): ShortArray {
        if (inputSampleRate == SAMPLE_RATE || input.isEmpty()) return input
        return try {
            val ratio = SAMPLE_RATE.toDouble() / inputSampleRate.toDouble()
            val outputLength = (input.size * ratio).toInt()
            if (outputLength <= 0) return ShortArray(0)
            val output = ShortArray(outputLength)
            for (i in 0 until outputLength) {
                val srcIdx = i / ratio
                val idx0 = srcIdx.toInt().coerceIn(0, input.size - 1)
                val idx1 = (idx0 + 1).coerceIn(0, input.size - 1)
                val frac = (srcIdx - idx0).toFloat()
                output[i] = (input[idx0] * (1f - frac) + input[idx1] * frac).toInt().toShort()
            }
            output
        } catch (_: Throwable) {
            input
        }
    }

    fun calculateRmsAmplitude(pcmData: ShortArray): Float {
        if (pcmData.isEmpty()) return 0f
        return try {
            var sumSquare = 0.0
            for (sample in pcmData) {
                sumSquare += (sample * sample).toDouble()
            }
            val rms = sqrt(sumSquare / pcmData.size)
            (rms / 32768.0).toFloat().coerceIn(0f, 1f)
        } catch (_: Throwable) {
            0f
        }
    }
}
