package com.example.alldemo.features.demovoicespeechrecognizer.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alldemo.features.demovoicespeechrecognizer.engine.EngineType
import com.example.alldemo.features.demovoicespeechrecognizer.engine.RecognitionLanguage
import com.example.alldemo.features.demovoicespeechrecognizer.engine.RecognitionLog
import com.example.alldemo.features.demovoicespeechrecognizer.engine.RecognitionState
import com.example.alldemo.features.demovoicespeechrecognizer.viewmodel.VoiceRecognizerUiState
import com.example.alldemo.features.demovoicespeechrecognizer.viewmodel.VoiceRecognizerViewModel
import com.example.alldemo.ui.theme.AllDemoTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VoiceSpeechDemoScreen(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: VoiceRecognizerViewModel = viewModel(factory = VoiceRecognizerViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setPermissionGranted(isGranted)
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.setPermissionGranted(granted)
    }

    VoiceRecognizerContent(
        uiState = uiState,
        onEngineSelected = viewModel::selectEngine,
        onLanguageSelected = viewModel::selectLanguage,
        onKeywordChanged = viewModel::setTargetKeyword,
        onToggleListening = viewModel::toggleListening,
        onRequestPermission = {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        onClearHistory = viewModel::clearHistory,
        onClearError = viewModel::clearError,
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRecognizerContent(
    uiState: VoiceRecognizerUiState,
    onEngineSelected: (EngineType) -> Unit,
    onLanguageSelected: (RecognitionLanguage) -> Unit,
    onKeywordChanged: (String) -> Unit,
    onToggleListening: () -> Unit,
    onRequestPermission: () -> Unit,
    onClearHistory: () -> Unit,
    onClearError: () -> Unit,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Speech Recognizer & Keyword Spotter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Dual Engine Latency & Accuracy Tester",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            if (!uiState.hasRecordAudioPermission) {
                item {
                    PermissionBannerCard(onRequestPermission = onRequestPermission)
                }
            }

            uiState.errorMessage?.let { errorMsg ->
                item {
                    ErrorMessageCard(
                        errorMessage = errorMsg,
                        onDismiss = onClearError
                    )
                }
            }

            item {
                EngineSelectionCard(
                    selectedEngine = uiState.selectedEngine,
                    onEngineSelected = onEngineSelected
                )
            }

            item {
                LanguageSelectionCard(
                    selectedLanguage = uiState.selectedLanguage,
                    onLanguageSelected = onLanguageSelected
                )
            }

            item {
                KeywordConfigCard(
                    targetKeyword = uiState.targetKeyword,
                    onKeywordChanged = onKeywordChanged
                )
            }

            item {
                LiveRecognitionMatchCard(
                    uiState = uiState,
                    onToggleListening = onToggleListening,
                    onRequestPermission = onRequestPermission
                )
            }

            item {
                LatencyMetricsCard(uiState = uiState)
            }

            item {
                HistoryLogSection(
                    historyList = uiState.historyList,
                    onClearHistory = onClearHistory
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun PermissionBannerCard(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MicOff,
                contentDescription = "Microphone Permission Required",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Microphone Permission Required",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Grant audio recording access to enable real-time speech recognition.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Grant")
            }
        }
    }
}

@Composable
private fun ErrorMessageCard(
    errorMessage: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MismatchRedLight
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = MismatchRedDark,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MismatchRedDark,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Dismiss error",
                    tint = MismatchRedDark
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EngineSelectionCard(
    selectedEngine: EngineType,
    onEngineSelected: (EngineType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "1. Select Speech Recognition Engine",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            val engines = EngineType.entries.toTypedArray()
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                engines.forEachIndexed { index, engine ->
                    SegmentedButton(
                        selected = selectedEngine == engine,
                        onClick = { onEngineSelected(engine) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = engines.size)
                    ) {
                        Text(
                            text = if (engine == EngineType.ANDROID_NATIVE) "Android Native" else "Sherpa-onnx",
                            fontWeight = if (selectedEngine == engine) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Engine info",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = selectedEngine.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelectionCard(
    selectedLanguage: RecognitionLanguage,
    onLanguageSelected: (RecognitionLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "2. Select Recognition Language",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            val languages = RecognitionLanguage.entries.toTypedArray()
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                languages.forEachIndexed { index, language ->
                    SegmentedButton(
                        selected = selectedLanguage == language,
                        onClick = { onLanguageSelected(language) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = languages.size)
                    ) {
                        Text(
                            text = when (language) {
                                RecognitionLanguage.VIETNAMESE -> "Tiếng Việt"
                                RecognitionLanguage.ENGLISH -> "English"
                                RecognitionLanguage.AUTO -> "Auto"
                            },
                            fontWeight = if (selectedLanguage == language) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Language info",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${selectedLanguage.displayName} (${selectedLanguage.code}): ${selectedLanguage.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeywordConfigCard(
    targetKeyword: String,
    onKeywordChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf("Mở khóa", "Open sesame", "Xin chào", "assistant")

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "3. Target Keyword Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = targetKeyword,
                onValueChange = onKeywordChanged,
                label = { Text("Target Keyword to Spot") },
                placeholder = { Text("Enter keyword (e.g., Mở khóa)") },
                trailingIcon = {
                    if (targetKeyword.isNotEmpty()) {
                        IconButton(onClick = { onKeywordChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear keyword")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Quick Keyword Presets:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                presets.forEach { preset ->
                    val isSelected = targetKeyword.trim().equals(preset.trim(), ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onKeywordChanged(preset) },
                        label = { Text(preset) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveRecognitionMatchCard(
    uiState: VoiceRecognizerUiState,
    onToggleListening: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val hasText = uiState.currentText.isNotBlank() || uiState.partialText.isNotBlank()
    val isError = (uiState.recognitionState is RecognitionState.Error) || (uiState.errorMessage != null)

    val targetBgColor: Color = when {
        isError -> if (isDark) MismatchRedDark else MismatchRedLight
        uiState.isKeywordMatched && hasText -> if (isDark) MatchGreenDark else MatchGreenLight
        !uiState.isKeywordMatched && hasText -> if (isDark) MismatchRedDark else MismatchRedLight
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val targetContentColor: Color = when {
        isError -> if (isDark) Color.White else MismatchRedDark
        uiState.isKeywordMatched && hasText -> if (isDark) Color.White else MatchGreenDark
        !uiState.isKeywordMatched && hasText -> if (isDark) Color.White else MismatchRedDark
        else -> MaterialTheme.colorScheme.onSurface
    }

    val animatedBgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "MatchStatusBgColor"
    )

    val animatedContentColor by animateColorAsState(
        targetValue = targetContentColor,
        animationSpec = tween(durationMillis = 350),
        label = "MatchStatusContentColor"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = animatedBgColor),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (hasText || isError) 2.dp else 1.dp,
                color = when {
                    uiState.isKeywordMatched && hasText -> MatchGreenContainer
                    (!uiState.isKeywordMatched && hasText) || isError -> MismatchRedContainer
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusBadgeHeader(
                uiState = uiState,
                hasText = hasText,
                isError = isError
            )

            Spacer(modifier = Modifier.height(16.dp))

            RecognizedTextDisplay(
                uiState = uiState,
                contentColor = animatedContentColor
            )

            Spacer(modifier = Modifier.height(20.dp))

            RecordToggleButton(
                isListening = uiState.isListening,
                hasPermission = uiState.hasRecordAudioPermission,
                onToggleListening = onToggleListening,
                onRequestPermission = onRequestPermission
            )
        }
    }
}

@Composable
private fun StatusBadgeHeader(
    uiState: VoiceRecognizerUiState,
    hasText: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val (statusIcon, statusText, badgeBg) = when {
        isError -> Triple(
            Icons.Default.Error,
            "ERROR / FAILURE",
            MismatchRedContainer
        )
        uiState.isKeywordMatched && hasText -> Triple(
            Icons.Default.CheckCircle,
            "KEYWORD MATCHED! [SUCCESS]",
            MatchGreenContainer
        )
        !uiState.isKeywordMatched && hasText -> Triple(
            Icons.Default.Cancel,
            "KEYWORD MISMATCH [FAILURE]",
            MismatchRedContainer
        )
        uiState.isListening -> Triple(
            Icons.Default.GraphicEq,
            "LISTENING...",
            MaterialTheme.colorScheme.primaryContainer
        )
        else -> Triple(
            Icons.Default.RecordVoiceOver,
            "READY - IDLE",
            MaterialTheme.colorScheme.surfaceVariant
        )
    }

    Surface(
        color = badgeBg,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = statusText,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun RecognizedTextDisplay(
    uiState: VoiceRecognizerUiState,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val textToDisplay = when {
        uiState.currentText.isNotBlank() -> uiState.currentText
        uiState.partialText.isNotBlank() -> uiState.partialText
        uiState.isListening -> "Listening... Speak your target keyword now."
        else -> "Tap the microphone button below to start listening..."
    }

    val isPartial = uiState.currentText.isBlank() && uiState.partialText.isNotBlank()

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Recognized Text:",
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = textToDisplay,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                textAlign = TextAlign.Center
            )
            if (isPartial) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "(Recognizing...)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun RecordToggleButton(
    isListening: Boolean,
    hasPermission: Boolean,
    onToggleListening: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening) 1.15f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Button(
        onClick = {
            if (!hasPermission) {
                onRequestPermission()
            } else {
                onToggleListening()
            }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        ),
        shape = CircleShape,
        modifier = modifier
            .scale(scalePulse)
            .height(56.dp)
            .padding(horizontal = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isListening) "Stop Recording" else "Start Listening",
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isListening) "Stop Listening" else "Start Listening",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LatencyMetricsCard(
    uiState: VoiceRecognizerUiState,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Latency Metrics",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "5. Latency & Performance Metrics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "Last Latency",
                    value = "${uiState.lastLatencyMs} ms",
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "Min Latency",
                    value = "${uiState.minLatencyMs} ms",
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "Avg Latency",
                    value = "${uiState.avgLatencyMs} ms",
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "Max Latency",
                    value = "${uiState.maxLatencyMs} ms",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Recognitions: ${uiState.totalResultsCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Matched Keywords: ${uiState.matchedCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MatchGreenDark
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun HistoryLogSection(
    historyList: List<RecognitionLog>,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "6. Recognition History (${historyList.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (historyList.isNotEmpty()) {
                    IconButton(onClick = onClearHistory) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear History",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (historyList.isEmpty()) {
                Text(
                    text = "No speech recognition logs yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    historyList.take(10).forEach { log ->
                        HistoryLogItem(log = log, timeStr = dateFormat.format(Date(log.timestamp)))
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryLogItem(
    log: RecognitionLog,
    timeStr: String,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (log.engineType == EngineType.ANDROID_NATIVE) "Android" else "Sherpa",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = log.languageCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${log.latencyMs} ms",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = log.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color = if (log.isKeywordMatched) MatchGreenContainer else MismatchRedContainer,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = if (log.isKeywordMatched) "MATCH" else "NO MATCH",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VoiceSpeechDemoScreenPreview() {
    AllDemoTheme {
        VoiceRecognizerContent(
            uiState = VoiceRecognizerUiState(
                currentText = "Mở khóa thiết bị",
                targetKeyword = "Mở khóa",
                isKeywordMatched = true,
                selectedLanguage = RecognitionLanguage.VIETNAMESE,
                lastLatencyMs = 120,
                minLatencyMs = 95,
                avgLatencyMs = 110,
                maxLatencyMs = 140,
                totalResultsCount = 5,
                matchedCount = 4,
                hasRecordAudioPermission = true
            ),
            onEngineSelected = {},
            onLanguageSelected = {},
            onKeywordChanged = {},
            onToggleListening = {},
            onRequestPermission = {},
            onClearHistory = {},
            onClearError = {}
        )
    }
}
