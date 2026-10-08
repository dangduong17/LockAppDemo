package com.example.alldemo.features.voskdemooo

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoskDemoScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val voskManager = remember(context) {
        VoskSpeechManager(context).apply {
            initModel("model_vi")
        }
    }

    DisposableEffect(voskManager) {
        onDispose {
            voskManager.destroy()
        }
    }

    var selectedLanguage by remember { mutableStateOf("vi") }
    var targetKeyword by remember { mutableStateOf("mở khóa") }
    val recognizedText by voskManager.recognizedText.collectAsStateWithLifecycle()

    val parsedText = remember(recognizedText) {
        parseVoskResult(recognizedText)
    }

    val trimmedKeyword = targetKeyword.trim()
    val trimmedText = parsedText.trim()
    val isMatch = trimmedText.isNotBlank() &&
            trimmedKeyword.isNotBlank() &&
            (trimmedText.equals(trimmedKeyword, ignoreCase = true) ||
                    trimmedText.contains(trimmedKeyword, ignoreCase = true))

    val backgroundColor = if (trimmedText.isEmpty()) {
        MaterialTheme.colorScheme.background
    } else if (isMatch) {
        Color(0xFF4CAF50) // Green on match
    } else {
        Color(0xFFE53935) // Red on mismatch
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Vosk Offline Speech") },
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
        containerColor = backgroundColor
    ) { innerPadding ->
        MainScreenContent(
            modifier = Modifier.padding(innerPadding),
            voskManager = voskManager,
            parsedText = parsedText,
            targetKeyword = targetKeyword,
            onTargetKeywordChange = { targetKeyword = it },
            selectedLanguage = selectedLanguage,
            onLanguageChange = { lang ->
                selectedLanguage = lang
                if (lang == "vi") {
                    targetKeyword = "mở khóa"
                    voskManager.initModel("model_vi")
                } else {
                    targetKeyword = "unlock"
                    voskManager.initModel("model_en")
                }
            },
            isMatch = isMatch
        )
    }
}

private fun parseVoskResult(rawText: String): String {
    if (rawText.isBlank()) return ""
    return try {
        val jsonObject = JSONObject(rawText)
        if (jsonObject.has("text")) {
            jsonObject.getString("text")
        } else if (jsonObject.has("partial")) {
            jsonObject.getString("partial")
        } else {
            rawText
        }
    } catch (_: Exception) {
        rawText
    }
}

@Composable
fun MainScreenContent(
    modifier: Modifier = Modifier,
    voskManager: VoskSpeechManager? = null,
    parsedText: String = "",
    targetKeyword: String = "mở khóa",
    onTargetKeywordChange: (String) -> Unit = {},
    selectedLanguage: String = "vi",
    onLanguageChange: (String) -> Unit = {},
    isMatch: Boolean = false
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val isReady by voskManager?.isReady?.collectAsStateWithLifecycle(initialValue = false) ?: remember { mutableStateOf(false) }
    val isListening by voskManager?.isListening?.collectAsStateWithLifecycle(initialValue = false) ?: remember { mutableStateOf(false) }
    val errorMessage by voskManager?.errorMessage?.collectAsStateWithLifecycle(initialValue = null) ?: remember { mutableStateOf<String?>(null) }

    val langLabel = if (selectedLanguage == "vi") "Tiếng Việt" else "English"
    val statusText = when {
        errorMessage != null -> errorMessage ?: "Lỗi"
        !isReady -> "Đang tải mô hình ($langLabel)..."
        isListening -> "Đang lắng nghe ($langLabel)..."
        else -> "Sẵn sàng - Nhấn Bắt đầu"
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!hasPermission) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MicOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Yêu cầu quyền truy cập Micro",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Ứng dụng cần quyền ghi âm để nhận diện giọng nói của bạn.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Rounded.Mic, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cấp quyền")
                        }
                    }
                }
            } else {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            text = "Vosk Speech Recognition",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Language Selector (FilterChips for Tiếng Việt & English)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                        ) {
                            FilterChip(
                                selected = selectedLanguage == "vi",
                                onClick = { onLanguageChange("vi") },
                                label = { Text("Tiếng Việt") },
                                leadingIcon = if (selectedLanguage == "vi") {
                                    { Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                            FilterChip(
                                selected = selectedLanguage == "en",
                                onClick = { onLanguageChange("en") },
                                label = { Text("English") },
                                leadingIcon = if (selectedLanguage == "en") {
                                    { Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }

                        // OutlinedTextField for custom target keyword
                        OutlinedTextField(
                            value = targetKeyword,
                            onValueChange = onTargetKeywordChange,
                            label = { Text(if (selectedLanguage == "vi") "Từ khóa mục tiêu" else "Target Keyword") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Instruction Banner
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (selectedLanguage == "vi") "Hãy nói: '$targetKeyword'" else "Say: '$targetKeyword'",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Status Display
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val statusIcon = when {
                                errorMessage != null -> Icons.Rounded.Warning
                                !isReady -> Icons.Rounded.Refresh
                                isListening -> Icons.Rounded.Mic
                                else -> Icons.Rounded.CheckCircle
                            }
                            val iconTint = when {
                                errorMessage != null -> MaterialTheme.colorScheme.error
                                !isReady -> MaterialTheme.colorScheme.secondary
                                isListening -> Color(0xFF4CAF50)
                                else -> MaterialTheme.colorScheme.primary
                            }
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Real-time Recognized Speech Display
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (selectedLanguage == "vi") "Văn bản nhận diện:" else "Recognized Text:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = if (parsedText.isBlank()) (if (selectedLanguage == "vi") "(Chưa có giọng nói... Hãy nói gì đó)" else "(No speech yet... Say something)") else parsedText,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (parsedText.isBlank()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                                )
                                if (parsedText.isNotBlank()) {
                                    Text(
                                        text = if (isMatch) (if (selectedLanguage == "vi") "✓ Khớp lệnh! ('$targetKeyword')" else "✓ Match! ('$targetKeyword')") else (if (selectedLanguage == "vi") "✗ Chưa khớp lệnh" else "✗ Mismatch"),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isMatch) Color(0xFF2E7D32) else Color(0xFFC62828),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Control Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                        ) {
                            Button(
                                onClick = { voskManager?.startListening() },
                                enabled = isReady && !isListening,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (selectedLanguage == "vi") "Bắt đầu" else "Start")
                            }

                            OutlinedButton(
                                onClick = { voskManager?.stopListening() },
                                enabled = isReady && isListening,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(Icons.Rounded.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (selectedLanguage == "vi") "Dừng" else "Stop")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VoskDemoScreenPreview() {
    MaterialTheme {
        MainScreenContent(
            parsedText = "mở khóa",
            targetKeyword = "mở khóa",
            selectedLanguage = "vi",
            isMatch = true
        )
    }
}
