package com.example.alldemo.features.wakewordvoiceprint.ui.settings

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alldemo.features.wakewordvoiceprint.ui.enrollment.AudioWaveformVisualizer
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsDashboardScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val micPermissionState = rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO)

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Automatically stop test recording if microphone permission is revoked
    LaunchedEffect(micPermissionState.status.isGranted) {
        if (!micPermissionState.status.isGranted && uiState.isTestRecording) {
            viewModel.stopTestRecording()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Cài đặt & Debug Dashboard",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth >= 600.dp

            if (isWideScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ThresholdConfigurationCard(
                            uiState = uiState,
                            onKwsThresholdChange = { viewModel.updateKwsThreshold(it) },
                            onVoiceprintThresholdChange = { viewModel.updateVoiceprintThreshold(it) },
                            onWakewordSelect = { viewModel.updateSelectedWakeword(it) },
                            onResetDefaults = { viewModel.resetToDefaults() }
                        )

                        MasterVoiceprintProfileCard(
                            uiState = uiState,
                            onDeleteClick = { showDeleteConfirmDialog = true }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        StandaloneTestModeCard(
                            uiState = uiState,
                            micPermissionGranted = micPermissionState.status.isGranted,
                            onRequestPermission = { micPermissionState.launchPermissionRequest() },
                            onSelectTab = { viewModel.selectTestTab(it) },
                            onToggleRecording = {
                                if (micPermissionState.status.isGranted) {
                                    viewModel.toggleTestRecording()
                                } else {
                                    micPermissionState.launchPermissionRequest()
                                }
                            }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ThresholdConfigurationCard(
                        uiState = uiState,
                        onKwsThresholdChange = { viewModel.updateKwsThreshold(it) },
                        onVoiceprintThresholdChange = { viewModel.updateVoiceprintThreshold(it) },
                        onWakewordSelect = { viewModel.updateSelectedWakeword(it) },
                        onResetDefaults = { viewModel.resetToDefaults() }
                    )

                    MasterVoiceprintProfileCard(
                        uiState = uiState,
                        onDeleteClick = { showDeleteConfirmDialog = true }
                    )

                    StandaloneTestModeCard(
                        uiState = uiState,
                        micPermissionGranted = micPermissionState.status.isGranted,
                        onRequestPermission = { micPermissionState.launchPermissionRequest() },
                        onSelectTab = { viewModel.selectTestTab(it) },
                        onToggleRecording = {
                            if (micPermissionState.status.isGranted) {
                                viewModel.toggleTestRecording()
                            } else {
                                micPermissionState.launchPermissionRequest()
                            }
                        }
                    )
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Xóa Vân giọng Chủ nhân?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Thao tác này sẽ xóa hồ sơ vân giọng hiện tại khỏi hệ thống. " +
                            "Bạn sẽ cần đăng ký lại vân giọng mới để sử dụng tính năng mở khóa 2 tầng."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMasterVoiceprint()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Xóa Hồ sơ")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun ThresholdConfigurationCard(
    uiState: SettingsUiState,
    onKwsThresholdChange: (Float) -> Unit,
    onVoiceprintThresholdChange: (Float) -> Unit,
    onWakewordSelect: (String) -> Unit,
    onResetDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cấu hình Ngưỡng & Từ khóa",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onResetDefaults) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Reset mặc định",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Từ khóa kích hoạt (Wake-word):",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    uiState.availableWakewords.forEach { word ->
                        val isSelected = word == uiState.selectedWakeword
                        FilterChip(
                            selected = isSelected,
                            onClick = { onWakewordSelect(word) },
                            label = { Text(word) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ngưỡng KWS (Tầng 1):",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "%.2f".format(uiState.kwsThreshold),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Slider(
                    value = uiState.kwsThreshold,
                    onValueChange = onKwsThresholdChange,
                    valueRange = 0.50f..0.95f,
                    steps = 89
                )

                Text(
                    text = "Phát hiện từ khóa kích hoạt (${uiState.selectedWakeword}). Ngưỡng cao giúp giảm báo động giả.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ngưỡng Vân giọng (Tầng 2):",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "%.2f".format(uiState.voiceprintThreshold),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Slider(
                    value = uiState.voiceprintThreshold,
                    onValueChange = onVoiceprintThresholdChange,
                    valueRange = 0.50f..0.95f,
                    steps = 89
                )

                Text(
                    text = "So sánh Cosine Similarity vân giọng chủ nhân. Ngưỡng cao hơn đòi hỏi giọng đọc chính xác hơn.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MasterVoiceprintProfileCard(
    uiState: SettingsUiState,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Fingerprint,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Vân giọng Chủ nhân (Profile)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                val badgeColor = if (uiState.isVoiceprintEnrolled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
                val textColor = if (uiState.isVoiceprintEnrolled) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }

                Surface(
                    color = badgeColor,
                    shape = CircleShape
                ) {
                    Text(
                        text = if (uiState.isVoiceprintEnrolled) "Đã đăng ký" else "Chưa đăng ký",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }

            Text(
                text = "Vector đặc trưng kích thước ${uiState.voiceprintDimensions} chiều (13 MFCC Means + 13 MFCC Stds):",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CompactVoiceprintSpectrumVisualizer(
                vector = uiState.masterVoiceprintVector,
                chartHeight = 100.dp
            )

            if (uiState.isVoiceprintEnrolled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDeleteClick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Xóa Profile")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StandaloneTestModeCard(
    uiState: SettingsUiState,
    micPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onSelectTab: (StandaloneTestTab) -> Unit,
    onToggleRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.BugReport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Module Kiểm thử Độc lập",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Kiểm tra riêng lẻ tính đúng đắn của Tầng 1 (KWS) hoặc Tầng 2 (Voiceprint Similarity).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PrimaryTabRow(
                selectedTabIndex = if (uiState.activeTestTab == StandaloneTestTab.KWS_ONLY) 0 else 1,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = uiState.activeTestTab == StandaloneTestTab.KWS_ONLY,
                    onClick = { onSelectTab(StandaloneTestTab.KWS_ONLY) },
                    text = { Text("Test KWS Only", fontWeight = FontWeight.SemiBold) },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                Tab(
                    selected = uiState.activeTestTab == StandaloneTestTab.VOICEPRINT_ONLY,
                    onClick = { onSelectTab(StandaloneTestTab.VOICEPRINT_ONLY) },
                    text = { Text("Test Voiceprint Only", fontWeight = FontWeight.SemiBold) },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }

            when (uiState.activeTestTab) {
                StandaloneTestTab.KWS_ONLY -> {
                    KwsStandaloneTestPanel(
                        uiState = uiState,
                        micPermissionGranted = micPermissionGranted,
                        onRequestPermission = onRequestPermission,
                        onToggleRecording = onToggleRecording
                    )
                }
                StandaloneTestTab.VOICEPRINT_ONLY -> {
                    VoiceprintStandaloneTestPanel(
                        uiState = uiState,
                        micPermissionGranted = micPermissionGranted,
                        onRequestPermission = onRequestPermission,
                        onToggleRecording = onToggleRecording
                    )
                }
            }
        }
    }
}

@Composable
private fun KwsStandaloneTestPanel(
    uiState: SettingsUiState,
    micPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onToggleRecording: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Thu âm câu nói và kiểm tra điểm kích hoạt từ khóa (${uiState.selectedWakeword}) độc lập:",
            style = MaterialTheme.typography.bodyMedium
        )

        AudioWaveformVisualizer(
            amplitude = uiState.testAmplitude,
            isRecording = uiState.isTestRecording,
            visualizerHeight = 90.dp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            if (!micPermissionGranted) {
                Button(onClick = onRequestPermission) {
                    Icon(imageVector = Icons.Rounded.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cấp quyền Micro")
                }
            } else {
                Button(
                    onClick = onToggleRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isTestRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (uiState.isTestRecording) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.isTestRecording) "Dừng Thử KWS" else "Thử nghiệm KWS")
                }
            }
        }

        val kwsResult = uiState.kwsTestResult
        if (kwsResult != null) {
            val isPassed = kwsResult.detected
            val containerColor = if (isPassed) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor)
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
                        Text(
                            text = if (isPassed) "KWS: PHÁT HIỆN" else "KWS: KHÔNG PHÁT HIỆN",
                            fontWeight = FontWeight.Bold,
                            color = if (isPassed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )

                        Icon(
                            imageVector = if (isPassed) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                            contentDescription = null,
                            tint = if (isPassed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }

                    Text(
                        text = "Điểm KWS: %.2f / Ngưỡng: %.2f".format(kwsResult.score, kwsResult.thresholdUsed),
                        style = MaterialTheme.typography.bodySmall
                    )

                    LinearProgressIndicator(
                        progress = { kwsResult.score.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                    )

                    Text(
                        text = "Năng lượng tín hiệu (RMS): %.3f".format(kwsResult.energy),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceprintStandaloneTestPanel(
    uiState: SettingsUiState,
    micPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onToggleRecording: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!uiState.isVoiceprintEnrolled) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Chưa có vân giọng chủ nhân. Vui lòng sang tab Đăng ký để lưu vân giọng trước khi kiểm thử.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Text(
            text = "Thu âm câu nói bất kỳ để tính điểm Cosine Similarity so với Vân giọng chủ nhân:",
            style = MaterialTheme.typography.bodyMedium
        )

        AudioWaveformVisualizer(
            amplitude = uiState.testAmplitude,
            isRecording = uiState.isTestRecording,
            visualizerHeight = 90.dp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            if (!micPermissionGranted) {
                Button(onClick = onRequestPermission) {
                    Icon(imageVector = Icons.Rounded.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cấp quyền Micro")
                }
            } else {
                Button(
                    onClick = onToggleRecording,
                    enabled = uiState.isVoiceprintEnrolled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isTestRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        imageVector = if (uiState.isTestRecording) Icons.Rounded.MicOff else Icons.Rounded.RecordVoiceOver,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.isTestRecording) "Dừng Thử Vân Giọng" else "Thử nghiệm Vân giọng")
                }
            }
        }

        val vpResult = uiState.voiceprintTestResult
        if (vpResult != null && vpResult.hasMasterProfile) {
            val isMatch = vpResult.isMatch
            val containerColor = if (isMatch) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor)
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
                        Text(
                            text = if (isMatch) "VOICEPRINT: GIỌNG CHỦ NHÂN" else "VOICEPRINT: KHÔNG PHẢI CHỦ NHÂN",
                            fontWeight = FontWeight.Bold,
                            color = if (isMatch) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                        )

                        Icon(
                            imageVector = if (isMatch) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                            contentDescription = null,
                            tint = if (isMatch) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                        )
                    }

                    Text(
                        text = "Cosine Similarity: %.2f / Ngưỡng: %.2f".format(vpResult.similarityScore, vpResult.thresholdUsed),
                        style = MaterialTheme.typography.bodySmall
                    )

                    LinearProgressIndicator(
                        progress = { vpResult.similarityScore.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                    )

                    Text(
                        text = if (isMatch) "Độ tương đồng cao hơn ngưỡng (${vpResult.thresholdUsed}). Cho phép mở khóa."
                        else "Độ tương đồng thấp hơn ngưỡng (${vpResult.thresholdUsed}). Từ chối truy cập.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
