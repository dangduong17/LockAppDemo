package com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.util

/**
 * Utility for verifying whether a speech-recognized transcript matches or contains a target password.
 */
object PasswordVerifier {

    /**
     * Normalizes text by converting to lowercase, trimming leading/trailing whitespace,
     * and collapsing internal multiple spaces into a single space.
     */
    fun normalizeText(text: String): String {
        return text.lowercase()
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    /**
     * Checks if [transcript] matches [targetPassword].
     *
     * Matching is case-insensitive, ignores excess whitespace, and returns true if the
     * normalized transcript either exactly equals or contains the normalized target password.
     *
     * @param transcript The speech recognition result (partial or final transcript).
     * @param targetPassword The target password or keyword to compare against.
     * @return `true` if password matches or is contained in the transcript, `false` otherwise.
     */
    fun verifyPassword(transcript: String, targetPassword: String): Boolean {
        if (transcript.isBlank() || targetPassword.isBlank()) {
            return false
        }

        val normalizedTranscript = normalizeText(transcript)
        val normalizedTarget = normalizeText(targetPassword)

        if (normalizedTarget.isEmpty()) {
            return false
        }

        return normalizedTranscript == normalizedTarget || normalizedTranscript.contains(normalizedTarget)
    }
}
