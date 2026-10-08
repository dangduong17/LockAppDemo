package com.example.alldemo.features.wakewordvoiceprint.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alldemo.features.wakewordvoiceprint.data.SettingsRepository
import com.example.alldemo.features.wakewordvoiceprint.data.VoiceprintRepository
import com.example.alldemo.features.wakewordvoiceprint.ui.enrollment.EnrollmentScreen
import com.example.alldemo.features.wakewordvoiceprint.ui.settings.SettingsDashboardScreen
import com.example.alldemo.features.wakewordvoiceprint.ui.settings.SettingsViewModel
import com.example.alldemo.features.wakewordvoiceprint.ui.settings.SettingsViewModelFactory
import com.example.alldemo.features.wakewordvoiceprint.ui.verification.VerificationScreen
import com.example.alldemo.features.wakewordvoiceprint.ui.verification.VerificationViewModel

enum class AppDestination(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    VERIFICATION("Mở khóa", Icons.Outlined.Lock, Icons.Rounded.Lock),
    ENROLLMENT("Đăng ký", Icons.Outlined.Fingerprint, Icons.Rounded.Fingerprint),
    SETTINGS("Cài đặt", Icons.Outlined.Settings, Icons.Rounded.Settings)
}

@Composable
fun WakewordVoiceprintMainScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext

    val voiceprintRepository = remember { VoiceprintRepository(context) }
    val settingsRepository = remember { SettingsRepository(context) }

    var currentDestination by remember { mutableStateOf(AppDestination.VERIFICATION) }

    val verificationViewModel: VerificationViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return VerificationViewModel(
                    voiceprintRepository = voiceprintRepository,
                    settingsRepository = settingsRepository
                ) as T
            }
        }
    )

    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(context)
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0.dp),
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar {
                        AppDestination.entries.forEach { destination ->
                            val isSelected = currentDestination == destination
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentDestination = destination },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) destination.selectedIcon else destination.icon,
                                        contentDescription = destination.label
                                    )
                                },
                                label = { Text(destination.label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail {
                        AppDestination.entries.forEach { destination ->
                            val isSelected = currentDestination == destination
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { currentDestination = destination },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) destination.selectedIcon else destination.icon,
                                        contentDescription = destination.label
                                    )
                                },
                                label = { Text(destination.label) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = currentDestination,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "screen_transition"
                    ) { destination ->
                        when (destination) {
                            AppDestination.VERIFICATION -> {
                                VerificationScreen(
                                    viewModel = verificationViewModel,
                                    onNavigateToEnrollment = {
                                        currentDestination = AppDestination.ENROLLMENT
                                    },
                                    onBackClick = onBackClick
                                )
                            }
                            AppDestination.ENROLLMENT -> {
                                EnrollmentScreen(
                                    onNavigateBack = {
                                        currentDestination = AppDestination.VERIFICATION
                                    },
                                    onNavigateToTest = {
                                        currentDestination = AppDestination.SETTINGS
                                    }
                                )
                            }
                            AppDestination.SETTINGS -> {
                                SettingsDashboardScreen(
                                    viewModel = settingsViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
