package com.example.alldemo.features.huggingfacetransformersjsonnxwebruntime.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alldemo.features.huggingfacetransformersjsonnxwebruntime.AuthStatus
import com.example.alldemo.features.huggingfacetransformersjsonnxwebruntime.RecordingState
import com.example.alldemo.features.huggingfacetransformersjsonnxwebruntime.SpeechRecognitionWebView
import com.example.alldemo.features.huggingfacetransformersjsonnxwebruntime.VoicePasswordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoicePasswordScreen(
    viewModel: VoicePasswordViewModel,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val targetPassword by viewModel.targetPassword.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()
    val webModelStatus by viewModel.webModelStatus.collectAsStateWithLifecycle()
    val recognizedTranscript by viewModel.recognizedTranscript.collectAsStateWithLifecycle()
    val authStatus by viewModel.authStatus.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording()
        } else {
            Toast.makeText(
                context,
                "Cần cấp quyền RECORD_AUDIO để sử dụng tính năng nhận diện giọng nói.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun handleMicClick() {
        when (recordingState) {
            RecordingState.IDLE -> {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    viewModel.startRecording()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
            RecordingState.RECORDING -> {
                viewModel.stopRecordingAndProcess()
            }
            RecordingState.PROCESSING -> {
                // Currently processing audio
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.RecordVoiceOver,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Hugging Face ONNX Web Runtime",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Quay lại"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Hidden WebView hosting Transformers.js / ONNX Web Runtime
            SpeechRecognitionWebView(
                webViewEngine = viewModel.webViewEngine,
                modifier = Modifier
                    .size(1.dp)
                    .graphicsLayer(alpha = 0f)
            )

            if (authStatus == AuthStatus.SUCCESS) {
                // Success screen layout
                SuccessResultScreen(
                    targetPassword = targetPassword,
                    recognizedTranscript = recognizedTranscript,
                    onReset = { viewModel.resetAuthStatus() }
                )
            } else {
                // Main screen layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Web Model Status Card
                    ModelStatusCard(webModelStatus = webModelStatus)

                    // Target Password Input Card
                    TargetPasswordCard(
                        targetPassword = targetPassword,
                        onPasswordChanged = { viewModel.setTargetPassword(it) },
                        selectedLanguage = selectedLanguage,
                        onLanguageSelected = { viewModel.setSelectedLanguage(it) }
                    )

                    // Error message banner if any
                    errorMessage?.let { error ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Mismatch card alert if authStatus is MISMATCH
                    AnimatedVisibility(
                        visible = authStatus == AuthStatus.MISMATCH,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        MismatchResultCard(
                            targetPassword = targetPassword,
                            recognizedTranscript = recognizedTranscript,
                            onRetry = { viewModel.resetAuthStatus() }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Speech Recording Section
                    RecordingSection(
                        recordingState = recordingState,
                        recognizedTranscript = recognizedTranscript,
                        onMicClick = { handleMicClick() }
                    )
                }
            }
        }
    }
}

@Composable
fun ModelStatusCard(webModelStatus: String) {
    val isReady = webModelStatus.contains("sẵn sàng", ignoreCase = true)
    val statusBgColor by animateColorAsState(
        targetValue = if (isReady) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant,
        label = "statusBgColor"
    )
    val statusTextColor = if (isReady) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(statusBgColor)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isReady) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Trạng thái Mô hình AI",
                    fontSize = 12.sp,
                    color = statusTextColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = webModelStatus,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusTextColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetPasswordCard(
    targetPassword: String,
    onPasswordChanged: (String) -> Unit,
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.VpnKey,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mật Khẩu Mục Tiêu",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = targetPassword,
                onValueChange = onPasswordChanged,
                label = { Text("Nhập mật khẩu cần nói") },
                placeholder = { Text("e.g. mở khóa") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Gợi ý mật khẩu mẫu:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("mở khóa", "open sesame", "xin chào").forEach { preset ->
                    AssistChip(
                        onClick = { onPasswordChanged(preset) },
                        label = { Text(preset) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Ngôn ngữ nhận diện:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                val languages = listOf(
                    "auto" to "Tự động (Auto)",
                    "vietnamese" to "Tiếng Việt",
                    "english" to "Tiếng Anh"
                )
                languages.forEachIndexed { index, (code, label) ->
                    SegmentedButton(
                        selected = selectedLanguage == code,
                        onClick = { onLanguageSelected(code) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = languages.size)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecordingSection(
    recordingState: RecordingState,
    recognizedTranscript: String,
    onMicClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val buttonColor = when (recordingState) {
        RecordingState.IDLE -> MaterialTheme.colorScheme.primary
        RecordingState.RECORDING -> Color(0xFFD32F2F)
        RecordingState.PROCESSING -> MaterialTheme.colorScheme.secondary
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = when (recordingState) {
                RecordingState.IDLE -> "Nhấn nút bên dưới và nói mật khẩu"
                RecordingState.RECORDING -> "Đang thu âm... Nhấn để dừng & phân tích"
                RecordingState.PROCESSING -> "Đang xử lý giọng nói với Whisper AI..."
            },
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp)
        ) {
            if (recordingState == RecordingState.RECORDING) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color(0xFFEF5350).copy(alpha = 0.3f))
                )
            }

            Surface(
                onClick = onMicClick,
                shape = CircleShape,
                color = buttonColor,
                shadowElevation = 8.dp,
                modifier = Modifier.size(80.dp),
                enabled = recordingState != RecordingState.PROCESSING
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when (recordingState) {
                        RecordingState.IDLE -> {
                            Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = "Nói mật khẩu",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        RecordingState.RECORDING -> {
                            Icon(
                                imageVector = Icons.Rounded.Stop,
                                contentDescription = "Dừng ghi âm",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        RecordingState.PROCESSING -> {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onMicClick,
            enabled = recordingState != RecordingState.PROCESSING,
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(
                imageVector = if (recordingState == RecordingState.RECORDING) Icons.Rounded.Stop else Icons.Rounded.Mic,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (recordingState) {
                    RecordingState.IDLE -> "Nói Mật Khẩu"
                    RecordingState.RECORDING -> "Dừng & Phân Tích"
                    RecordingState.PROCESSING -> "Đang Phân Tích..."
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (recognizedTranscript.isNotBlank()) {
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Văn bản nhận diện được:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"$recognizedTranscript\"",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun MismatchResultCard(
    targetPassword: String,
    recognizedTranscript: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = Color(0xFFC62828),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mật khẩu chưa đúng. Vui lòng thử lại!",
                    color = Color(0xFFC62828),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "• Mật khẩu mục tiêu: \"$targetPassword\"",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Đã nghe: \"$recognizedTranscript\"",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFB71C1C)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Thử lại")
            }
        }
    }
}

@Composable
fun SuccessResultScreen(
    targetPassword: String,
    recognizedTranscript: String,
    onReset: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8F5E9))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LockOpen,
                        contentDescription = "Thành công",
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Mở khóa thành công!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2E7D32),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF1F8E9)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Mật khẩu mục tiêu: \"$targetPassword\"",
                            fontSize = 14.sp,
                            color = Color(0xFF33691E)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Giọng nói nhận diện: \"$recognizedTranscript\"",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onReset,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thử Lại / Khóa Lại",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
