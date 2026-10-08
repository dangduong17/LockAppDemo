package com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

class AudioRecorderManager {

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val DEFAULT_CHUNK_SIZE = 3200 // 100ms audio @ 16kHz 16-bit mono

        fun getMinBufferSize(): Int {
            return try {
                AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT
                )
            } catch (_: Exception) {
                DEFAULT_CHUNK_SIZE
            }
        }
    }

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startRecording(chunkSize: Int = DEFAULT_CHUNK_SIZE): Flow<ByteArray> = channelFlow {
        if (_isRecording.value) {
            return@channelFlow
        }

        val minBuf = getMinBufferSize()
        if (minBuf <= 0) {
            close(IllegalStateException("Invalid AudioRecord buffer size parameters."))
            return@channelFlow
        }

        val actualBufferSize = maxOf(minBuf, chunkSize * 2)
        var audioRecord: AudioRecord? = null

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                actualBufferSize
            )

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                close(IllegalStateException("AudioRecord failed to initialize."))
                return@channelFlow
            }

            audioRecord.startRecording()
            _isRecording.value = true

            val buffer = ByteArray(chunkSize)
            while (isActive && _isRecording.value) {
                val bytesRead = audioRecord.read(buffer, 0, buffer.size)
                if (bytesRead > 0) {
                    send(buffer.copyOf(bytesRead))
                } else if (bytesRead < 0) {
                    break
                }
            }
        } catch (e: Exception) {
            close(e)
        } finally {
            try {
                if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop()
                }
            } catch (_: Exception) {}
            audioRecord?.release()
            _isRecording.value = false
        }

        awaitClose {
            _isRecording.value = false
        }
    }.flowOn(Dispatchers.IO)

    fun stopRecording() {
        _isRecording.value = false
    }
}
