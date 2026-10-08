package com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rev_ai_assembly_ai_app_settings")

open class SettingsRepository(private val context: Context? = null) {

    private companion object {
        val KEY_ASSEMBLY_AI_API_KEY = stringPreferencesKey("assembly_ai_api_key")
        val KEY_REV_AI_API_KEY = stringPreferencesKey("rev_ai_api_key")
        val KEY_SELECTED_PROVIDER = stringPreferencesKey("selected_provider")
        val KEY_TARGET_PASSWORD = stringPreferencesKey("target_password")
    }

    open val settingsFlow: Flow<AppSettings> = context?.dataStore?.data?.map { preferences ->
        AppSettings(
            assemblyAiApiKey = preferences[KEY_ASSEMBLY_AI_API_KEY] ?: "",
            revAiApiKey = preferences[KEY_REV_AI_API_KEY] ?: "",
            selectedProvider = ApiProvider.fromId(preferences[KEY_SELECTED_PROVIDER]),
            targetPassword = preferences[KEY_TARGET_PASSWORD] ?: "mở khóa",
        )
    } ?: emptyFlow()

    open suspend fun updateAssemblyAiApiKey(key: String) {
        context?.dataStore?.edit { preferences ->
            preferences[KEY_ASSEMBLY_AI_API_KEY] = key.trim()
        }
    }

    open suspend fun updateRevAiApiKey(key: String) {
        context?.dataStore?.edit { preferences ->
            preferences[KEY_REV_AI_API_KEY] = key.trim()
        }
    }

    open suspend fun updateSelectedProvider(provider: ApiProvider) {
        context?.dataStore?.edit { preferences ->
            preferences[KEY_SELECTED_PROVIDER] = provider.id
        }
    }

    open suspend fun updateTargetPassword(password: String) {
        context?.dataStore?.edit { preferences ->
            preferences[KEY_TARGET_PASSWORD] = password.trim()
        }
    }

    open suspend fun updateAllSettings(
        assemblyAiApiKey: String,
        revAiApiKey: String,
        selectedProvider: ApiProvider,
        targetPassword: String
    ) {
        context?.dataStore?.edit { preferences ->
            preferences[KEY_ASSEMBLY_AI_API_KEY] = assemblyAiApiKey.trim()
            preferences[KEY_REV_AI_API_KEY] = revAiApiKey.trim()
            preferences[KEY_SELECTED_PROVIDER] = selectedProvider.id
            preferences[KEY_TARGET_PASSWORD] = targetPassword.trim()
        }
    }
}
