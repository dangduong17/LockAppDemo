package com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.ui.unlock

import com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.data.ApiProvider

enum class UnlockStatus {
    IDLE,
    CONNECTING,
    LISTENING,
    UNLOCKED,
    FAILED,
    ERROR
}

data class UnlockUiState(
    val status: UnlockStatus = UnlockStatus.IDLE,
    val currentTranscript: String = "",
    val targetPassword: String = "mở khóa",
    val selectedProvider: ApiProvider = ApiProvider.ASSEMBLY_AI,
    val errorMessage: String? = null,
    val hasApiKey: Boolean = true
)
