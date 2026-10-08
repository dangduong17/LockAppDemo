package com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.data

enum class ApiProvider(val id: String, val displayName: String) {
    ASSEMBLY_AI("assembly_ai", "AssemblyAI"),
    REV_AI("rev_ai", "Rev AI");

    companion object {
        fun fromId(id: String?): ApiProvider {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: ASSEMBLY_AI
        }
    }
}
