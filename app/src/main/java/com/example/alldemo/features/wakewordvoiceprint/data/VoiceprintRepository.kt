package com.example.alldemo.features.wakewordvoiceprint.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.json.JSONArray

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "voiceprint_settings")

/**
 * Repository to persist master voiceprint vector in Jetpack DataStore Preferences.
 */
open class VoiceprintRepository(private val context: Context? = null) {

    companion object {
        private val MASTER_VOICEPRINT_KEY = stringPreferencesKey("master_voiceprint_vector")
    }

    /**
     * Flow emitting the master voiceprint FloatArray vector, or null if none saved.
     */
    open val masterVoiceprintFlow: Flow<FloatArray?>
        get() = context?.dataStore?.data?.map { preferences ->
            val jsonString = preferences[MASTER_VOICEPRINT_KEY] ?: return@map null
            try {
                val jsonArray = JSONArray(jsonString)
                val vector = FloatArray(jsonArray.length())
                for (i in 0 until jsonArray.length()) {
                    vector[i] = jsonArray.getDouble(i).toFloat()
                }
                vector
            } catch (e: Exception) {
                null
            }
        } ?: kotlinx.coroutines.flow.flowOf(null)

    /**
     * Saves the master voiceprint vector to DataStore.
     */
    open suspend fun saveVoiceprint(vector: FloatArray) {
        val ctx = context ?: return
        val jsonArray = JSONArray()
        for (value in vector) {
            jsonArray.put(value.toDouble())
        }
        val jsonString = jsonArray.toString()
        ctx.dataStore.edit { preferences ->
            preferences[MASTER_VOICEPRINT_KEY] = jsonString
        }
    }

    /**
     * Clears stored master voiceprint from DataStore.
     */
    open suspend fun clearVoiceprint() {
        val ctx = context ?: return
        ctx.dataStore.edit { preferences ->
            preferences.remove(MASTER_VOICEPRINT_KEY)
        }
    }

    /**
     * Reads current master voiceprint synchronously / single fetch.
     */
    open suspend fun getVoiceprintSync(): FloatArray? {
        return masterVoiceprintFlow.firstOrNull()
    }
}
