package com.example.alldemo.features.speechbraintest.data.model

data class AudioPayload(
    val audioBytes: ByteArray? = null,
    val base64Audio: String? = null,
    val encoding: AudioEncoding = AudioEncoding.LINEAR16,
    val sampleRateHertz: Int = 16000,
    val languageCode: String = "en-US",
    val audioFileName: String = "audio_sample.wav",
    val durationMs: Long = 0L,
    val enableWordTimeOffsets: Boolean = true,
    val enableAutomaticPunctuation: Boolean = true,
    val model: String = "default"
) {
    fun getBase64Content(): String {
        return when {
            !base64Audio.isNullOrEmpty() -> base64Audio
            audioBytes != null -> android.util.Base64.encodeToString(audioBytes, android.util.Base64.NO_WRAP)
            else -> ""
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioPayload
        if (audioBytes != null) {
            if (other.audioBytes == null) return false
            if (!audioBytes.contentEquals(other.audioBytes)) return false
        } else if (other.audioBytes != null) return false
        if (base64Audio != other.base64Audio) return false
        if (encoding != other.encoding) return false
        if (sampleRateHertz != other.sampleRateHertz) return false
        if (languageCode != other.languageCode) return false
        if (audioFileName != other.audioFileName) return false
        if (durationMs != other.durationMs) return false
        return true
    }

    override fun hashCode(): Int {
        var result = audioBytes?.contentHashCode() ?: 0
        result = 31 * result + (base64Audio?.hashCode() ?: 0)
        result = 31 * result + encoding.hashCode()
        result = 31 * result + sampleRateHertz
        result = 31 * result + languageCode.hashCode()
        result = 31 * result + audioFileName.hashCode()
        result = 31 * result + durationMs.hashCode()
        return result
    }
}

enum class AudioEncoding(val value: String) {
    ENCODING_UNSPECIFIED("ENCODING_UNSPECIFIED"),
    LINEAR16("LINEAR16"),
    FLAC("FLAC"),
    MULAW("MULAW"),
    AMR("AMR"),
    AMR_WB("AMR_WB"),
    OGG_OPUS("OGG_OPUS"),
    MP3("MP3"),
    WEBM_OPUS("WEBM_OPUS")
}
