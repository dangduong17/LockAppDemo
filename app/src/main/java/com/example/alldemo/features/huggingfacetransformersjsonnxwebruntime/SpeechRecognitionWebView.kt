package com.example.alldemo.features.huggingfacetransformersjsonnxwebruntime

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Jetpack Compose wrapper for embedding or managing the WebView component of WebViewEngine.
 */
@Composable
fun SpeechRecognitionWebView(
    webViewEngine: WebViewEngine,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = {
            webViewEngine.webView.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier
    )
}

/**
 * Audio processing helper methods for SpeechRecognitionWebView / WebViewEngine with language support.
 */
fun processFloatAudio(
    webViewEngine: WebViewEngine,
    floatArray: FloatArray,
    language: String = "auto",
    callback: ((String?) -> Unit)? = null
) {
    webViewEngine.processFloatAudio(floatArray, language, callback)
}

fun processAudioData(
    webViewEngine: WebViewEngine,
    base64Audio: String,
    language: String = "auto",
    callback: ((String?) -> Unit)? = null
) {
    webViewEngine.processAudioData(base64Audio, language, callback)
}

fun processAudioBytes(
    webViewEngine: WebViewEngine,
    pcmBytes: ByteArray,
    language: String = "auto",
    callback: ((String?) -> Unit)? = null
) {
    webViewEngine.processAudioBytes(pcmBytes, language, callback)
}
