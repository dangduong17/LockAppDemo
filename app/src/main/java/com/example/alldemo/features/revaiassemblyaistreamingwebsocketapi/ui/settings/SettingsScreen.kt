package com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.data.ApiProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(LocalContext.current.applicationContext as android.app.Application),
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Cấu Hình Ứng Dụng", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Quay lại"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        SettingsContent(
            uiState = uiState,
            onAssemblyAiApiKeyChanged = viewModel::onAssemblyAiApiKeyChanged,
            onRevAiApiKeyChanged = viewModel::onRevAiApiKeyChanged,
            onProviderSelected = viewModel::onProviderSelected,
            onTargetPasswordChanged = viewModel::onTargetPasswordChanged,
            onSaveClicked = viewModel::saveSettings,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        )
    }
}

@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onAssemblyAiApiKeyChanged: (String) -> Unit,
    onRevAiApiKeyChanged: (String) -> Unit,
    onProviderSelected: (ApiProvider) -> Unit,
    onTargetPasswordChanged: (String) -> Unit,
    onSaveClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAssemblyAiKey by remember { mutableStateOf(false) }
    var showRevAiKey by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Provider selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Nhà Cung Cấp API",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Chọn dịch vụ nhận dạng giọng nói WebSocket theo thời gian thực:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ApiProvider.entries.forEach { provider ->
                        val isSelected = uiState.selectedProvider == provider
                        FilterChip(
                            selected = isSelected,
                            onClick = { onProviderSelected(provider) },
                            label = {
                                Text(
                                    text = provider.displayName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Section 2: API Keys
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "API Key",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // AssemblyAI API Key
                OutlinedTextField(
                    value = uiState.assemblyAiApiKey,
                    onValueChange = onAssemblyAiApiKeyChanged,
                    label = { Text("AssemblyAI API Key") },
                    placeholder = { Text("Nhập AssemblyAI API Key...") },
                    singleLine = true,
                    visualTransformation = if (showAssemblyAiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    trailingIcon = {
                        IconButton(onClick = { showAssemblyAiKey = !showAssemblyAiKey }) {
                            Icon(
                                imageVector = if (showAssemblyAiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showAssemblyAiKey) "Ẩn API key" else "Hiện API key"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Rev AI API Key
                OutlinedTextField(
                    value = uiState.revAiApiKey,
                    onValueChange = onRevAiApiKeyChanged,
                    label = { Text("Rev AI API Key") },
                    placeholder = { Text("Nhập Rev AI Access Token...") },
                    singleLine = true,
                    visualTransformation = if (showRevAiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    trailingIcon = {
                        IconButton(onClick = { showRevAiKey = !showRevAiKey }) {
                            Icon(
                                imageVector = if (showRevAiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showRevAiKey) "Ẩn API key" else "Hiện API key"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                val activeKeyMessage = when (uiState.selectedProvider) {
                    ApiProvider.ASSEMBLY_AI -> if (uiState.assemblyAiApiKey.isBlank()) "⚠️ Chưa nhập AssemblyAI API Key" else "✅ AssemblyAI API Key đang hoạt động"
                    ApiProvider.REV_AI -> if (uiState.revAiApiKey.isBlank()) "⚠️ Chưa nhập Rev AI API Key" else "✅ Rev AI API Key đang hoạt động"
                }

                Text(
                    text = activeKeyMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (activeKeyMessage.startsWith("⚠️")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }

        // Section 3: Target Password / Keyword
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Mật Khẩu Mục Tiêu (Target Keyword)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Khi cụm từ này xuất hiện trong kết quả nhận dạng giọng nói, ứng dụng sẽ kích hoạt lệnh thành công.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                OutlinedTextField(
                    value = uiState.targetPassword,
                    onValueChange = onTargetPasswordChanged,
                    label = { Text("Mật khẩu/Từ khóa mục tiêu") },
                    placeholder = { Text("Ví dụ: mở khóa") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Save Button
        Button(
            onClick = onSaveClicked,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = "Lưu Cấu Hình",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
