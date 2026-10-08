package com.example.alldemo.features.deepgramnova2apiwebsocket.data.pref

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

interface AppPreferences {
    fun getApiKey(): String
    fun saveApiKey(apiKey: String)
    fun getPassword(): String
    fun savePassword(password: String)
}

class DefaultAppPreferences(context: Context) : AppPreferences {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "deepgram_app_prefs"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_PASSWORD = "password"
        const val DEFAULT_PASSWORD = "mở khóa"
    }

    override fun getApiKey(): String {
        return sharedPreferences.getString(KEY_API_KEY, "") ?: ""
    }

    override fun saveApiKey(apiKey: String) {
        sharedPreferences.edit { putString(KEY_API_KEY, apiKey) }
    }

    override fun getPassword(): String {
        val saved = sharedPreferences.getString(KEY_PASSWORD, DEFAULT_PASSWORD)
        return if (saved.isNullOrBlank()) DEFAULT_PASSWORD else saved
    }

    override fun savePassword(password: String) {
        sharedPreferences.edit { putString(KEY_PASSWORD, password) }
    }
}
