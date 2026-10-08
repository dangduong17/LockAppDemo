package com.example.alldemo.features.wakewordvoiceprint.ui.enrollment

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Top-level Enrollment Screen Composable for Voiceprint Registration.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun EnrollmentScreen(
    modifier: Modifier = Modifier,
    viewModel: EnrollmentViewModel = viewModel(
        factory = EnrollmentViewModelFactory(LocalContext.current)
    ),
    onNavigateBack: () -> Unit = {},
    onNavigateToTest: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val amplitude by viewModel.amplitude.collectAsStateWithLifecycle()

    val permissionState = rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO)

    // Reset recording state if permission is revoked
    LaunchedEffect(permissionState.status.isGranted) {
        if (!permissionState.status.isGranted && uiState.status is EnrollmentStatus.Recording) {
            viewModel.resetEnrollment()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Đăng ký Vân giọng",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Trở về"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            if (!permissionState.status.isGranted) {
                MicrophonePermissionRequestContent(
                    onRequestPermission = { permissionState.launchPermissionRequest() }
                )
            } else {
                EnrollmentContent(
                    uiState = uiState,
                    amplitude = amplitude,
                    onStartRecording = {
                        if (permissionState.status.isGranted) {
                            viewModel.startRecording()
                        } else {
                            permissionState.launchPermissionRequest()
                        }
                    },
                    onStopRecording = { viewModel.stopRecording() },
                    onFinishEarly = { viewModel.finishEarlyAndSave() },
                    onReset = { viewModel.resetEnrollment() },
                    onSetTotalRequired = { count -> viewModel.setTotalRequiredSamples(count) },
                    onNavigateToTest = onNavigateToTest,
                    onNavigateBack = onNavigateBack
                )
            }
        }
    }
}

