package com.example.alldemo.features.speechbraintest.ui.verification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alldemo.features.speechbraintest.domain.MatchingStrategy
import com.example.alldemo.features.speechbraintest.ui.audio.AudioRecorderSection
import com.example.alldemo.features.speechbraintest.ui.audio.AudioRecorderViewModel
import com.example.alldemo.features.speechbraintest.ui.components.JsonViewerCard
import com.example.alldemo.features.speechbraintest.ui.components.MatchResultBannerCard

private val PRESET_PASSWORDS = listOf(
    "Open Sesame",
    "SpeechBrain 2026",
    "Security Voice Key",
    "Turn on the living room lights"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VerificationScreen(
    verificationViewModel: VerificationViewModel = viewModel(),
    recorderViewModel: AudioRecorderViewModel = viewModel(),
    onNavigateToConfig: () -> Unit = {}
) {
    val uiState by verificationViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Verification") },
                actions = {
                    IconButton(onClick = onNavigateToConfig) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuration Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Target Password Prompt",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.targetPassword,
                        onValueChange = { verificationViewModel.setTargetPassword(it) },
                        label = { Text("Target Password / Phrase") },
                        placeholder = { Text("e.g. Open Sesame") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null)
                        },
                        trailingIcon = {
                            if (uiState.targetPassword.isNotEmpty()) {
                                IconButton(onClick = { verificationViewModel.setTargetPassword("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Preset Target Password Samples:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PRESET_PASSWORDS.forEach { preset ->
                            val isSelected = uiState.targetPassword.equals(preset, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { verificationViewModel.setTargetPassword(preset) },
                                label = { Text(preset) },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Matching Strategy:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MatchingStrategy.entries.forEach { strategy ->
                            val isSelected = uiState.matchingStrategy == strategy
                            FilterChip(
                                selected = isSelected,
                                onClick = { verificationViewModel.setMatchingStrategy(strategy) },
                                label = { Text(strategy.displayName) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (uiState.matchingStrategy != MatchingStrategy.EXACT) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Threshold: ${(uiState.matchingThreshold * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Slider(
                            value = uiState.matchingThreshold,
                            onValueChange = { verificationViewModel.setMatchingThreshold(it) },
                            valueRange = 0.5f..1.0f,
                            steps = 9
                        )
                    }
                }
            }

            AudioRecorderSection(
                viewModel = recorderViewModel,
                onAudioCaptured = { payload ->
                    verificationViewModel.processRecordedAudio(payload)
                }
            )

            AnimatedVisibility(visible = uiState.isRecognizing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Analyzing speech with ${if (uiState.config.isMockMode) "Mock Engine" else "Google Cloud STT"}...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            uiState.errorMessage?.let { errorMsg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Recognition Error",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (uiState.lastMatchResult != null && uiState.lastResponse != null) {
                MatchResultBannerCard(
                    matchResult = uiState.lastMatchResult!!,
                    metrics = uiState.lastResponse!!.metrics
                )

                JsonViewerCard(
                    rawJson = uiState.lastResponse!!.result.rawJson,
                    title = "Raw JSON API Response (${uiState.lastResponse!!.metrics.rawJsonSizeBytes} B)",
                    initialExpanded = false
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { verificationViewModel.clearResults() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Clear Results")
                    }
                }
            }
        }
    }
}
