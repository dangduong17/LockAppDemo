package com.example.alldemo.features.huggingfacetransformersjsonnxwebruntime

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONArray

/**
 * Manages and configures the WebView hosting the Hugging Face Transformers.js / ONNX Web Runtime
 * Speech-to-Text engine.
 */
class WebViewEngine(
    context: Context,
    val javascriptBridge: JavascriptBridge = JavascriptBridge(),
) {
    companion object {
        private const val TAG = "WebViewEngine"
        const val INDEX_ASSET_URL = "file:///android_asset/index.html"
    }

    val webView: WebView = WebView(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    var isPageLoaded: Boolean = false
        private set

    init {
        configureWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        @Suppress("DEPRECATION")
        settings.databaseEnabled = true
        settings.allowFileAccess = true

        @Suppress("DEPRECATION")
        settings.allowFileAccessFromFileURLs = true
        @Suppress("DEPRECATION")
        settings.allowUniversalAccessFromFileURLs = true

        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // Attach JavascriptBridge to WebView
        webView.addJavascriptInterface(javascriptBridge, "AndroidBridge")
        webView.addJavascriptInterface(javascriptBridge, "AndroidJSBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "WebView page loaded: $url")
                isPageLoaded = true
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    Log.d(TAG, "JS Console [${it.messageLevel()}]: ${it.message()} (${it.sourceId()}:${it.lineNumber()})")
                }
                return true
            }
        }

        loadEngine()
    }

    /**
     * Load index.html from Android assets.
     */
    fun loadEngine() {
        mainHandler.post {
            webView.loadUrl(INDEX_ASSET_URL)
        }
    }

    /**
     * Pass Base64-encoded audio buffer into JS window.processAudioData(base64Audio, selectedLanguage).
     *
     * @param base64Audio Base64-encoded PCM or WAV audio data
     * @param language Selected language ("auto", "vietnamese", "english")
     * @param callback Optional result callback
     */
    fun processAudioData(base64Audio: String, language: String = "auto", callback: ((String?) -> Unit)? = null) {
        val script = "window.processAudioData('$base64Audio', '$language');"
        evaluateJavascript(script, callback)
    }

    /**
     * Pass FloatArray of normalized PCM audio samples [-1.0, 1.0] into JS window.processFloatAudio(pcmData, selectedLanguage).
     *
     * @param floatArray Float array of PCM audio samples
     * @param language Selected language ("auto", "vietnamese", "english")
     * @param callback Optional result callback
     */
    fun processFloatAudio(floatArray: FloatArray, language: String = "auto", callback: ((String?) -> Unit)? = null) {
        val jsonArray = JSONArray()
        for (sample in floatArray) {
            jsonArray.put(sample.toDouble())
        }
        val script = "window.processFloatAudio($jsonArray, '$language');"
        evaluateJavascript(script, callback)
    }

    /**
     * Pass raw 16-bit PCM byte array directly into JS processAudioData by encoding it to Base64.
     *
     * @param pcmBytes PCM 16-bit audio bytes
     * @param language Selected language ("auto", "vietnamese", "english")
     * @param callback Optional result callback
     */
    fun processAudioBytes(pcmBytes: ByteArray, language: String = "auto", callback: ((String?) -> Unit)? = null) {
        val base64 = Base64.encodeToString(pcmBytes, Base64.NO_WRAP)
        processAudioData(base64, language, callback)
    }

    /**
     * Evaluate arbitrary JavaScript string on the main UI thread.
     */
    fun evaluateJavascript(script: String, callback: ((String?) -> Unit)? = null) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            webView.evaluateJavascript(script) { result ->
                callback?.invoke(result)
            }
        } else {
            mainHandler.post {
                webView.evaluateJavascript(script) { result ->
                    callback?.invoke(result)
                }
            }
        }
    }

    /**
     * Clean up and destroy WebView resources.
     */
    fun destroy() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            performDestroy()
        } else {
            mainHandler.post {
                performDestroy()
            }
        }
    }

    private fun performDestroy() {
        webView.removeJavascriptInterface("AndroidBridge")
        webView.removeJavascriptInterface("AndroidJSBridge")
        webView.stopLoading()
        webView.destroy()
    }
}
