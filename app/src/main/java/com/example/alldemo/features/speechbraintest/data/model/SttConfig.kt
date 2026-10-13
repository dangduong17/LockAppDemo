package com.example.alldemo.features.speechbraintest.data.model

import com.example.alldemo.features.speechbraintest.domain.MatchingStrategy

data class SttConfig(
    val apiKey: String = "",
    val isMockMode: Boolean = true,
    val languageCode: String = "en-US",
    val sampleRateHertz: Int = 16000,
    val encoding: AudioEncoding = AudioEncoding.LINEAR16,
    val model: String = "default",
    val baseUrl: String = "https://speech.googleapis.com/",
    val matchingStrategy: MatchingStrategy = MatchingStrategy.FUZZY,
    val matchingThreshold: Float = 0.80f,
    val defaultTargetPassword: String = "Open Sesame"
) {
    val isConfigured: Boolean
        get() = isMockMode || apiKey.isNotBlank()

    fun getMaskedApiKey(): String {
        if (apiKey.isBlank()) return "Not Set"
        if (apiKey.length <= 8) return "****"
        return "${apiKey.take(4)}...${apiKey.takeLast(4)}"
    }
}
