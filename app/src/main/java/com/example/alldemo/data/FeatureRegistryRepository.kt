package com.example.alldemo.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.ui.graphics.Color
import com.example.alldemo.model.DemoCategory
import com.example.alldemo.model.DemoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface FeatureRegistryRepository {
    fun getDemoItems(): Flow<List<DemoItem>>
    fun getCategories(): List<DemoCategory>
}

class FeatureRegistryRepositoryImpl : FeatureRegistryRepository {

    private val realFeatureDemos = listOf(
        DemoItem(
            id = "lockscreen_demo",
            title = "Lockscreen Overlay Service",
            description = "Foreground overlay service listening for screen ON/OFF events with latency tracking & permissions.",
            category = DemoCategory.LOCATION_SENSORS,
            icon = Icons.Rounded.Lock,
            color = Color(0xFF2563EB),
            destination = "lockscreen_demo",
            tags = listOf("Lockscreen", "Overlay", "ForegroundService", "Permissions", "ScreenReceiver")
        ),
        DemoItem(
            id = "locktest_demo",
            title = "Lock Test & Keyguard Management",
            description = "App lock manager monitoring foreground apps with PIN overlay and usage stats permissions.",
            category = DemoCategory.SETTINGS_CONFIG,
            icon = Icons.Rounded.Key,
            color = Color(0xFF7C3AED),
            destination = "locktest_demo",
            tags = listOf("AppLock", "Keyguard", "Overlay", "UsageStats", "PIN")
        ),
        DemoItem(
            id = "voicespeech_demo",
            title = "Voice Speech Recognizer",
            description = "Dual-engine speech recognition and keyword spotting with real-time latency and accuracy metrics.",
            category = DemoCategory.UI_COMPONENTS,
            icon = Icons.Rounded.Mic,
            color = Color(0xFF059669),
            destination = "voicespeech_demo",
            tags = listOf("Speech", "Recognizer", "Voice", "SherpaOnnx", "Keyword")
        ),
        DemoItem(
            id = "cloudwhisperdemo_demo",
            title = "CloudWhisperDemo",
            description = "Groq Whisper Cloud Speech Recognition & Keyword Spotting.",
            category = DemoCategory.UI_COMPONENTS,
            icon = Icons.Rounded.Mic,
            color = Color(0xFFD97706),
            destination = "cloudwhisperdemo_demo",
            tags = listOf("Whisper", "Cloud", "Groq", "Speech", "Keyword")
        ),
        DemoItem(
            id = "androidnativespeechrecognizer_demo",
            title = "AndroidNativeSpeechRecognizer",
            description = "Android Native SpeechRecognizer API for Voice Commands.",
            category = DemoCategory.UI_COMPONENTS,
            icon = Icons.Rounded.Mic,
            color = Color(0xFF0284C7),
            destination = "androidnativespeechrecognizer_demo",
            tags = listOf("Native", "SpeechRecognizer", "Android", "Passphrase")
        ),
        DemoItem(
            id = "voskdemooo_demo",
            title = "VoskDemooo",
            description = "Vosk Offline Speech Recognition Engine for Multi-language Voice Control.",
            category = DemoCategory.UI_COMPONENTS,
            icon = Icons.Rounded.Mic,
            color = Color(0xFFDC2626),
            destination = "voskdemooo_demo",
            tags = listOf("Vosk", "Offline", "Speech", "Multi-language")
        ),
        DemoItem(
            id = "wakewordvoiceprint_demo",
            title = "WakewordVoiceprint",
            description = "Dual-layer real-time Keyword Spotting and Voiceprint verification engine.",
            category = DemoCategory.UI_COMPONENTS,
            icon = Icons.Rounded.RecordVoiceOver,
            color = Color(0xFF0D9488),
            destination = "wakewordvoiceprint_demo",
            tags = listOf("Wakeword", "Voiceprint", "CosineSimilarity", "MFCC", "Audio")
        ),
        DemoItem(
            id = "futovoiceinputengine_demo",
            title = "FUTOVoiceInputEngine",
            description = "FUTO Voice Input Engine integration for voice password recognition.",
            category = DemoCategory.UI_COMPONENTS,
            icon = Icons.Rounded.Mic,
            color = Color(0xFF4F46E5),
            destination = "futovoiceinputengine_demo",
            tags = listOf("FUTO", "VoiceInput", "SpeechRecognizer", "Password")
        ),
        DemoItem(
            id = "deepgramnova2apiwebsocket_demo",
            title = "DeepgramNova2APIWebSocket",
            description = "Real-time streaming speech recognition via Deepgram Nova-2 WebSocket API.",
            category = DemoCategory.UI_COMPONENTS,
            icon = Icons.Rounded.GraphicEq,
            color = Color(0xFF9333EA),
            destination = "deepgramnova2apiwebsocket_demo",
            tags = listOf("Deepgram", "Nova2", "WebSocket", "Realtime", "Streaming")
        )
    )

    private val demoItemsFlow = MutableStateFlow(realFeatureDemos)

    override fun getDemoItems(): Flow<List<DemoItem>> = demoItemsFlow.asStateFlow()

    override fun getCategories(): List<DemoCategory> = DemoCategory.entries
}
