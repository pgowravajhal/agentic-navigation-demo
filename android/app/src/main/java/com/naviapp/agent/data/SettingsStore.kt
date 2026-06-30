package com.naviapp.agent.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Persists app settings using Jetpack DataStore.
 */
class SettingsStore(private val context: Context) {

    companion object {
        private val BACKEND_URL_KEY = stringPreferencesKey("backend_url")
        const val DEFAULT_URL = "http://10.0.2.2:8000"
    }

    val backendUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[BACKEND_URL_KEY] ?: DEFAULT_URL
    }

    suspend fun saveBackendUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[BACKEND_URL_KEY] = url
        }
    }
}
