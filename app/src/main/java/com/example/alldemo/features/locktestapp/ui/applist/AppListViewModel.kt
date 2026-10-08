package com.example.alldemo.features.locktestapp.ui.applist

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alldemo.features.locktestapp.data.model.AppInfo
import com.example.alldemo.features.locktestapp.data.repository.LockedAppsRepository
import com.example.alldemo.features.locktestapp.service.AppLockService
import com.example.alldemo.features.locktestapp.util.InstalledAppsLoader
import com.example.alldemo.features.locktestapp.util.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LockedAppsRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(AppListUiState())
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    private var lockedPackages: Set<String> = emptySet()
    private var rawApps: List<AppInfo> = emptyList()

    init {
        checkPermissions()
        observeLockedApps()
        observeServiceAndPinState()
    }

    fun checkPermissions() {
        val context = getApplication<Application>().applicationContext
        val usageGranted = PermissionUtils.hasUsageStatsPermission(context)
        val overlayGranted = PermissionUtils.hasOverlayPermission(context)
        val allGranted = usageGranted && overlayGranted

        _uiState.update { currentState ->
            currentState.copy(
                permissionsState = PermissionsState(
                    hasUsageStats = usageGranted,
                    hasOverlay = overlayGranted
                )
            )
        }

        if (_uiState.value.isServiceEnabled && allGranted && !AppLockService.isServiceRunning) {
            AppLockService.startService(context)
        }
    }

    private fun observeLockedApps() {
        viewModelScope.launch {
            repository.lockedPackagesFlow.collectLatest { set ->
                lockedPackages = set
                if (rawApps.isEmpty()) {
                    loadInstalledApps()
                } else {
                    updateAppListWithLocks()
                }
            }
        }
    }

    private fun observeServiceAndPinState() {
        viewModelScope.launch {
            repository.isServiceEnabledFlow.collectLatest { isEnabled ->
                _uiState.update { it.copy(isServiceEnabled = isEnabled) }
                val context = getApplication<Application>().applicationContext
                val permissionsGranted = PermissionUtils.hasUsageStatsPermission(context) &&
                        PermissionUtils.hasOverlayPermission(context)

                if (isEnabled && permissionsGranted) {
                    if (!AppLockService.isServiceRunning) {
                        AppLockService.startService(context)
                    }
                } else if (!isEnabled) {
                    AppLockService.stopService(context)
                }
            }
        }

        viewModelScope.launch {
            repository.pinFlow.collectLatest { pin ->
                _uiState.update { it.copy(currentPin = pin) }
            }
        }
    }

    fun toggleService(enabled: Boolean) {
        val context = getApplication<Application>().applicationContext
        viewModelScope.launch {
            if (enabled) {
                checkPermissions()
                val permissionsGranted = PermissionUtils.hasUsageStatsPermission(context) &&
                        PermissionUtils.hasOverlayPermission(context)

                repository.setServiceEnabled(true)
                if (permissionsGranted) {
                    AppLockService.startService(context)
                } else {
                    Toast.makeText(
                        getApplication(),
                        "Grant Usage Access and Display Over Other Apps permissions to activate App Lock.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                repository.setServiceEnabled(false)
                AppLockService.stopService(context)
            }
        }
    }

    fun updatePin(newPin: String) {
        viewModelScope.launch {
            repository.setPin(newPin)
        }
    }

    fun setShowChangePinDialog(show: Boolean) {
        _uiState.update { it.copy(showChangePinDialog = show) }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val context = getApplication<Application>().applicationContext

            val apps = withContext(Dispatchers.IO) {
                InstalledAppsLoader.getInstalledApps(context, lockedPackages)
            }

            rawApps = apps
            applyFilters()
        }
    }

    private fun updateAppListWithLocks() {
        rawApps = rawApps.map { app ->
            app.copy(isLocked = lockedPackages.contains(app.packageName))
        }
        applyFilters()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun onFilterChanged(filter: AppFilter) {
        _uiState.update { it.copy(filter = filter) }
        applyFilters()
    }

    fun toggleAppLock(packageName: String) {
        viewModelScope.launch {
            repository.toggleLock(packageName)
        }
    }

    private fun applyFilters() {
        val state = _uiState.value
        val query = state.searchQuery.trim().lowercase()
        val filter = state.filter

        val filtered = rawApps.filter { app ->
            val matchesQuery = query.isEmpty() ||
                    app.appName.lowercase().contains(query) ||
                    app.packageName.lowercase().contains(query)

            val matchesFilter = when (filter) {
                AppFilter.ALL -> true
                AppFilter.USER -> !app.isSystemApp
                AppFilter.SYSTEM -> app.isSystemApp
                AppFilter.LOCKED -> app.isLocked
            }

            matchesQuery && matchesFilter
        }

        val lockedCount = rawApps.count { it.isLocked }

        _uiState.update {
            it.copy(
                isLoading = false,
                apps = rawApps,
                filteredApps = filtered,
                lockedCount = lockedCount,
                totalCount = rawApps.size
            )
        }
    }

    fun openUsageStatsSettings() {
        PermissionUtils.openUsageStatsSettings(getApplication())
    }

    fun openOverlaySettings() {
        PermissionUtils.openOverlaySettings(getApplication())
    }
}
