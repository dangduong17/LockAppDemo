package com.example.alldemo.features.wakewordvoiceprint.ui.verification

import android.Manifest
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

import com.example.alldemo.features.wakewordvoiceprint.ui.enrollment.AudioWaveformVisualizer
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlin.math.roundToInt

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VerificationScreen(
    viewModel: VerificationViewModel,
    onNavigateToEnrollment: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val micPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(micPermissionState.status.isGranted) {
        if (!micPermissionState.status.isGranted && uiState.isListening) {
            viewModel.stopListening()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Xác Thực Vân Giọng",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Real-time Hybrid Engine (2 Tầng)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Trở về"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToEnrollment) {
                        Icon(
                            imageVector = Icons.Rounded.PersonAdd,
                            contentDescription = "Đăng ký vân giọng",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        VerificationContent(
            uiState = uiState,
            onToggleListening = {
                if (micPermissionState.status.isGranted) {
                    viewModel.toggleListening()
                } else {
                    micPermissionState.launchPermissionRequest()
                }
            },
            onResetVerification = { viewModel.resetVerification() },
            onNavigateToEnrollment = onNavigateToEnrollment,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

@Composable
fun VerificationContent(
    uiState: VerificationUiState,
    onToggleListening: () -> Unit,
    onResetVerification: () -> Unit,
    onNavigateToEnrollment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(uiState.result) {
        if ((uiState.result is VerificationResult.DeniedVoiceprint) ||
            (uiState.result is VerificationResult.DeniedKeyword)
        ) {
            try {
                val shakeSequence = listOf(-25f, 25f, -18f, 18f, -10f, 10f, -5f, 5f, 0f)
                for (offset in shakeSequence) {
                    shakeOffset.animateTo(
                        targetValue = offset,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessHigh
                        )
                    )
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenLockSimulationCard(
            isUnlocked = uiState.isUnlocked,
            result = uiState.result,
            hasOwnerProfile = uiState.hasOwnerProfile,
            wakeword = uiState.selectedWakeword,
            shakeOffsetX = shakeOffset.value,
            onResetLock = onResetVerification,
            onNavigateToEnrollment = onNavigateToEnrollment
        )

        TwoStagePipelineIndicator(
            pipelineStage = uiState.pipelineStage,
            result = uiState.result,
            kwsScore = uiState.kwsScore,
            voiceprintScore = uiState.voiceprintScore,
            kwsThreshold = uiState.kwsThreshold,
            voiceprintThreshold = uiState.voiceprintThreshold,
            selectedWakeword = uiState.selectedWakeword
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tín hiệu âm thanh Micro",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (uiState.isListening) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                AudioWaveformVisualizer(
                    amplitude = uiState.currentAmplitude,
                    isRecording = uiState.isListening,
                    visualizerHeight = 100.dp
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val buttonColor by animateColorAsState(
                targetValue = if (uiState.isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                label = "btn_color"
            )

            Button(
                onClick = onToggleListening,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isListening) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.isListening) "Tắt Micro" else "Bật Lắng Nhận Diện",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onResetVerification,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Khóa lại"
                )
            }
        }
    }
}

@Composable
fun ScreenLockSimulationCard(
    isUnlocked: Boolean,
    result: VerificationResult,
    hasOwnerProfile: Boolean,
    wakeword: String,
    shakeOffsetX: Float,
    onResetLock: () -> Unit,
    onNavigateToEnrollment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lockContainerColor by animateColorAsState(
        targetValue = when {
            isUnlocked -> Color(0xFF1E3A2B)
            result is VerificationResult.DeniedVoiceprint || result is VerificationResult.DeniedKeyword -> Color(0xFF3B1E22)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(400),
        label = "lock_color"
    )

    val lockBorderColor by animateColorAsState(
        targetValue = when {
            isUnlocked -> Color(0xFF4CAF50)
            result is VerificationResult.DeniedVoiceprint || result is VerificationResult.DeniedKeyword -> Color(0xFFEF5350)
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        label = "lock_border"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_unlock")
    val pulseGlowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = lockContainerColor),
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(shakeOffsetX.roundToInt(), 0) }
            .border(2.dp, lockBorderColor, RoundedCornerShape(32.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "12:45",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) Color(0xFF81C784) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Thứ Hai, 5 tháng 10",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(96.dp)
                ) {
                    if (isUnlocked) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(pulseGlowScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0x664CAF50),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = when {
                            isUnlocked -> Color(0xFF2E7D32)
                            result is VerificationResult.DeniedVoiceprint || result is VerificationResult.DeniedKeyword -> Color(0xFFC62828)
                            else -> MaterialTheme.colorScheme.primaryContainer
                        },
                        modifier = Modifier.size(76.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isUnlocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = if (isUnlocked || result is VerificationResult.DeniedVoiceprint || result is VerificationResult.DeniedKeyword) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                Text(
                    text = when {
                        isUnlocked -> "ĐÃ MỞ KHÓA THÀNH CÔNG!"
                        result is VerificationResult.DeniedVoiceprint -> "XÁC THỰC THẤT BẠI"
                        result is VerificationResult.DeniedKeyword -> "XÁC THỰC THẤT BẠI"
                        !hasOwnerProfile -> "CHƯA CÓ VÂN GIỌNG CHỦ NHÂN"
                        else -> "MÀN HÌNH ĐÃ KHÓA"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isUnlocked -> Color(0xFF81C784)
                        result is VerificationResult.DeniedVoiceprint || result is VerificationResult.DeniedKeyword -> Color(0xFFEF5350)
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    textAlign = TextAlign.Center
                )

                Text(
                    text = when (result) {
                        is VerificationResult.Granted -> "Từ khóa + Vân giọng khớp chính xác (${(result.voiceprintScore * 100).toInt()}%)"
                        is VerificationResult.DeniedVoiceprint -> "Sai vân giọng! Cosine Similarity (${(result.voiceprintScore * 100).toInt()}%) < Ngưỡng"
                        is VerificationResult.DeniedKeyword -> "Từ khóa không đúng! KWS Score (${(result.kwsScore * 100).toInt()}%) < Ngưỡng"
                        is VerificationResult.NoVoiceprintProfile -> "Hãy đăng ký mẫu giọng nói chủ nhân trước khi thử nghiệm"
                        is VerificationResult.Idle -> if (!hasOwnerProfile) "Hãy đăng ký vân giọng để bắt đầu sử dụng" else "Hãy nói từ khóa kích hoạt \"$wakeword\" để mở khóa"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                if (!hasOwnerProfile) {
                    Spacer(modifier = Modifier.height(4.dp))
                    FilledTonalButton(
                        onClick = onNavigateToEnrollment,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Đăng ký vân giọng ngay")
                    }
                } else if (isUnlocked) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onResetLock,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF81C784)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(Color(0xFF81C784), Color(0xFF4CAF50)))
                        )
                    ) {
                        Icon(imageVector = Icons.Rounded.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Khóa màn hình lại")
                    }
                }
            }
        }
    }
}

@Composable
fun TwoStagePipelineIndicator(
    pipelineStage: PipelineStage,
    result: VerificationResult,
    kwsScore: Float,
    voiceprintScore: Float,
    kwsThreshold: Float,
    voiceprintThreshold: Float,
    selectedWakeword: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Quy trình xử lý 2 Tầng (Hybrid Pipeline)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            StageItemRow(
                stageNumber = 1,
                title = "Tầng 1: KWS (Keyword Spotting)",
                subtitle = "Lắng nghe từ khóa \"$selectedWakeword\"",
                isActive = pipelineStage == PipelineStage.LISTENING_KWS,
                isSuccess = result is VerificationResult.Granted ||
                        result is VerificationResult.DeniedVoiceprint ||
                        (kwsScore >= kwsThreshold && kwsScore > 0f),
                isFailure = result is VerificationResult.DeniedKeyword,
                scoreText = if (kwsScore > 0f) "${(kwsScore * 100).toInt()}% (Ngưỡng ${(kwsThreshold * 100).toInt()}%)" else "Chờ tín hiệu..."
            )

            StageItemRow(
                stageNumber = 2,
                title = "Tầng 2: Voiceprint Verification",
                subtitle = "Trích xuất MFCC & So khớp Cosine Similarity",
                isActive = pipelineStage == PipelineStage.VERIFYING_VOICEPRINT,
                isSuccess = result is VerificationResult.Granted,
                isFailure = result is VerificationResult.DeniedVoiceprint,
                scoreText = if (voiceprintScore > 0f) "${(voiceprintScore * 100).toInt()}% (Ngưỡng ${(voiceprintThreshold * 100).toInt()}%)" else "Chờ Tầng 1..."
            )
        }
    }
}

@Composable
fun StageItemRow(
    stageNumber: Int,
    title: String,
    subtitle: String,
    isActive: Boolean,
    isSuccess: Boolean,
    isFailure: Boolean,
    scoreText: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_stage")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stage_pulse"
    )

    val iconColor = when {
        isSuccess -> Color(0xFF4CAF50)
        isFailure -> Color(0xFFEF5350)
        isActive -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    val iconVector: ImageVector = when {
        isSuccess -> Icons.Rounded.CheckCircle
        isFailure -> Icons.Rounded.Cancel
        isActive -> Icons.Rounded.GraphicEq
        else -> Icons.Rounded.Security
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier
                    .size(42.dp)
                    .scale(if (isActive) pulseScale else 1.0f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = scoreText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = iconColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
