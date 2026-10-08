package com.example.alldemo.features.locktestapp.ui.applist

import com.example.alldemo.features.locktestapp.data.model.AppInfo
import com.example.alldemo.features.locktestapp.data.repository.LockedAppsRepository

enum class AppFilter {
    ALL,
    USER,
    SYSTEM,
    LOCKED
}

data class PermissionsState(
    val hasUsageStats: Boolean = false,
    val hasOverlay: Boolean = false
) {
    val allGranted: Boolean get() = hasUsageStats && hasOverlay
}

data class AppListUiState(
    val isLoading: Boolean = true,
    val apps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val filter: AppFilter = AppFilter.ALL,
    val permissionsState: PermissionsState = PermissionsState(),
    val isServiceEnabled: Boolean = false,
    val currentPin: String = LockedAppsRepository.DEFAULT_PIN,
    val showChangePinDialog: Boolean = false,
    val lockedCount: Int = 0,
    val totalCount: Int = 0
)
