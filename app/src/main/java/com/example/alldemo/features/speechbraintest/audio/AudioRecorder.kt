package com.example.alldemo.features.speechbraintest.audio

import com.example.alldemo.features.speechbraintest.data.model.AudioPayload
import kotlinx.coroutines.flow.StateFlow

interface AudioRecorder {
    val isRecording: StateFlow<Boolean>
    val currentAmplitude: StateFlow<Float>
    val amplitudeHistory: StateFlow<List<Float>>
    val elapsedTimeMs: StateFlow<Long>
    val targetDurationMs: StateFlow<Long>

    fun startRecording(
        durationMs: Long = 3000L,
        onComplete: ((AudioPayload) -> Unit)? = null
    )

    fun stopRecording(): AudioPayload?
    fun cancelRecording()
}
