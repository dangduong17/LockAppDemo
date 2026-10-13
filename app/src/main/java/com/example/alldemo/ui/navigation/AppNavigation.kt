package com.example.alldemo.ui.navigation

import android.app.Application
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alldemo.activity.SampleDemoActivity
import com.example.alldemo.features.androidnativespeechrecognizer.AndroidNativeSpeechRecognizerDemoScreen
import com.example.alldemo.features.cloudwhisperdemo.CloudWhisperDemoScreen
import com.example.alldemo.features.deepgramnova2apiwebsocket.ui.DeepgramNova2DemoScreen
import com.example.alldemo.features.demovoicespeechrecognizer.ui.VoiceSpeechDemoScreen
import com.example.alldemo.features.facerecognitionandroid.FaceRecognitionAndroidDemoScreen
import com.example.alldemo.features.futovoiceinputengine.VoicePasswordScreen
import com.example.alldemo.features.geminimultimodallivevoiceapi.GeminiMultimodalLiveVoiceDemoScreen
import com.example.alldemo.features.huggingfacetransformersjsonnxwebruntime.VoicePasswordViewModel
import com.example.alldemo.features.huggingfacetransformersjsonnxwebruntime.VoicePasswordViewModelFactory
import com.example.alldemo.features.huggingfacetransformersjsonnxwebruntime.ui.VoicePasswordScreen as HuggingFaceVoicePasswordScreen
import com.example.alldemo.features.lockscreentestapp.ui.LockscreenDemoScreen
import com.example.alldemo.features.locktestapp.ui.applist.LockTestDemoScreen
import com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.ui.RevAiAssemblyAiDemoScreen
import com.example.alldemo.features.speechbraintest.SpeechBrainTestDemoScreen
import com.example.alldemo.features.voskdemooo.VoskDemoScreen
import com.example.alldemo.features.wakewordvoiceprint.ui.navigation.WakewordVoiceprintMainScreen
import com.example.alldemo.ui.DemoLauncherViewModel
import com.example.alldemo.ui.dashboard.DashboardScreen

sealed class AppDestination(val route: String) {
    data object Dashboard : AppDestination("dashboard")
    data object Lockscreen : AppDestination("lockscreen_demo")
    data object LockTest : AppDestination("locktest_demo")
    data object VoiceSpeech : AppDestination("voicespeech_demo")
    data object CloudWhisper : AppDestination("cloudwhisperdemo_demo")
    data object AndroidNativeSpeechRecognizer : AppDestination("androidnativespeechrecognizer_demo")
    data object VoskDemooo : AppDestination("voskdemooo_demo")
    data object WakewordVoiceprint : AppDestination("wakewordvoiceprint_demo")
    data object FUTOVoiceInputEngine : AppDestination("futovoiceinputengine_demo")
    data object DeepgramNova2APIWebSocket : AppDestination("deepgramnova2apiwebsocket_demo")
    data object RevAiAssemblyAiStreamingWebSocket : AppDestination("revaiassemblyaistreamingwebsocketapi_demo")
    data object HuggingFaceTransformersjsONNXWebRuntime : AppDestination("huggingfacetransformersjsonnxwebruntime_demo")
    data object SpeechBrainTest : AppDestination("speechbraintest_demo")
    data object GeminiMultimodalLiveVoiceAPI : AppDestination("geminimultimodallivevoiceapi_demo")
    data object FaceRecognitionAndroid : AppDestination("facerecognitionandroid_demo")
}

