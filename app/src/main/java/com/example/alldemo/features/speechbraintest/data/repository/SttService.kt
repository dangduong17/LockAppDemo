package com.example.alldemo.features.speechbraintest.data.repository

import com.example.alldemo.features.speechbraintest.data.model.AudioPayload
import com.example.alldemo.features.speechbraintest.data.model.SttMetrics
import com.example.alldemo.features.speechbraintest.data.model.SttResult

data class SttResponseWithMetrics(
    val result: SttResult,
    val metrics: SttMetrics
)

interface SttService {
    suspend fun recognize(payload: AudioPayload): Result<SttResponseWithMetrics>
}
