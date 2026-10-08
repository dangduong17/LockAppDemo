package com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.data

data class AppSettings(
    val assemblyAiApiKey: String = "",
    val revAiApiKey: String = "",
    val selectedProvider: ApiProvider = ApiProvider.ASSEMBLY_AI,
    val targetPassword: String = "mở khóa",
) {
    val activeApiKey: String
        get() = when (selectedProvider) {
            ApiProvider.ASSEMBLY_AI -> assemblyAiApiKey
            ApiProvider.REV_AI -> revAiApiKey
        }
}
