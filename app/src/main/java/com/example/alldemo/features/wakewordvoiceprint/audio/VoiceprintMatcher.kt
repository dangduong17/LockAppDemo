package com.example.alldemo.features.wakewordvoiceprint.audio

import kotlin.math.sqrt

/**
 * Compares two feature vectors using Cosine Similarity:
 * Cosine Similarity = (A . B) / (||A|| * ||B||)
 * Outputs a similarity score bounded between 0.0 and 1.0.
 */
class VoiceprintMatcher {

    /**
     * Calculates Cosine Similarity between vectorA and vectorB.
     * Returns a float score in [0.0, 1.0].
     */
    fun calculateSimilarity(vectorA: FloatArray, vectorB: FloatArray): Float {
        if (vectorA.isEmpty() || vectorB.isEmpty() || vectorA.size != vectorB.size) {
            return 0f
        }

        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in vectorA.indices) {
            val a = vectorA[i].toDouble()
            val b = vectorB[i].toDouble()

            dotProduct += a * b
            normA += a * a
            normB += b * b
        }

        if (normA <= 0.0 || normB <= 0.0) {
            return 0f
        }

        val similarity = dotProduct / (sqrt(normA) * sqrt(normB))

        // Coerce into [0.0, 1.0] range
        return similarity.toFloat().coerceIn(0f, 1f)
    }

    /**
     * Returns true if similarity score between vectorA and vectorB
     * meets or exceeds the given threshold (default 0.70f).
     */
    fun isMatch(vectorA: FloatArray, vectorB: FloatArray, threshold: Float = 0.70f): Boolean {
        return calculateSimilarity(vectorA, vectorB) >= threshold
    }
}
