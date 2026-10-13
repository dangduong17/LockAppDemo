package com.example.alldemo.features.geminimultimodallivevoiceapi.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class AudioRecorder(
    private val sampleRate: Int = DEFAULT_SAMPLE_RATE,
    private val chunkDurationMs: Int = 100
) {
    companion object {
        const val DEFAULT_SAMPLE_RATE = 24000
        private const val TAG = "AudioRecorder"
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _audioBase64Flow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val audioBase64Flow: SharedFlow<String> = _audioBase64Flow.asSharedFlow()

    private val _audioPcmFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val audioPcmFlow: SharedFlow<ByteArray> = _audioPcmFlow.asSharedFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    var onAudioChunkListener: ((pcmChunk: ByteArray, base64Chunk: String) -> Unit)? = null

    @SuppressLint("MissingPermission")
    fun startRecording(scope: CoroutineScope): Boolean {
        if (_isRecording.value) {
            Log.w(TAG, "Recording is already in progress.")
            return true
        }

        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bytesPerSample = 2

        val chunkSize = (sampleRate * bytesPerSample * chunkDurationMs) / 1000
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = maxOf(minBufferSize, chunkSize * 2)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize.")
                audioRecord?.release()
                audioRecord = null
                return false
            }

            audioRecord?.startRecording()
            _isRecording.value = true

            recordingJob = scope.launch(Dispatchers.IO) {
                val buffer = ByteArray(chunkSize)
                while (isActive && _isRecording.value) {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (bytesRead > 0) {
                        val pcmChunk = buffer.copyOf(bytesRead)
                        val base64Chunk = Base64.encodeToString(pcmChunk, Base64.NO_WRAP)

                        val rms = calculateRms(pcmChunk)
                        _audioLevel.value = (rms / 32768f).coerceIn(0f, 1f)

                        _audioPcmFlow.emit(pcmChunk)
                        _audioBase64Flow.emit(base64Chunk)
                        onAudioChunkListener?.invoke(pcmChunk, base64Chunk)
                    } else if (bytesRead < 0) {
                        Log.e(TAG, "AudioRecord read error: $bytesRead")
                        break
                    }
                }
            }
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for recording audio", e)
            _isRecording.value = false
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error starting AudioRecord", e)
            _isRecording.value = false
            return false
        }
    }

    fun stopRecording() {
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null

        runCatching {
            audioRecord?.stop()
            audioRecord?.release()
        }.onFailure {
            Log.e(TAG, "Error releasing AudioRecord", it)
        }
        audioRecord = null
        _audioLevel.value = 0f
    }

    private fun calculateRms(pcmChunk: ByteArray): Float {
        if (pcmChunk.size < 2) return 0f
        var sum = 0.0
        val shortCount = pcmChunk.size / 2
        for (i in 0 until shortCount) {
            val sample = (pcmChunk[i * 2].toInt() and 0xFF) or (pcmChunk[i * 2 + 1].toInt() shl 8)
            val shortSample = sample.toShort()
            sum += shortSample * shortSample
        }
        return sqrt(sum / shortCount).toFloat()
    }
}
