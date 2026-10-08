package com.example.alldemo.features.deepgramnova2apiwebsocket.data.audio

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.alldemo.features.deepgramnova2apiwebsocket.data.remote.DeepgramWebSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Handles AudioRecord audio capture (PCM 16kHz 16-bit Mono)
 * and streams byte buffers to Deepgram WebSocket client via Kotlin Coroutines.
 */
class AudioRecordStreamer {

    companion object {
        private const val TAG = "AudioRecordStreamer"
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
        const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
        const val CHUNK_SIZE_BYTES = 4096
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private var activeWebSocketManager: DeepgramWebSocketManager? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording(
        coroutineScope: CoroutineScope,
        webSocketManager: DeepgramWebSocketManager,
        onError: ((Throwable) -> Unit)? = null
    ) {
        this.activeWebSocketManager = webSocketManager
        startRecording(
            coroutineScope = coroutineScope,
            onAudioChunk = { buffer, readBytes ->
                val sent = webSocketManager.sendAudio(buffer, 0, readBytes)
                if (!sent) {
                    Log.w(TAG, "Audio chunk of $readBytes bytes could not be sent (WebSocket disconnected?)")
                }
            },
            onError = onError
        )
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording(
        coroutineScope: CoroutineScope,
        onAudioChunk: (buffer: ByteArray, readBytes: Int) -> Unit,
        onError: ((Throwable) -> Unit)? = null
    ) {
        if (_isRecording.value) {
            Log.d(TAG, "Already recording, ignoring startRecording call")
            return
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_ENCODING
        )

        Log.d(TAG, "Calculated minBufferSize: $minBufferSize")

        if ((minBufferSize == AudioRecord.ERROR) || (minBufferSize == AudioRecord.ERROR_BAD_VALUE)) {
            val err = IllegalStateException("Kích thước buffer AudioRecord không hợp lệ: $minBufferSize")
            Log.e(TAG, err.message ?: "")
            onError?.invoke(err)
            return
        }

        val bufferSize = maxOf(minBufferSize, CHUNK_SIZE_BYTES * 2)

        val record = try {
            AudioRecord(
                AUDIO_SOURCE,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_ENCODING,
                bufferSize
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Thiếu quyền RECORD_AUDIO khi khởi tạo AudioRecord", e)
            onError?.invoke(e)
            return
        } catch (e: Exception) {
            Log.e(TAG, "Không thể khởi tạo AudioRecord", e)
            onError?.invoke(e)
            return
        }

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            val err = IllegalStateException("AudioRecord không thể khởi tạo thành công (STATE_UNINITIALIZED)")
            Log.e(TAG, err.message ?: "")
            onError?.invoke(err)
            return
        }

        audioRecord = record

        try {
            record.startRecording()
            Log.d(TAG, "AudioRecord startRecording() called successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gọi record.startRecording()", e)
            record.release()
            audioRecord = null
            onError?.invoke(e)
            return
        }

        _isRecording.value = true

        recordingJob = coroutineScope.launch(Dispatchers.IO) {
            val audioBuffer = ByteArray(CHUNK_SIZE_BYTES)
            var totalBytesRead = 0L
            Log.d(TAG, "Audio recording loop started")
            try {
                while (isActive && _isRecording.value) {
                    val readBytes = record.read(audioBuffer, 0, audioBuffer.size)
                    if (readBytes > 0) {
                        totalBytesRead += readBytes
                        if (totalBytesRead % (CHUNK_SIZE_BYTES * 20) == 0L) {
                            Log.d(TAG, "Recording live... Total PCM audio bytes read so far: $totalBytesRead")
                        }
                        onAudioChunk(audioBuffer, readBytes)
                    } else if (readBytes < 0) {
                        Log.e(TAG, "AudioRecord.read() returned error code: $readBytes")
                        withContext(Dispatchers.Main) {
                            onError?.invoke(IllegalStateException("Lỗi đọc dữ liệu âm thanh từ micro (mã: $readBytes)"))
                        }
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ngoại lệ trong luồng ghi âm AudioRecord", e)
                withContext(Dispatchers.Main) {
                    onError?.invoke(e)
                }
            } finally {
                Log.d(TAG, "Luồng ghi âm dừng lại. Tổng số byte đã đọc: $totalBytesRead")
                stopInternal()
            }
        }
    }

    fun stopRecording() {
        if (!_isRecording.value) {
            return
        }
        Log.d(TAG, "Stopping recording")
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null

        stopInternal()

        activeWebSocketManager?.closeStream()
        activeWebSocketManager = null
    }

    private fun stopInternal() {
        try {
            audioRecord?.let { record ->
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
                record.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Ngoại lệ khi dọn dẹp AudioRecord", e)
        } finally {
            audioRecord = null
            _isRecording.value = false
        }
    }
}
