package com.example.alldemo.features.geminimultimodallivevoiceapi.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.alldemo.features.geminimultimodallivevoiceapi.network.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionSettingsCard(
    apiKey: String,
    targetPassword: String,
    selectedModel: String,
    connectionState: ConnectionState,
    onApiKeyChange: (String) -> Unit,
    onTargetPasswordChange: (String) -> Unit,
    onModelChange: (String) -> Unit = {},
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var apiKeyVisible by remember { mutableStateOf(false) }
    var isExpandedSettings by remember { mutableStateOf(true) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Gemini Live Voice Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "wss://generativelanguage.googleapis.com/... • $selectedModel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ConnectionStateBadge(connectionState = connectionState)
            }

            AnimatedVisibility(visible = isExpandedSettings) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = onApiKeyChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Google Gemini API Key (AI Studio)") },
                        placeholder = { Text("AIzaSy...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Key,
                                contentDescription = "API Key Icon"
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Icon(
                                    imageVector = if (apiKeyVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (apiKeyVisible) "Hide API Key" else "Show API Key"
                                )
                            }
                        },
                        visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true
                    )

                    var modelDropdownExpanded by remember { mutableStateOf(false) }
                    val availableModels = remember {
                        listOf(
                            "models/gemini-3.1-flash-live-preview",
                            "models/gemini-2.5-flash-native-audio-preview-12-2025"
                        )
                    }

                    ExposedDropdownMenuBox(
                        expanded = modelDropdownExpanded,
                        onExpandedChange = { modelDropdownExpanded = !modelDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedModel,
                            onValueChange = onModelChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryEditable),
                            label = { Text("Realtime Model") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.SmartToy,
                                    contentDescription = "Model Icon"
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded)
                            },
                            singleLine = true,
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = modelDropdownExpanded,
                            onDismissRequest = { modelDropdownExpanded = false }
                        ) {
                            availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model) },
                                    onClick = {
                                        onModelChange(model)
                                        modelDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = targetPassword,
                        onValueChange = onTargetPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Target Voice Password / Mật khẩu voice") },
                        placeholder = { Text("e.g. mở khóa") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Target Password Icon"
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { keyboardController?.hide() }
                        ),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (connectionState) {
                            is ConnectionState.Connected -> {
                                OutlinedButton(
                                    onClick = onDisconnectClick,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PowerOff,
                                        contentDescription = "Disconnect Icon",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Disconnect")
                                }
                            }

                            is ConnectionState.Connecting -> {
                                Button(
                                    onClick = {},
                                    enabled = false
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Connecting...")
                                }
                            }

                            else -> {
                                Button(
                                    onClick = {
                                        keyboardController?.hide()
                                        onConnectClick()
                                    },
                                    enabled = apiKey.isNotBlank()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Power,
                                        contentDescription = "Connect Icon",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Connect WebSocket")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionStateBadge(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, contentColor, text, icon) = when (connectionState) {
        is ConnectionState.Connected -> Quadruple(
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32),
            "Connected",
            Icons.Filled.CheckCircle
        )
        is ConnectionState.Connecting -> Quadruple(
            Color(0xFFFFF8E1),
            Color(0xFFF57F17),
            "Connecting",
            Icons.Filled.Sync
        )
        is ConnectionState.Error -> Quadruple(
            Color(0xFFFFEBEE),
            Color(0xFFC62828),
            "Error",
            Icons.Filled.Error
        )
        else -> Quadruple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Disconnected",
            Icons.Filled.PowerOff
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
