package com.example.alldemo.features.huggingfacetransformersjsonnxwebruntime

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages audio recording using Android's AudioRecord API.
 * Captures 16kHz sample rate, Mono, 16-bit PCM audio.
 * Provides conversions into FloatArray ([-1.0, 1.0]) and Base64 string for JavaScript processing.
 */
class AudioCaptureManager(
    val sampleRate: Int = SAMPLE_RATE_16KHZ,
    val channelConfig: Int = CHANNEL_MONO,
    val audioFormat: Int = AUDIO_FORMAT_16BIT,
) {
    companion object {
        const val SAMPLE_RATE_16KHZ = 16000
        const val CHANNEL_MONO = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT_16BIT = AudioFormat.ENCODING_PCM_16BIT
        private const val TAG = "AudioCaptureManager"
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val isRecording = AtomicBoolean(false)
    private val accumulatedStream = ByteArrayOutputStream()

    var onAudioChunkListener: ((pcmBytes: ByteArray, readSize: Int) -> Unit)? = null
    var onFloatChunkListener: ((floatArray: FloatArray) -> Unit)? = null

    fun isRecording(): Boolean = isRecording.get()

    /**
     * Start recording audio from microphone in a background thread.
     *
     * @param onChunk optional listener called with raw PCM byte chunks
     * @param onFloatChunk optional listener called with normalized FloatArray chunks
     * @return true if recording started successfully, false otherwise
     */
    @SuppressLint("MissingPermission")
    @Synchronized
    fun startRecording(
        onChunk: ((pcmBytes: ByteArray, readSize: Int) -> Unit)? = null,
        onFloatChunk: ((floatArray: FloatArray) -> Unit)? = null,
    ): Boolean {
        if (isRecording.get()) {
            Log.w(TAG, "Recording is already in progress.")
            return false
        }

        onAudioChunkListener = onChunk
        onFloatChunkListener = onFloatChunk

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "Invalid AudioRecord buffer size: $minBufferSize")
            return false
        }

        val bufferSize = maxOf(minBufferSize, sampleRate * 2)
        audioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create AudioRecord instance", e)
            return false
        }

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord failed to initialize")
            audioRecord?.release()
            audioRecord = null
            return false
        }

        synchronized(accumulatedStream) {
            accumulatedStream.reset()
        }

        try {
            audioRecord?.startRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting AudioRecord", e)
            audioRecord?.release()
            audioRecord = null
            return false
        }

        isRecording.set(true)

        recordingThread = Thread {
            val readBuffer = ByteArray(minBufferSize)
            while (isRecording.get()) {
                val bytesRead = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: -1
                if (bytesRead > 0) {
                    val chunk = readBuffer.copyOf(bytesRead)
                    synchronized(accumulatedStream) {
                        accumulatedStream.write(chunk, 0, bytesRead)
                    }
                    onAudioChunkListener?.invoke(chunk, bytesRead)

                    if (onFloatChunkListener != null) {
                        val floatArray = convertPcm16ToFloatArray(chunk)
                        onFloatChunkListener?.invoke(floatArray)
                    }
                } else if (bytesRead < 0) {
                    Log.e(TAG, "Error reading from AudioRecord: $bytesRead")
                    break
                }
            }
        }.apply {
            name = "AudioCaptureThread"
            start()
        }

        Log.d(TAG, "Audio capture started.")
        return true
    }

    /**
     * Stop audio capture and return the accumulated PCM 16-bit byte array.
     */
    @Synchronized
    fun stopRecording(): ByteArray {
        if (!isRecording.getAndSet(false)) {
            Log.w(TAG, "stopRecording called while not recording.")
            return getCapturedBytes()
        }

        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        } finally {
            audioRecord?.release()
            audioRecord = null
        }

        try {
            recordingThread?.join(1000)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        recordingThread = null

        val result = getCapturedBytes()
        Log.d(TAG, "Audio capture stopped. Captured ${result.size} bytes.")
        return result
    }

    /**
     * Returns accumulated raw PCM bytes captured in the current or most recent session.
     */
    fun getCapturedBytes(): ByteArray {
        synchronized(accumulatedStream) {
            return accumulatedStream.toByteArray()
        }
    }

    /**
     * Returns accumulated PCM audio converted to FloatArray normalized to [-1.0, 1.0].
     */
    fun getCapturedFloatArray(): FloatArray {
        return convertPcm16ToFloatArray(getCapturedBytes())
    }

    /**
     * Returns accumulated PCM audio converted to Base64 encoded string.
     */
    fun getCapturedBase64(): String {
        return convertPcm16ToBase64(getCapturedBytes())
    }

    /**
     * Converts raw 16-bit Little-Endian PCM ByteArray to FloatArray with values in [-1.0, 1.0].
     */
    fun convertPcm16ToFloatArray(pcmBytes: ByteArray): FloatArray {
        val shortCount = pcmBytes.size / 2
        val floatArray = FloatArray(shortCount)
        val byteBuffer = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until shortCount) {
            val shortVal = byteBuffer.short
            floatArray[i] = shortVal / 32768.0f
        }
        return floatArray
    }

    /**
     * Converts ShortArray of 16-bit PCM samples to FloatArray in [-1.0, 1.0].
     */
    fun convertShortArrayToFloatArray(shorts: ShortArray, size: Int = shorts.size): FloatArray {
        val count = minOf(shorts.size, size)
        val floatArray = FloatArray(count)
        for (i in 0 until count) {
            floatArray[i] = shorts[i] / 32768.0f
        }
        return floatArray
    }

    /**
     * Converts raw PCM ByteArray to Base64 encoded String without line wraps.
     */
    fun convertPcm16ToBase64(pcmBytes: ByteArray): String {
        return Base64.encodeToString(pcmBytes, Base64.NO_WRAP)
    }

    /**
     * Converts normalized FloatArray [-1.0, 1.0] back to 16-bit PCM Base64 encoded String.
     */
    fun convertFloatArrayToBase64Pcm16(floats: FloatArray): String {
        val byteBuffer = ByteBuffer.allocate(floats.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (f in floats) {
            val clamped = f.coerceIn(-1.0f, 1.0f)
            val shortVal = (clamped * 32767.0f).toInt().toShort()
            byteBuffer.putShort(shortVal)
        }
        return Base64.encodeToString(byteBuffer.array(), Base64.NO_WRAP)
    }
}
