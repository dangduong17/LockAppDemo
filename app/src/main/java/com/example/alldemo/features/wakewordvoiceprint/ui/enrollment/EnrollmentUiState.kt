package com.example.alldemo.features.wakewordvoiceprint.ui.enrollment

sealed interface EnrollmentStatus {
    data object Idle : EnrollmentStatus
    data class Recording(val sampleIndex: Int, val totalRequired: Int) : EnrollmentStatus
    data object Processing : EnrollmentStatus
    data class Success(
        val sampleCount: Int,
        val voiceprintVector: FloatArray
    ) : EnrollmentStatus {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Success
            return sampleCount == other.sampleCount && voiceprintVector.contentEquals(other.voiceprintVector)
        }

        override fun hashCode(): Int {
            var result = sampleCount
            result = 31 * result + voiceprintVector.contentHashCode()
            return result
        }
    }
    data class Error(val message: String) : EnrollmentStatus
}

data class EnrollmentUiState(
    val status: EnrollmentStatus = EnrollmentStatus.Idle,
    val currentSampleIndex: Int = 1,
    val totalRequiredSamples: Int = 3,
    val recordedSamplesCount: Int = 0,
    val recordedSampleVectors: List<FloatArray> = emptyList(),
    val errorMessage: String? = null
)