@Composable
fun AppNavigation(
    viewModel: DemoLauncherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val backStack = remember { mutableStateListOf<AppDestination>(AppDestination.Dashboard) }
    val currentDestination = backStack.lastOrNull() ?: AppDestination.Dashboard

    BackHandler(enabled = backStack.size > 1) {
        backStack.removeAt(backStack.lastIndex)
    }

    AnimatedContent(
        targetState = currentDestination,
        transitionSpec = {
            if (targetState != AppDestination.Dashboard) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width / 3 } + fadeOut()
                )
            } else {
                (slideInHorizontally { width -> -width / 3 } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut()
                )
            }
        },
        label = "NavigationTransition",
        modifier = modifier
    ) { destination ->
        when (destination) {
            is AppDestination.Dashboard -> {
                DashboardScreen(
                    uiState = uiState,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onClearSearch = viewModel::clearSearchQuery,
                    onCategorySelected = viewModel::onCategorySelected,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onDemoItemClick = { item ->
                        if (item.isActivityDestination) {
                            val intent = Intent(context, SampleDemoActivity::class.java)
                            context.startActivity(intent)
                        } else {
                            val targetDestination = when (item.destination) {
                                AppDestination.Lockscreen.route -> AppDestination.Lockscreen
                                AppDestination.LockTest.route -> AppDestination.LockTest
                                AppDestination.VoiceSpeech.route -> AppDestination.VoiceSpeech
                                AppDestination.CloudWhisper.route -> AppDestination.CloudWhisper
                                AppDestination.AndroidNativeSpeechRecognizer.route -> AppDestination.AndroidNativeSpeechRecognizer
                                AppDestination.VoskDemooo.route -> AppDestination.VoskDemooo
                                AppDestination.WakewordVoiceprint.route -> AppDestination.WakewordVoiceprint
                                AppDestination.FUTOVoiceInputEngine.route -> AppDestination.FUTOVoiceInputEngine
                                AppDestination.DeepgramNova2APIWebSocket.route -> AppDestination.DeepgramNova2APIWebSocket
                                AppDestination.RevAiAssemblyAiStreamingWebSocket.route -> AppDestination.RevAiAssemblyAiStreamingWebSocket
                                AppDestination.HuggingFaceTransformersjsONNXWebRuntime.route -> AppDestination.HuggingFaceTransformersjsONNXWebRuntime
                                AppDestination.SpeechBrainTest.route -> AppDestination.SpeechBrainTest
                                AppDestination.GeminiMultimodalLiveVoiceAPI.route -> AppDestination.GeminiMultimodalLiveVoiceAPI
                                AppDestination.FaceRecognitionAndroid.route -> AppDestination.FaceRecognitionAndroid
                                else -> AppDestination.Dashboard
                            }
                            if (targetDestination != AppDestination.Dashboard) {
                                backStack.add(targetDestination)
                            }
                        }
                    }
                )
            }

            is AppDestination.Lockscreen -> {
                LockscreenDemoScreen(
                    onBackClick = {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                )
            }

            is AppDestination.LockTest -> {
                LockTestDemoScreen(
                    onBackClick = {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                )
            }

            is AppDestination.VoiceSpeech -> {
                VoiceSpeechDemoScreen(
                    onBackClick = {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                )
            }

            is AppDestination.CloudWhisper -> {
                CloudWhisperDemoScreen(
                    onBackClick = {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                )
            }

            is AppDestination.AndroidNativeSpeechRecognizer -> {
                AndroidNativeSpeechRecognizerDemoScreen(
                    onBackClick = {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                )
            }

            is AppDestination.VoskDemooo -> {
                VoskDemoScreen(
                    onBackClick = {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                )
            }

            is AppDestination.WakewordVoiceprint -> {
                WakewordVoiceprintMainScreen(
                    onBackClick = {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                )
            }

            is AppDestination.FUTOVoiceInputEngine -> {
                VoicePasswordScreen(
                    onBackClick = {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                )
            }

            is AppDestination.DeepgramNova2APIWebSocket -> {
                DeepgramNova2DemoScreen(
                    onBackClick = {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                )
            }

            is AppDestination.RevAiAssemblyAiStreamingWebSocket -> {
                RevAiAssemblyAiDemoScreen(
                    onBackClick = {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                )
            }

            is AppDestination.HuggingFaceTransformersjsONNXWebRuntime -> {
                val voicePasswordViewModel: VoicePasswordViewModel = viewModel(
                    factory = VoicePasswordViewModelFactory(LocalContext.current.applicationContext as Application)
                )
                HuggingFaceVoicePasswordScreen(
                    viewModel = voicePasswordViewModel,
                    onBackClick = {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                )
            }

            is AppDestination.SpeechBrainTest -> {
                SpeechBrainTestDemoScreen(
                    onBackClick = {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                )
            }

            is AppDestination.GeminiMultimodalLiveVoiceAPI -> {
                GeminiMultimodalLiveVoiceDemoScreen(
                    onBackClick = {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                )
            }

            is AppDestination.FaceRecognitionAndroid -> {
                FaceRecognitionAndroidDemoScreen(
                    onBackClick = {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                )
            }
        }
    }
}
