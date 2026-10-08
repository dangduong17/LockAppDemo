package com.example.alldemo.features.futovoiceinputengine

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private const val SpeechRecognizerErrorPermissions = android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS

@Composable
fun VoicePasswordScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: VoicePasswordViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (uiState.targetPassword.isEmpty()) {
            viewModel.updateTargetPassword("mở khóa")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening(context)
        } else {
            viewModel.processError(SpeechRecognizerErrorPermissions)
        }
    }

    val onSpeakClick = {
        val permissionCheck = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        )
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            if (uiState.isListening) {
                viewModel.stopListening()
            } else {
                viewModel.startListening(context)
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    VoicePasswordContent(
        uiState = uiState,
        onTargetPasswordChange = viewModel::updateTargetPassword,
        onSpeakClick = onSpeakClick,
        onResetClick = viewModel::resetRecognition,
        onClearError = viewModel::clearError,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoicePasswordContent(
    uiState: VoicePasswordUiState,
    onTargetPasswordChange: (String) -> Unit,
    onSpeakClick: () -> Unit,
    onResetClick: () -> Unit,
    onClearError: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val targetBackgroundColor = if (uiState.isMatched) {
        Color(0xFF4CAF50)
    } else {
        MaterialTheme.colorScheme.background
    }

    val backgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(durationMillis = 600),
        label = "BackgroundColorAnimation",
    )

    val contentColor = if (uiState.isMatched) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onBackground
    }

    Scaffold(
        modifier = modifier,
        containerColor = backgroundColor,
        contentColor = contentColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Demo Mật Khẩu Giọng Nói",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                        Text(
                            text = "FUTO Voice Input Engine",
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Trở về",
                                tint = contentColor
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (uiState.activeEngine.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.isMatched) {
                            Color.White.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Engine: ",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = contentColor.copy(alpha = 0.8f)
                        )
                        Text(
                            text = uiState.activeEngine,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isMatched) {
                        Color.White.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Mật khẩu mục tiêu (Target Password):",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.targetPassword,
                        onValueChange = onTargetPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Nhập mật khẩu (ví dụ: mở khóa)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Lock Icon",
                                tint = contentColor
                            )
                        },
                        trailingIcon = {
                            if (uiState.targetPassword.isNotEmpty()) {
                                IconButton(onClick = { onTargetPasswordChange("") }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Clear,
                                        contentDescription = "Clear",
                                        tint = contentColor
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = contentColor,
                            unfocusedTextColor = contentColor,
                            focusedBorderColor = if (uiState.isMatched) Color.White else MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = contentColor.copy(alpha = 0.5f),
                            focusedLabelColor = contentColor,
                            unfocusedLabelColor = contentColor.copy(alpha = 0.7f),
                            cursorColor = contentColor
                        )
                    )
                }
            }

            if (uiState.isListening) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.25f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 700),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .scale(pulseScale)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                shape = CircleShape
                            )
                    )
                    CircularProgressIndicator(
                        modifier = Modifier.size(70.dp),
                        color = if (uiState.isMatched) Color.White else MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = "Listening",
                        modifier = Modifier.size(36.dp),
                        tint = contentColor
                    )
                }

                Text(
                    text = if (uiState.partialResults.isNotEmpty()) {
                        "Đang nghe: \"${uiState.partialResults}\""
                    } else {
                        "Đang lắng nghe giọng nói..."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = contentColor,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = onSpeakClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isMatched) {
                        Color.White
                    } else if (uiState.isListening) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (uiState.isMatched) {
                        Color(0xFF2E7D32)
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    }
                )
            ) {
                Icon(
                    imageVector = if (uiState.isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.isListening) "Dừng lắng nghe" else "Nói mật khẩu",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        uiState.isMatched -> Color.White.copy(alpha = 0.2f)
                        uiState.recognizedText.isNotEmpty() -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "KẾT QUẢ NHẬN DIỆN",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor.copy(alpha = 0.8f)
                    )

                    val recognizedTextDisplay = if (uiState.recognizedText.isNotEmpty()) {
                        "\"${uiState.recognizedText}\""
                    } else {
                        "(Chưa có dữ liệu giọng nói)"
                    }

                    Text(
                        text = recognizedTextDisplay,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        textAlign = TextAlign.Center
                    )

                    val errorMessage = uiState.errorMessage
                    when {
                        uiState.isMatched -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Matched",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "MẬT KHẨU CHÍNH XÁC!\nĐã mở khóa thành công.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        uiState.recognizedText.isNotEmpty() && !uiState.isMatched -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = "Mismatch",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "MẬT KHẨU KHÔNG CHÍNH XÁC\nVui lòng thử lại.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        errorMessage != null -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                IconButton(onClick = onClearError) {
                                    Icon(
                                        imageVector = Icons.Rounded.Clear,
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        else -> {
                            Text(
                                text = "Nhấn nút \"Nói mật khẩu\" và nói từ khóa tương ứng để kiểm tra.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = contentColor.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            if ((uiState.recognizedText.isNotEmpty()) || (uiState.errorMessage != null)) {
                OutlinedButton(
                    onClick = onResetClick,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = contentColor
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Reset",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Đặt lại (Reset)")
                }
            }
        }
    }
}
