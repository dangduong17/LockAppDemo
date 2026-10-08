package com.example.alldemo.features.deepgramnova2apiwebsocket.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun DeepgramNova2DemoScreen(
    onBackClick: (() -> Unit)? = null,
    viewModel: MainViewModel = viewModel(
        factory = MainViewModel.provideFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MainScreenContent(
        uiState = uiState,
        onApiKeyChange = viewModel::updateApiKey,
        onPasswordChange = viewModel::updatePassword,
        onToggleListening = viewModel::toggleListening,
        onResetUnlock = viewModel::resetUnlockState,
        onClearError = viewModel::clearError,
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    uiState: MainUiState,
    onApiKeyChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleListening: () -> Unit,
    onResetUnlock: () -> Unit,
    onClearError: () -> Unit,
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onToggleListening()
        } else {
            Toast.makeText(
                context,
                "Cần quyền Micro để sử dụng tính năng nhận diện giọng nói",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun handleTalkButtonClick() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            onToggleListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = if (uiState.isUnlocked) {
            Color(0xFF1E5622)
        } else {
            MaterialTheme.colorScheme.background
        },
        animationSpec = tween(durationMillis = 500),
        label = "backgroundColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (uiState.isUnlocked) {
            Color.White
        } else {
            MaterialTheme.colorScheme.onBackground
        },
        animationSpec = tween(durationMillis = 500),
        label = "contentColor"
    )

    Scaffold(
        containerColor = backgroundColor,
        contentColor = contentColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Deepgram Voice Unlock",
                        fontWeight = FontWeight.Bold
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (uiState.isUnlocked) Color(0xFF144118) else MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = if (uiState.isUnlocked) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isUnlocked) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2E7D32),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Success",
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ĐÃ MỞ KHÓA THÀNH CÔNG!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Mật khẩu giọng nói đã khớp chính xác.",
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            color = Color(0xFFE8F5E9)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onResetUnlock,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1B5E20)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Đặt lại / Khóa lại", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Mạng khóa giọng nói",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Nói mật khẩu cấu hình để mở khóa hệ thống",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            if (uiState.errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = "Lỗi",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = uiState.errorMessage,
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp
                        )
                        TextButton(onClick = onClearError) {
                            Text("Đóng")
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isUnlocked) Color(0x33FFFFFF) else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Cấu hình Mật khẩu & API Key",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (uiState.isUnlocked) Color.White else MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = uiState.apiKey,
                        onValueChange = onApiKeyChange,
                        label = { Text("Deepgram API Key") },
                        placeholder = { Text("Nhập API Key Deepgram...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Rounded.Key, contentDescription = null)
                        },
                        trailingIcon = {
                            if (uiState.apiKey.isNotEmpty()) {
                                IconButton(onClick = { onApiKeyChange("") }) {
                                    Icon(imageVector = Icons.Rounded.Clear, contentDescription = "Xóa")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (uiState.isUnlocked) Color.White else MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = if (uiState.isUnlocked) Color(0xAAFFFFFF) else MaterialTheme.colorScheme.outline,
                            focusedLabelColor = if (uiState.isUnlocked) Color.White else MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = if (uiState.isUnlocked) Color(0xDDFFFFFF) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        label = { Text("Mật khẩu cài đặt") },
                        placeholder = { Text("mở khóa") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Rounded.Password, contentDescription = null)
                        },
                        trailingIcon = {
                            if (uiState.password.isNotEmpty()) {
                                IconButton(onClick = { onPasswordChange("") }) {
                                    Icon(imageVector = Icons.Rounded.Clear, contentDescription = "Xóa")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (uiState.isUnlocked) Color.White else MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = if (uiState.isUnlocked) Color(0xAAFFFFFF) else MaterialTheme.colorScheme.outline,
                            focusedLabelColor = if (uiState.isUnlocked) Color.White else MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = if (uiState.isUnlocked) Color(0xDDFFFFFF) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Button(
                onClick = ::handleTalkButtonClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isListening) {
                        MaterialTheme.colorScheme.error
                    } else if (uiState.isUnlocked) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (uiState.isListening) {
                        MaterialTheme.colorScheme.onError
                    } else if (uiState.isUnlocked) {
                        Color(0xFF1B5E20)
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    }
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (uiState.isListening) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Đang lắng nghe... (Chạm để dừng)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    } else {
                        Icon(
                            imageVector = if (uiState.isUnlocked) Icons.Rounded.LockOpen else Icons.Rounded.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Nói mật khẩu",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isUnlocked) Color(0x33FFFFFF) else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Live Transcript (Thời gian thực)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (uiState.isUnlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.isListening) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LIVE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Red
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (uiState.isUnlocked) Color(0x44000000) else MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            if (uiState.transcript.isNotBlank()) {
                                Text(
                                    text = uiState.transcript,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (uiState.isUnlocked) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Text(
                                    text = if (uiState.isListening) "Đang chờ giọng nói từ micro..." else "Chưa có văn bản. Nhấn 'Nói mật khẩu' để bắt đầu.",
                                    fontSize = 14.sp,
                                    color = if (uiState.isUnlocked) Color(0xCCFFFFFF) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
