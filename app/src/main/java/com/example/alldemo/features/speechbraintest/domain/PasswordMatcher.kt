package com.example.alldemo.features.speechbraintest.domain

import kotlin.math.max

enum class MatchingStrategy(val displayName: String, val description: String) {
    EXACT("Exact Match", "Requires 100% exact text match"),
    FUZZY("Fuzzy Match", "Uses similarity score threshold"),
    WER_THRESHOLD("WER Threshold", "Passes if Word Error Rate <= allowed threshold")
}

data class PasswordMatchResult(
    val isMatch: Boolean,
    val similarityScore: Float,
    val wordErrorRate: Float,
    val normalizedTarget: String,
    val normalizedTranscribed: String,
    val matchingStrategy: MatchingStrategy,
    val threshold: Float
) {
    val similarityPercentage: Int
        get() = (similarityScore * 100).toInt().coerceIn(0, 100)

    val werPercentage: Int
        get() = (wordErrorRate * 100).toInt().coerceAtLeast(0)
}

object PasswordMatcher {

    fun match(
        target: String,
        transcribed: String,
        strategy: MatchingStrategy = MatchingStrategy.FUZZY,
        threshold: Float = 0.80f
    ): PasswordMatchResult {
        val normTarget = normalizeText(target)
        val normTranscribed = normalizeText(transcribed)

        if (normTarget.isEmpty()) {
            val isEmptyMatch = normTranscribed.isEmpty()
            return PasswordMatchResult(
                isMatch = isEmptyMatch,
                similarityScore = if (isEmptyMatch) 1f else 0f,
                wordErrorRate = if (isEmptyMatch) 0f else 1f,
                normalizedTarget = normTarget,
                normalizedTranscribed = normTranscribed,
                matchingStrategy = strategy,
                threshold = threshold
            )
        }

        val similarity = calculateSimilarity(normTarget, normTranscribed)
        val wer = calculateWer(normTarget, normTranscribed)

        val isMatch = when (strategy) {
            MatchingStrategy.EXACT -> normTarget == normTranscribed
            MatchingStrategy.FUZZY -> similarity >= threshold
            MatchingStrategy.WER_THRESHOLD -> wer <= (1.0f - threshold).coerceAtLeast(0f)
        }

        return PasswordMatchResult(
            isMatch = isMatch,
            similarityScore = similarity,
            wordErrorRate = wer,
            normalizedTarget = normTarget,
            normalizedTranscribed = normTranscribed,
            matchingStrategy = strategy,
            threshold = threshold
        )
    }

    fun normalizeText(text: String): String {
        return text.lowercase()
            .trim()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
    }

    fun calculateSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        if (s1.isEmpty() || s2.isEmpty()) return 0.0f

        val distance = levenshteinDistance(s1, s2)
        val maxLen = max(s1.length, s2.length)
        if (maxLen == 0) return 1.0f

        return (1.0f - distance.toFloat() / maxLen).coerceIn(0f, 1f)
    }

    fun calculateWer(reference: String, hypothesis: String): Float {
        val refWords = reference.split(" ").filter { it.isNotBlank() }
        val hypWords = hypothesis.split(" ").filter { it.isNotBlank() }

        if (refWords.isEmpty()) return if (hypWords.isEmpty()) 0f else 1f

        val dp = Array(refWords.size + 1) { IntArray(hypWords.size + 1) }

        for (i in 0..refWords.size) dp[i][0] = i
        for (j in 0..hypWords.size) dp[0][j] = j

        for (i in 1..refWords.size) {
            for (j in 1..hypWords.size) {
                val cost = if (refWords[i - 1] == hypWords[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,       // Deletion
                    dp[i][j - 1] + 1,       // Insertion
                    dp[i - 1][j - 1] + cost  // Substitution
                )
            }
        }

        return dp[refWords.size][hypWords.size].toFloat() / refWords.size
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }

        return dp[s1.length][s2.length]
    }
}
