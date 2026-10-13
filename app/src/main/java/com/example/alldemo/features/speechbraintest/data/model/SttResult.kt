package com.example.alldemo.features.speechbraintest.data.model

data class SttResult(
    val transcript: String,
    val confidence: Float,
    val alternatives: List<TranscriptAlternative> = emptyList(),
    val wordInfoList: List<WordInfo> = emptyList(),
    val rawJson: String = ""
)

data class TranscriptAlternative(
    val transcript: String,
    val confidence: Float
)

data class WordInfo(
    val word: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val speakerTag: Int = 0
)
