package com.example.alldemo.features.huggingfacetransformersjsonnxwebruntime

import android.webkit.JavascriptInterface

/**
 * JavaScript interface bridge for receiving Speech-to-Text inference results,
 * status updates, and errors from WebView running Transformers.js / ONNX Web Runtime.
 */
class JavascriptBridge(
    private var onSpeechResultListener: ((recognizedText: String, isFinal: Boolean) -> Unit)? = null,
    private var onStatusChangeListener: ((status: String) -> Unit)? = null,
    private var onErrorListener: ((errorMessage: String) -> Unit)? = null
) {

    /**
     * Set listener for speech recognition results from JavaScript.
     */
    fun setOnSpeechResultListener(listener: (recognizedText: String, isFinal: Boolean) -> Unit) {
        this.onSpeechResultListener = listener
    }

    /**
     * Set listener for status changes (e.g. "INITIALIZING", "LOADING_MODEL", "READY", "PROCESSING").
     */
    fun setOnStatusChangeListener(listener: (status: String) -> Unit) {
        this.onStatusChangeListener = listener
    }

    /**
     * Set listener for error callbacks.
     */
    fun setOnErrorListener(listener: (errorMessage: String) -> Unit) {
        this.onErrorListener = listener
    }

    /**
     * Called by JavaScript when speech transcription completes or returns intermediate results.
     */
    @JavascriptInterface
    fun onSpeechResult(recognizedText: String, isFinal: Boolean) {
        onSpeechResultListener?.invoke(recognizedText, isFinal)
    }

    /**
     * Called by JavaScript when model or engine status changes.
     */
    @JavascriptInterface
    fun onStatusChange(status: String) {
        onStatusChangeListener?.invoke(status)
    }

    /**
     * Called by JavaScript when the Hugging Face Transformers.js model is loaded and ready.
     */
    @JavascriptInterface
    fun onModelLoaded() {
        onStatusChangeListener?.invoke("READY")
    }

    /**
     * Called by JavaScript when an error occurs in the Web Runtime / STT engine.
     */
    @JavascriptInterface
    fun onError(errorMessage: String) {
        onErrorListener?.invoke(errorMessage)
    }
}
