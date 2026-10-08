package com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.ui.unlock

import android.Manifest
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.data.ApiProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UnlockScreen(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    viewModel: UnlockViewModel = viewModel(
        factory = UnlockViewModel.Factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    UnlockScreenContent(
        uiState = uiState,
        hasAudioPermission = audioPermissionState.status.isGranted,
        onRequestPermission = { audioPermissionState.launchPermissionRequest() },
        onStartListening = viewModel::startListening,
        onStopListening = viewModel::stopListening,
        onReset = viewModel::reset,
        onNavigateToSettings = onNavigateToSettings,
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockScreenContent(
    uiState: UnlockUiState,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onReset: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null
) {
    val isUnlocked = uiState.status == UnlockStatus.UNLOCKED

    // Animated background color transition to Vibrant Green when unlocked
    val backgroundColor by animateColorAsState(
        targetValue = if (isUnlocked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.background,
        animationSpec = tween(durationMillis = 600),
        label = "backgroundColorTransition"
    )

    val topAppBarContainerColor by animateColorAsState(
        targetValue = if (isUnlocked) Color(0xFF1B5E20) else MaterialTheme.colorScheme.primaryContainer,
        animationSpec = tween(durationMillis = 600),
        label = "topAppBarColorTransition"
    )

    val contentColor = if (isUnlocked) Color.White else MaterialTheme.colorScheme.onBackground

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "RevAI & AssemblyAI Voice Unlock",
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Quay lại Dashboard",
                                tint = if (isUnlocked) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Cấu hình cài đặt",
                            tint = if (isUnlocked) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topAppBarContainerColor
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Status Icon Display
            LockStatusHeader(
                status = uiState.status,
                contentColor = contentColor
            )

            // Target Password Info Card
            TargetPasswordCard(
                targetPassword = uiState.targetPassword,
                providerName = uiState.selectedProvider.displayName,
                isUnlocked = isUnlocked
            )

            // Real-Time Transcript Display
            TranscriptCard(
                transcript = uiState.currentTranscript,
                status = uiState.status,
                isUnlocked = isUnlocked
            )

            // Error / Status Message Banner
            if (uiState.errorMessage != null && !isUnlocked) {
                ErrorMessageBanner(
                    errorMessage = uiState.errorMessage,
                    hasApiKey = uiState.hasApiKey,
                    selectedProvider = uiState.selectedProvider,
                    onNavigateToSettings = onNavigateToSettings
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Control Buttons
            if (isUnlocked) {
                // Success Action Controls
                Button(
                    onClick = onReset,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1B5E20)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mở Khóa Lại",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (!hasAudioPermission) {
                // Permission Request Button
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MicOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cần quyền Ghi âm (RECORD_AUDIO)",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Text(
                            text = "Ứng dụng cần quyền ghi âm microphone để nhận diện mật khẩu giọng nói.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Cấp Quyền Micro")
                        }
                    }
                }
            } else {
                // Main Voice Listening Button
                VoiceActionButton(
                    status = uiState.status,
                    onStartListening = onStartListening,
                    onStopListening = onStopListening,
                    onReset = onReset
                )
            }
        }
    }
}

