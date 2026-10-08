package com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.ui.settings.SettingsScreen
import com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.ui.unlock.UnlockScreen

private enum class RevAiRoute {
    UNLOCK,
    SETTINGS
}

@Composable
fun RevAiAssemblyAiDemoScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentRoute by remember { mutableStateOf(RevAiRoute.UNLOCK) }

    when (currentRoute) {
        RevAiRoute.UNLOCK -> {
            UnlockScreen(
                onNavigateToSettings = { currentRoute = RevAiRoute.SETTINGS },
                onBackClick = onBackClick,
                modifier = modifier
            )
        }
        RevAiRoute.SETTINGS -> {
            BackHandler {
                currentRoute = RevAiRoute.UNLOCK
            }
            SettingsScreen(
                onNavigateBack = { currentRoute = RevAiRoute.UNLOCK },
                modifier = modifier
            )
        }
    }
}
