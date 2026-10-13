package com.example.alldemo.features.speechbraintest.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.example.alldemo.features.speechbraintest.data.model.AudioEncoding
import com.example.alldemo.features.speechbraintest.data.model.AudioPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class PcmAudioRecorder(
    private val sampleRate: Int = SAMPLE_RATE_16KHZ,
    private val maxHistorySize: Int = 60
) : AudioRecorder {

    companion object {
        const val SAMPLE_RATE_16KHZ = 16000
        const val MIN_DURATION_MS = 1000L
        const val MAX_DURATION_MS = 3000L
        const val DEFAULT_DURATION_MS = 3000L
    }

    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f)
    override val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    private val _amplitudeHistory = MutableStateFlow<List<Float>>(emptyList())
    override val amplitudeHistory: StateFlow<List<Float>> = _amplitudeHistory.asStateFlow()

    private val _elapsedTimeMs = MutableStateFlow(0L)
    override val elapsedTimeMs: StateFlow<Long> = _elapsedTimeMs.asStateFlow()

    private val _targetDurationMs = MutableStateFlow(DEFAULT_DURATION_MS)
    override val targetDurationMs: StateFlow<Long> = _targetDurationMs.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var completionCallback: ((AudioPayload) -> Unit)? = null
    private val pcmOutputStream = ByteArrayOutputStream()
    private val recordingScope = CoroutineScope(Dispatchers.IO)

    @SuppressLint("MissingPermission")
    override fun startRecording(
        durationMs: Long,
        onComplete: ((AudioPayload) -> Unit)?
    ) {
        if (_isRecording.value) return

        val clampedDuration = durationMs.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)
        _targetDurationMs.value = clampedDuration
        completionCallback = onComplete

        _elapsedTimeMs.value = 0L
        _currentAmplitude.value = 0f
        _amplitudeHistory.value = emptyList()
        pcmOutputStream.reset()

        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            _isRecording.value = false
            return
        }

        try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minBufferSize * 2
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                _isRecording.value = false
                return
            }

            audioRecord = record
            record.startRecording()
            _isRecording.value = true

            recordingJob = recordingScope.launch {
                val bufferSize = 1024
                val buffer = ShortArray(bufferSize)
                val startTime = System.currentTimeMillis()

                while (_isRecording.value && record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val readShorts = record.read(buffer, 0, bufferSize)

                    if (readShorts > 0) {
                        val pcmBytes = WavUtils.shortArrayToByteArray(buffer, readShorts)
                        pcmOutputStream.write(pcmBytes)

                        val amplitude = WavUtils.calculateRmsAmplitude(buffer, readShorts)
                        val currentTime = System.currentTimeMillis()
                        val elapsed = currentTime - startTime

                        _currentAmplitude.value = amplitude
                        _elapsedTimeMs.value = elapsed

                        val updatedHistory = (_amplitudeHistory.value + amplitude).takeLast(maxHistorySize)
                        _amplitudeHistory.value = updatedHistory

                        if (elapsed >= clampedDuration) {
                            withContext(Dispatchers.Main) {
                                stopAndEmitPayload()
                            }
                            break
                        }
                    }

                    delay(40)
                }
            }

        } catch (e: SecurityException) {
            _isRecording.value = false
        } catch (e: Exception) {
            _isRecording.value = false
        }
    }

    override fun stopRecording(): AudioPayload? {
        if (!_isRecording.value) return null
        return stopAndEmitPayload()
    }

    override fun cancelRecording() {
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore
        } finally {
            audioRecord = null
        }

        pcmOutputStream.reset()
        _currentAmplitude.value = 0f
        _amplitudeHistory.value = emptyList()
        _elapsedTimeMs.value = 0L
    }

    private fun stopAndEmitPayload(): AudioPayload? {
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore
        } finally {
            audioRecord = null
        }

        val rawPcmBytes = pcmOutputStream.toByteArray()
        if (rawPcmBytes.isEmpty()) {
            return null
        }

        val wavBytes = WavUtils.pcmToWav(
            pcmBytes = rawPcmBytes,
            sampleRate = sampleRate,
            channels = 1,
            bitsPerSample = 16
        )

        val duration = _elapsedTimeMs.value
        val payload = AudioPayload(
            audioBytes = wavBytes,
            encoding = AudioEncoding.LINEAR16,
            sampleRateHertz = sampleRate,
            languageCode = "en-US",
            audioFileName = "recorded_voice_${System.currentTimeMillis()}.wav",
            durationMs = duration
        )

        val cb = completionCallback
        completionCallback = null
        cb?.invoke(payload)

        return payload
    }
}
