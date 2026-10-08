package com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.data.ApiProvider
import com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.data.AppSettings
import com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val assemblyAiApiKey: String = "",
    val revAiApiKey: String = "",
    val selectedProvider: ApiProvider = ApiProvider.ASSEMBLY_AI,
    val targetPassword: String = "mở khóa",
    val isEdited: Boolean = false,
    val isSaved: Boolean = false,
    val statusMessage: String? = null,
)

class SettingsViewModel(
    application: Application,
    private val repository: SettingsRepository = SettingsRepository(application)
) : AndroidViewModel(application) {

    val savedSettings: StateFlow<AppSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            savedSettings.collect { settings ->
                _uiState.update { currentState ->
                    if (!currentState.isEdited) {
                        currentState.copy(
                            assemblyAiApiKey = settings.assemblyAiApiKey,
                            revAiApiKey = settings.revAiApiKey,
                            selectedProvider = settings.selectedProvider,
                            targetPassword = settings.targetPassword
                        )
                    } else currentState
                }
            }
        }
    }

    fun onAssemblyAiApiKeyChanged(key: String) {
        _uiState.update { it.copy(assemblyAiApiKey = key, isEdited = true, statusMessage = null) }
    }

    fun onRevAiApiKeyChanged(key: String) {
        _uiState.update { it.copy(revAiApiKey = key, isEdited = true, statusMessage = null) }
    }

    fun onProviderSelected(provider: ApiProvider) {
        _uiState.update { it.copy(selectedProvider = provider, isEdited = true, statusMessage = null) }
    }

    fun onTargetPasswordChanged(password: String) {
        _uiState.update { it.copy(targetPassword = password, isEdited = true, statusMessage = null) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            val state = _uiState.value
            repository.updateAllSettings(
                assemblyAiApiKey = state.assemblyAiApiKey,
                revAiApiKey = state.revAiApiKey,
                selectedProvider = state.selectedProvider,
                targetPassword = state.targetPassword
            )
            _uiState.update {
                it.copy(
                    isEdited = false,
                    isSaved = true,
                    statusMessage = "Đã lưu cấu hình thành công!"
                )
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(application) as T
        }
    }
}
