package com.example.alldemo.features.speechbraintest.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.alldemo.features.speechbraintest.data.repository.SttRepository
import com.example.alldemo.features.speechbraintest.ui.audio.AudioRecorderViewModel
import com.example.alldemo.features.speechbraintest.ui.config.ConfigScreen
import com.example.alldemo.features.speechbraintest.ui.config.ConfigViewModel
import com.example.alldemo.features.speechbraintest.ui.history.TestLogHistoryScreen
import com.example.alldemo.features.speechbraintest.ui.history.TestLogHistoryViewModel
import com.example.alldemo.features.speechbraintest.ui.verification.VerificationScreen
import com.example.alldemo.features.speechbraintest.ui.verification.VerificationViewModel

data class NavItem(
    val screen: Screen,
    val title: String,
    val icon: ImageVector
)

private val NAV_ITEMS = listOf(
    NavItem(Screen.Verification, "Verify", Icons.Default.Lock),
    NavItem(Screen.Config, "Settings", Icons.Default.Settings),
    NavItem(Screen.History, "History", Icons.Default.History)
)

@Composable
fun SpeechBrainNavContent(
    sttRepository: SttRepository = remember { SttRepository() },
    verificationViewModel: VerificationViewModel = viewModel { VerificationViewModel(sttRepository) },
    recorderViewModel: AudioRecorderViewModel = viewModel(),
    configViewModel: ConfigViewModel = viewModel { ConfigViewModel(sttRepository) },
    historyViewModel: TestLogHistoryViewModel = viewModel { TestLogHistoryViewModel(sttRepository) }
) {
    val backStack = remember { mutableStateListOf<Screen>(Screen.Verification) }
    val currentScreen = backStack.lastOrNull() ?: Screen.Verification

    fun navigateTo(screen: Screen) {
        if (currentScreen != screen) {
            backStack.removeAll { it == screen }
            backStack.add(screen)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        if (isWideScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail {
                    Spacer(modifier = Modifier.height(16.dp))
                    NAV_ITEMS.forEach { item ->
                        val selected = currentScreen == item.screen
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTo(item.screen) },
                            icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                            label = { Text(item.title) }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavDisplayContent(
                        backStack = backStack,
                        verificationViewModel = verificationViewModel,
                        recorderViewModel = recorderViewModel,
                        configViewModel = configViewModel,
                        historyViewModel = historyViewModel,
                        onNavigateToConfig = { navigateTo(Screen.Config) }
                    )
                }
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NAV_ITEMS.forEach { item ->
                            val selected = currentScreen == item.screen
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(item.screen) },
                                icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                                label = { Text(item.title) }
                            )
                        }
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    NavDisplayContent(
                        backStack = backStack,
                        verificationViewModel = verificationViewModel,
                        recorderViewModel = recorderViewModel,
                        configViewModel = configViewModel,
                        historyViewModel = historyViewModel,
                        onNavigateToConfig = { navigateTo(Screen.Config) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavDisplayContent(
    backStack: List<Screen>,
    verificationViewModel: VerificationViewModel,
    recorderViewModel: AudioRecorderViewModel,
    configViewModel: ConfigViewModel,
    historyViewModel: TestLogHistoryViewModel,
    onNavigateToConfig: () -> Unit
) {
    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack is MutableList && backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
            }
        },
        entryProvider = entryProvider {
            entry(Screen.Verification) {
                VerificationScreen(
                    verificationViewModel = verificationViewModel,
                    recorderViewModel = recorderViewModel,
                    onNavigateToConfig = onNavigateToConfig
                )
            }
            entry(Screen.Config) {
                ConfigScreen(viewModel = configViewModel)
            }
            entry(Screen.History) {
                TestLogHistoryScreen(viewModel = historyViewModel)
            }
        }
    )
}
