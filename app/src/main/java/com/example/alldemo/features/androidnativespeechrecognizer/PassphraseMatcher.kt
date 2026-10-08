package com.example.alldemo.features.androidnativespeechrecognizer

import java.text.Normalizer

object PassphraseMatcher {

    fun matches(recognized: String, targetPassphrase: String): Boolean {
        if (recognized.isBlank() || targetPassphrase.isBlank()) return false

        val normRecRaw = normalize(recognized, false)
        val normPassRaw = normalize(targetPassphrase, false)
        if (normRecRaw.isNotEmpty() && (normRecRaw == normPassRaw || normRecRaw.contains(normPassRaw))) {
            return true
        }

        val normRecStripped = normalize(recognized, true)
        val normPassStripped = normalize(targetPassphrase, true)
        if (normRecStripped.isNotEmpty() && (normRecStripped == normPassStripped || normRecStripped.contains(normPassStripped))) {
            return true
        }

        return false
    }

    fun normalize(text: String, stripDiacritics: Boolean): String {
        var result = text.lowercase().trim().replace(Regex("\\s+"), " ")
        result = result.replace(Regex("[^\\p{L}\\p{Nd}\\s]"), "")
        if (stripDiacritics) {
            val nfd = Normalizer.normalize(result, Normalizer.Form.NFD)
            result = Regex("\\p{InCombiningDiacriticalMarks}+").replace(nfd, "")
            result = result.replace('đ', 'd').replace('Đ', 'D')
        }
        return result.trim()
    }
}
