package com.example.alldemo.features.wakewordvoiceprint.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository to persist application settings and thresholds in Jetpack DataStore Preferences.
 */
open class SettingsRepository(private val context: Context? = null) {

    companion object {
        val KEY_KWS_THRESHOLD = floatPreferencesKey("kws_threshold")
        val KEY_VOICEPRINT_THRESHOLD = floatPreferencesKey("voiceprint_threshold")
        val KEY_SELECTED_WAKEWORD = stringPreferencesKey("selected_wakeword")

        const val DEFAULT_KWS_THRESHOLD = 0.65f
        const val DEFAULT_VOICEPRINT_THRESHOLD = 0.70f
        const val DEFAULT_WAKEWORD = "Hey Assistant"
    }

    /**
     * Flow emitting KWS threshold (default 0.65f).
     */
    open val kwsThresholdFlow: Flow<Float>
        get() = context?.dataStore?.data?.map { preferences ->
            preferences[KEY_KWS_THRESHOLD] ?: DEFAULT_KWS_THRESHOLD
        } ?: kotlinx.coroutines.flow.flowOf(DEFAULT_KWS_THRESHOLD)

    /**
     * Flow emitting Voiceprint similarity threshold (default 0.70f).
     */
    open val voiceprintThresholdFlow: Flow<Float>
        get() = context?.dataStore?.data?.map { preferences ->
            preferences[KEY_VOICEPRINT_THRESHOLD] ?: DEFAULT_VOICEPRINT_THRESHOLD
        } ?: kotlinx.coroutines.flow.flowOf(DEFAULT_VOICEPRINT_THRESHOLD)

    /**
     * Flow emitting selected wake-word string (default "Hey Assistant").
     */
    open val selectedWakewordFlow: Flow<String>
        get() = context?.dataStore?.data?.map { preferences ->
            preferences[KEY_SELECTED_WAKEWORD] ?: DEFAULT_WAKEWORD
        } ?: kotlinx.coroutines.flow.flowOf(DEFAULT_WAKEWORD)

    /**
     * Updates KWS threshold setting.
     */
    open suspend fun setKwsThreshold(threshold: Float) {
        val ctx = context ?: return
        ctx.dataStore.edit { preferences ->
            preferences[KEY_KWS_THRESHOLD] = threshold
        }
    }

    /**
     * Updates Voiceprint threshold setting.
     */
    open suspend fun setVoiceprintThreshold(threshold: Float) {
        val ctx = context ?: return
        ctx.dataStore.edit { preferences ->
            preferences[KEY_VOICEPRINT_THRESHOLD] = threshold
        }
    }

    /**
     * Updates selected wake-word setting.
     */
    open suspend fun setSelectedWakeword(wakeword: String) {
        val ctx = context ?: return
        ctx.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_WAKEWORD] = wakeword
        }
    }
}