@Composable
fun LockStatusHeader(
    status: UnlockStatus,
    contentColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(110.dp)
                .scale(if (status == UnlockStatus.LISTENING) scale else 1f)
                .background(
                    color = when (status) {
                        UnlockStatus.UNLOCKED -> Color.White.copy(alpha = 0.25f)
                        UnlockStatus.LISTENING -> MaterialTheme.colorScheme.primaryContainer
                        UnlockStatus.CONNECTING -> MaterialTheme.colorScheme.secondaryContainer
                        UnlockStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                        UnlockStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                        UnlockStatus.IDLE -> MaterialTheme.colorScheme.primaryContainer
                    },
                    shape = CircleShape
                )
                .border(
                    width = 3.dp,
                    color = when (status) {
                        UnlockStatus.UNLOCKED -> Color.White
                        UnlockStatus.LISTENING -> MaterialTheme.colorScheme.primary
                        UnlockStatus.CONNECTING -> MaterialTheme.colorScheme.secondary
                        UnlockStatus.FAILED -> MaterialTheme.colorScheme.error
                        UnlockStatus.ERROR -> MaterialTheme.colorScheme.error
                        UnlockStatus.IDLE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    },
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = when (status) {
                    UnlockStatus.UNLOCKED -> Icons.Default.LockOpen
                    UnlockStatus.LISTENING -> Icons.Default.Mic
                    UnlockStatus.CONNECTING -> Icons.Outlined.RecordVoiceOver
                    UnlockStatus.FAILED -> Icons.Default.Lock
                    UnlockStatus.ERROR -> Icons.Default.ErrorOutline
                    UnlockStatus.IDLE -> Icons.Default.Lock
                },
                contentDescription = null,
                tint = when (status) {
                    UnlockStatus.UNLOCKED -> Color.White
                    UnlockStatus.LISTENING -> MaterialTheme.colorScheme.primary
                    UnlockStatus.CONNECTING -> MaterialTheme.colorScheme.secondary
                    UnlockStatus.FAILED -> MaterialTheme.colorScheme.error
                    UnlockStatus.ERROR -> MaterialTheme.colorScheme.error
                    UnlockStatus.IDLE -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(56.dp)
            )
        }

        Text(
            text = when (status) {
                UnlockStatus.UNLOCKED -> "MỞ KHÓA THÀNH CÔNG!"
                UnlockStatus.LISTENING -> "Đang Lắng Nghe..."
                UnlockStatus.CONNECTING -> "Đang Kết Nối WebSocket..."
                UnlockStatus.FAILED -> "Mật Khẩu Không Trùng Khớp"
                UnlockStatus.ERROR -> "Gặp Lỗi Kết Nối"
                UnlockStatus.IDLE -> "Nhấn 'Nói Mật Khẩu' Để Mở Khóa"
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = contentColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TargetPasswordCard(
    targetPassword: String,
    providerName: String,
    isUnlocked: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) {
                Color.White.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Mật Khẩu Mục Tiêu:",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isUnlocked) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = targetPassword.ifBlank { "(Chưa thiết lập)" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .background(
                        color = if (isUnlocked) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = providerName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isUnlocked) Color.White else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun TranscriptCard(
    transcript: String,
    status: UnlockStatus,
    isUnlocked: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) {
                Color.White.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnlocked) 0.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Văn Bản Nhận Diện (Real-Time):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary
                )

                if (status == UnlockStatus.LISTENING) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = if (isUnlocked) Color.White else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Live",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) Color.White else MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (isUnlocked) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Thành công",
                        tint = Color.White
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (transcript.isNotBlank()) {
                    Text(
                        text = transcript,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (isUnlocked) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp
                    )
                } else {
                    Text(
                        text = when (status) {
                            UnlockStatus.CONNECTING -> "Đang thiết lập kết nối WebSocket..."
                            UnlockStatus.LISTENING -> "Đang chờ giọng nói của bạn..."
                            UnlockStatus.UNLOCKED -> "Đã nhận diện mật khẩu thành công!"
                            else -> "Hãy nhấn nút bên dưới và nói mật khẩu..."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUnlocked) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorMessageBanner(
    errorMessage: String,
    hasApiKey: Boolean,
    selectedProvider: ApiProvider,
    onNavigateToSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Thông Báo Lỗi Kết Nối",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (selectedProvider == ApiProvider.ASSEMBLY_AI &&
                (errorMessage.contains("404") || errorMessage.contains("401") || errorMessage.contains("thẻ thanh toán") || errorMessage.contains("AssemblyAI"))
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "💡 Gợi ý: Chuyển sang nhà cung cấp Rev AI",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Rev AI cung cấp $5 dùng thử miễn phí Real-Time WebSocket ngay khi tạo tài khoản, không yêu cầu thẻ thanh toán.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = onNavigateToSettings,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Chuyển sang Rev AI trong Cài Đặt")
                            }
                        }
                    }
                }
            } else if (!hasApiKey || errorMessage.contains("API Key") || errorMessage.contains("401") || errorMessage.contains("404") || errorMessage.contains("trống")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onNavigateToSettings
                    ) {
                        Text("Đến Cài Đặt Key")
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceActionButton(
    status: UnlockStatus,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when (status) {
            UnlockStatus.LISTENING, UnlockStatus.CONNECTING -> {
                Button(
                    onClick = onStopListening,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (status == UnlockStatus.CONNECTING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đang Kết Nối... (Hủy)", style = MaterialTheme.typography.titleMedium)
                    } else {
                        Icon(imageVector = Icons.Default.MicOff, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dừng Lắng Nghe", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            UnlockStatus.FAILED, UnlockStatus.ERROR -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Đặt Lại")
                    }

                    Button(
                        onClick = onStartListening,
                        modifier = Modifier
                            .weight(2f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Thử Lại", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            else -> {
                Button(
                    onClick = onStartListening,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nói Mật Khẩu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
