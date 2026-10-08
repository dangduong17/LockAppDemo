package com.example.alldemo.features.wakewordvoiceprint.ui.settings

enum class StandaloneTestTab {
    KWS_ONLY,
    VOICEPRINT_ONLY
}

data class KwsTestResult(
    val score: Float = 0f,
    val energy: Float = 0f,
    val detected: Boolean = false,
    val keyword: String = "",
    val thresholdUsed: Float = 0.65f
)

data class VoiceprintTestResult(
    val similarityScore: Float = 0f,
    val isMatch: Boolean = false,
    val hasMasterProfile: Boolean = false,
    val thresholdUsed: Float = 0.70f
)

data class SettingsUiState(
    val kwsThreshold: Float = 0.65f,
    val voiceprintThreshold: Float = 0.70f,
    val selectedWakeword: String = "Hey Assistant",
    val availableWakewords: List<String> = listOf("Hey Assistant", "Mở khóa", "OK Google", "Smart Home"),
    val masterVoiceprintVector: FloatArray? = null,
    val isVoiceprintEnrolled: Boolean = false,
    val voiceprintDimensions: Int = 26,
    val activeTestTab: StandaloneTestTab = StandaloneTestTab.KWS_ONLY,
    val isTestRecording: Boolean = false,
    val testAmplitude: Float = 0f,
    val kwsTestResult: KwsTestResult? = null,
    val voiceprintTestResult: VoiceprintTestResult? = null,
    val statusMessage: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SettingsUiState

        if (kwsThreshold != other.kwsThreshold) return false
        if (voiceprintThreshold != other.voiceprintThreshold) return false
        if (selectedWakeword != other.selectedWakeword) return false
        if (availableWakewords != other.availableWakewords) return false
        if (masterVoiceprintVector != null) {
            if (other.masterVoiceprintVector == null) return false
            if (!masterVoiceprintVector.contentEquals(other.masterVoiceprintVector)) return false
        } else if (other.masterVoiceprintVector != null) return false
        if (isVoiceprintEnrolled != other.isVoiceprintEnrolled) return false
        if (voiceprintDimensions != other.voiceprintDimensions) return false
        if (activeTestTab != other.activeTestTab) return false
        if (isTestRecording != other.isTestRecording) return false
        if (testAmplitude != other.testAmplitude) return false
        if (kwsTestResult != other.kwsTestResult) return false
        if (voiceprintTestResult != other.voiceprintTestResult) return false
        if (statusMessage != other.statusMessage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = kwsThreshold.hashCode()
        result = 31 * result + voiceprintThreshold.hashCode()
        result = 31 * result + selectedWakeword.hashCode()
        result = 31 * result + availableWakewords.hashCode()
        result = 31 * result + (masterVoiceprintVector?.contentHashCode() ?: 0)
        result = 31 * result + isVoiceprintEnrolled.hashCode()
        result = 31 * result + voiceprintDimensions.hashCode()
        result = 31 * result + activeTestTab.hashCode()
        result = 31 * result + isTestRecording.hashCode()
        result = 31 * result + testAmplitude.hashCode()
        result = 31 * result + (kwsTestResult?.hashCode() ?: 0)
        result = 31 * result + (voiceprintTestResult?.hashCode() ?: 0)
        result = 31 * result + (statusMessage?.hashCode() ?: 0)
        return result
    }
}
