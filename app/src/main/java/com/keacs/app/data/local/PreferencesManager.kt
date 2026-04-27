package com.keacs.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.keacsDataStore by preferencesDataStore(name = "keacs_preferences")

class PreferencesManager private constructor(context: Context) {
    private val dataStore = context.applicationContext.keacsDataStore

    val hasWelcomed: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_HAS_WELCOMED] ?: false
    }

    suspend fun setHasWelcomed() {
        dataStore.edit { preferences ->
            preferences[KEY_HAS_WELCOMED] = true
        }
    }

    companion object {
        private val KEY_HAS_WELCOMED = booleanPreferencesKey("has_welcomed")

        @Volatile
        private var instance: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager =
            instance ?: synchronized(this) {
                instance ?: PreferencesManager(context).also { instance = it }
            }
    }
}