@Composable
private fun MicrophonePermissionRequestContent(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Yêu cầu quyền Microphone",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Ứng dụng cần quyền ghi âm Micro để thu thập các mẫu vân giọng của bạn. Dữ liệu âm thanh chỉ được xử lý cục bộ trên thiết bị.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(imageVector = Icons.Rounded.Mic, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cấp quyền Microphone", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EnrollmentContent(
    uiState: EnrollmentUiState,
    amplitude: Float,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onFinishEarly: () -> Unit,
    onReset: () -> Unit,
    onSetTotalRequired: (Int) -> Unit,
    onNavigateToTest: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val isRecording = uiState.status is EnrollmentStatus.Recording
    val isProcessing = uiState.status is EnrollmentStatus.Processing
    val successStatus = uiState.status as? EnrollmentStatus.Success

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Tạo chữ ký âm thanh từ khóa",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Hãy phát âm từ khóa chính chủ (ví dụ: 'Xin chào Galaxy') rõ ràng 3 - 5 lần để hệ thống tạo vân giọng.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (uiState.recordedSamplesCount == 0 && uiState.status is EnrollmentStatus.Idle) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Số lượng mẫu:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    for (count in 3..5) {
                        FilterChip(
                            selected = uiState.totalRequiredSamples == count,
                            onClick = { onSetTotalRequired(count) },
                            label = { Text("$count Mẫu") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            StepProgressIndicator(
                totalSteps = uiState.totalRequiredSamples,
                recordedCount = uiState.recordedSamplesCount,
                currentStep = uiState.currentSampleIndex,
                isRecording = isRecording
            )

            Spacer(modifier = Modifier.height(24.dp))

            AudioWaveformVisualizer(
                amplitude = amplitude,
                isRecording = isRecording,
                visualizerHeight = 130.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            EnrollmentStatusBadge(uiState = uiState)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (successStatus != null) {
                SuccessEnrollmentCard(
                    successState = successStatus,
                    onNavigateToTest = onNavigateToTest,
                    onNavigateBack = onNavigateBack,
                    onReEnroll = onReset
                )
            } else {
                PulsingRecordButton(
                    isRecording = isRecording,
                    isProcessing = isProcessing,
                    onStartRecording = onStartRecording,
                    onStopRecording = onStopRecording
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.recordedSamplesCount in 3 until uiState.totalRequiredSamples && uiState.status is EnrollmentStatus.Idle) {
                    OutlinedButton(
                        onClick = onFinishEarly,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hoàn tất sớm với ${uiState.recordedSamplesCount} mẫu")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (uiState.recordedSamplesCount > 0 && uiState.status is EnrollmentStatus.Idle) {
                    TextButton(onClick = onReset) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bắt đầu lại từ đầu")
                    }
                }
            }
        }
    }
}

@Composable
private fun StepProgressIndicator(
    totalSteps: Int,
    recordedCount: Int,
    currentStep: Int,
    isRecording: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Tiến trình thu âm: Mẫu $currentStep / $totalSteps",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (step in 1..totalSteps) {
                val isCompleted = step <= recordedCount
                val isActive = step == currentStep && !isCompleted

                val stepColor = when {
                    isCompleted -> MaterialTheme.colorScheme.primary
                    isActive -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.outlineVariant
                }

                Box(
                    modifier = Modifier
                        .size(if (isActive) 38.dp else 32.dp)
                        .scale(if (isActive && isRecording) 1.1f else 1.0f)
                        .clip(CircleShape)
                        .background(
                            if (isCompleted) stepColor else Color.Transparent
                        )
                        .border(
                            width = if (isActive) 3.dp else 1.5.dp,
                            color = stepColor,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Mẫu $step hoàn tất",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "$step",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnrollmentStatusBadge(uiState: EnrollmentUiState) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (uiState.status) {
                is EnrollmentStatus.Recording -> MaterialTheme.colorScheme.errorContainer
                is EnrollmentStatus.Processing -> MaterialTheme.colorScheme.secondaryContainer
                is EnrollmentStatus.Error -> MaterialTheme.colorScheme.errorContainer
                is EnrollmentStatus.Success -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val (statusText, statusColor) = when (val status = uiState.status) {
                is EnrollmentStatus.Idle -> {
                    "Sẵn sàng. Nhấn nút Micro bên dưới và nói từ khóa." to MaterialTheme.colorScheme.onSurfaceVariant
                }

                is EnrollmentStatus.Recording -> {
                    "🔴 Đang ghi âm Mẫu ${status.sampleIndex}... Nhấn Dừng khi nói xong." to MaterialTheme.colorScheme.onErrorContainer
                }

                is EnrollmentStatus.Processing -> {
                    "⚙️ Đang phân tích trích xuất vector đặc trưng MFCC..." to MaterialTheme.colorScheme.onSecondaryContainer
                }

                is EnrollmentStatus.Error -> {
                    "⚠️ ${status.message}" to MaterialTheme.colorScheme.onErrorContainer
                }

                is EnrollmentStatus.Success -> {
                    "✅ Đăng ký hoàn tất thành công!" to MaterialTheme.colorScheme.onPrimaryContainer
                }
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = statusColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PulsingRecordButton(
    isRecording: Boolean,
    isProcessing: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp)
        ) {
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.error.copy(alpha = pulseAlpha)
                        )
                )
            }

            Surface(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !isProcessing) {
                        if (isRecording) onStopRecording() else onStartRecording()
                    },
                shape = CircleShape,
                color = when {
                    isRecording -> MaterialTheme.colorScheme.error
                    isProcessing -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.primary
                },
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Dừng thu âm" else "Bắt đầu thu âm",
                            modifier = Modifier.size(42.dp),
                            tint = if (isRecording) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when {
                isRecording -> "Nhấn để dừng mẫu này"
                isProcessing -> "Đang xử lý..."
                else -> "Nhấn để bắt đầu thu âm"
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SuccessEnrollmentCard(
    successState: EnrollmentStatus.Success,
    onNavigateToTest: () -> Unit,
    onNavigateBack: () -> Unit,
    onReEnroll: () -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Đăng ký Vân giọng Thành công!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Đã tính toán và lưu trữ thành công Vector Vân giọng trung bình (26 chiều) từ ${successState.sampleCount} mẫu âm thanh từ khóa.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onNavigateToTest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.GraphicEq, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Thử nghiệm ngay", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Trở về Màn hình chính", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onReEnroll) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Đăng ký lại vân giọng mới")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EnrollmentScreenPreview() {
    MaterialTheme {
        EnrollmentContent(
            uiState = EnrollmentUiState(
                currentSampleIndex = 2,
                totalRequiredSamples = 3,
                recordedSamplesCount = 1
            ),
            amplitude = 0.45f,
            onStartRecording = {},
            onStopRecording = {},
            onFinishEarly = {},
            onReset = {},
            onSetTotalRequired = {},
            onNavigateToTest = {},
            onNavigateBack = {}
        )
    }
}
