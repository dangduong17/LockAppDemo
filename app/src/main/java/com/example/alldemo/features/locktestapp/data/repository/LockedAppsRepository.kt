package com.example.alldemo.features.locktestapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "locked_apps_prefs")

class LockedAppsRepository(private val context: Context) {

    private val lockedPackagesKey = stringSetPreferencesKey("locked_packages")
    private val appPinKey = stringPreferencesKey("app_pin")
    private val serviceEnabledKey = booleanPreferencesKey("is_service_enabled")

    companion object {
        const val DEFAULT_PIN = "1234"
    }

    val lockedPackagesFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[lockedPackagesKey] ?: emptySet()
    }

    val pinFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[appPinKey] ?: DEFAULT_PIN
    }

    val isServiceEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[serviceEnabledKey] ?: false
    }

    suspend fun toggleLock(packageName: String) {
        context.dataStore.edit { preferences ->
            val currentSet = preferences[lockedPackagesKey] ?: emptySet()
            if (currentSet.contains(packageName)) {
                preferences[lockedPackagesKey] = currentSet - packageName
            } else {
                preferences[lockedPackagesKey] = currentSet + packageName
            }
        }
    }

    suspend fun setLocked(packageName: String, isLocked: Boolean) {
        context.dataStore.edit { preferences ->
            val currentSet = preferences[lockedPackagesKey] ?: emptySet()
            if (isLocked) {
                preferences[lockedPackagesKey] = currentSet + packageName
            } else {
                preferences[lockedPackagesKey] = currentSet - packageName
            }
        }
    }

    suspend fun setPin(newPin: String) {
        context.dataStore.edit { preferences ->
            preferences[appPinKey] = newPin
        }
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[serviceEnabledKey] = enabled
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.remove(lockedPackagesKey)
            preferences.remove(appPinKey)
            preferences.remove(serviceEnabledKey)
        }
    }
}
