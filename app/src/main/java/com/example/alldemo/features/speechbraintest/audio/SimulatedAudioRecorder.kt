package com.example.alldemo.features.speechbraintest.audio

import com.example.alldemo.features.speechbraintest.data.model.AudioEncoding
import com.example.alldemo.features.speechbraintest.data.model.AudioPayload
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

class SimulatedAudioRecorder(
    private val maxHistorySize: Int = 60,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default
) : AudioRecorder {

    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f)
    override val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    private val _amplitudeHistory = MutableStateFlow<List<Float>>(emptyList())
    override val amplitudeHistory: StateFlow<List<Float>> = _amplitudeHistory.asStateFlow()

    private val _elapsedTimeMs = MutableStateFlow(0L)
    override val elapsedTimeMs: StateFlow<Long> = _elapsedTimeMs.asStateFlow()

    private val _targetDurationMs = MutableStateFlow(3000L)
    override val targetDurationMs: StateFlow<Long> = _targetDurationMs.asStateFlow()

    private var recordingJob: Job? = null
    private var completionCallback: ((AudioPayload) -> Unit)? = null

    override fun startRecording(
        durationMs: Long,
        onComplete: ((AudioPayload) -> Unit)?
    ) {
        if (_isRecording.value) return

        val clampedDuration = durationMs.coerceIn(1000L, 3000L)
        _targetDurationMs.value = clampedDuration
        completionCallback = onComplete

        _elapsedTimeMs.value = 0L
        _currentAmplitude.value = 0f
        _amplitudeHistory.value = emptyList()
        _isRecording.value = true

        recordingJob = CoroutineScope(coroutineDispatcher).launch {
            var step = 0
            var elapsed = 0L
            val stepDelay = 50L

            while (_isRecording.value) {
                _elapsedTimeMs.value = elapsed

                val baseWave = (sin(step * 0.2) + 1.0) / 2.0
                val noise = Random.nextDouble(0.1, 0.4)
                val simulatedAmplitude = ((baseWave * 0.7 + noise).toFloat()).coerceIn(0.05f, 0.95f)

                _currentAmplitude.value = simulatedAmplitude
                val updatedHistory = (_amplitudeHistory.value + simulatedAmplitude).takeLast(maxHistorySize)
                _amplitudeHistory.value = updatedHistory

                if (elapsed >= clampedDuration) {
                    stopAndEmitPayload()
                    break
                }

                step++
                delay(stepDelay)
                elapsed += stepDelay
            }
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
        _currentAmplitude.value = 0f
        _amplitudeHistory.value = emptyList()
        _elapsedTimeMs.value = 0L
    }

    private fun stopAndEmitPayload(): AudioPayload? {
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null

        val duration = _elapsedTimeMs.value.coerceAtLeast(1000L)
        val pcmSize = (16000 * 2 * (duration / 1000.0)).toInt()
        val dummyPcm = ByteArray(pcmSize) { (it % 128).toByte() }
        val wavBytes = WavUtils.pcmToWav(dummyPcm, 16000, 1, 16)

        val payload = AudioPayload(
            audioBytes = wavBytes,
            encoding = AudioEncoding.LINEAR16,
            sampleRateHertz = 16000,
            languageCode = "en-US",
            audioFileName = "simulated_voice_${System.currentTimeMillis()}.wav",
            durationMs = duration
        )

        val cb = completionCallback
        completionCallback = null
        cb?.invoke(payload)

        return payload
    }
}
