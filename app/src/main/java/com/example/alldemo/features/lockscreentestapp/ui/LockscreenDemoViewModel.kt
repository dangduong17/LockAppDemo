package com.example.alldemo.features.lockscreentestapp.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alldemo.features.lockscreentestapp.model.LatencyLogEntry
import com.example.alldemo.features.lockscreentestapp.model.LatencySummaryStats
import com.example.alldemo.features.lockscreentestapp.service.OverlayService
import com.example.alldemo.features.lockscreentestapp.util.LatencyLogRepository
import com.example.alldemo.features.lockscreentestapp.util.PermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LockscreenDemoUiState(
    val isOverlayPermissionGranted: Boolean = false,
    val isNotificationPermissionGranted: Boolean = false,
    val isFullScreenIntentPermissionGranted: Boolean = false,
    val isServiceRunning: Boolean = false,
    val lastScreenEvent: String = "NONE",
    val lastEventTimestamp: Long = 0L,
    val isOverlayShowing: Boolean = false,
    val latencyLogs: List<LatencyLogEntry> = emptyList(),
    val summaryStats: LatencySummaryStats = LatencySummaryStats(),
    val userMessage: String? = null
) {
    val areAllPermissionsGranted: Boolean
        get() = isOverlayPermissionGranted && isNotificationPermissionGranted && isFullScreenIntentPermissionGranted
}

class LockscreenDemoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LockscreenDemoUiState())
    val uiState: StateFlow<LockscreenDemoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            OverlayService.isServiceRunning.collect { isRunning ->
                _uiState.update { it.copy(isServiceRunning = isRunning) }
            }
        }
        viewModelScope.launch {
            OverlayService.lastScreenEvent.collect { event ->
                _uiState.update { it.copy(lastScreenEvent = event) }
            }
        }
        viewModelScope.launch {
            OverlayService.lastEventTimestamp.collect { timestamp ->
                _uiState.update { it.copy(lastEventTimestamp = timestamp) }
            }
        }
        viewModelScope.launch {
            OverlayService.isOverlayShowing.collect { isShowing ->
                _uiState.update { it.copy(isOverlayShowing = isShowing) }
            }
        }
        viewModelScope.launch {
            LatencyLogRepository.logs.collect { logs ->
                val stats = LatencyLogRepository.getSummaryStats(logs)
                _uiState.update {
                    it.copy(
                        latencyLogs = logs,
                        summaryStats = stats
                    )
                }
            }
        }
    }

    fun refreshPermissions(context: Context) {
        val overlayGranted = PermissionManager.hasOverlayPermission(context)
        val notificationGranted = PermissionManager.hasNotificationPermission(context)
        val fullScreenGranted = PermissionManager.hasFullScreenIntentPermission(context)

        _uiState.update {
            it.copy(
                isOverlayPermissionGranted = overlayGranted,
                isNotificationPermissionGranted = notificationGranted,
                isFullScreenIntentPermissionGranted = fullScreenGranted
            )
        }
    }

    fun toggleOverlayService(context: Context, enable: Boolean) {
        if (enable) {
            if (!PermissionManager.hasOverlayPermission(context)) {
                _uiState.update {
                    it.copy(
                        userMessage = "System Overlay permission is required to start the service."
                    )
                }
                return
            }
            OverlayService.startOverlayService(context)
        } else {
            OverlayService.stopOverlayService(context)
        }
    }

    fun triggerTestOverlay(context: Context) {
        if (!PermissionManager.hasOverlayPermission(context)) {
            _uiState.update {
                it.copy(
                    userMessage = "System Overlay permission is required to display lockscreen overlay."
                )
            }
            return
        }
        OverlayService.triggerTestOverlay(context)
    }

    fun clearLatencyLogs() {
        LatencyLogRepository.clearLogs()
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
