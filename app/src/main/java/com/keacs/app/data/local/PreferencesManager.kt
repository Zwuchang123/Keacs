package com.keacs.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.keacsDataStore by preferencesDataStore(name = "keacs_preferences")

class PreferencesManager private constructor(context: Context) {
    private val dataStore = context.applicationContext.keacsDataStore

    val hasWelcomed: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_HAS_WELCOMED] ?: false
    }

    val defaultRecordAccountId: Flow<Long?> = dataStore.data.map { preferences ->
        preferences[KEY_DEFAULT_RECORD_ACCOUNT_ID]
    }

    val defaultRecordType: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_DEFAULT_RECORD_TYPE] ?: DEFAULT_RECORD_TYPE
    }

    suspend fun setHasWelcomed() {
        dataStore.edit { preferences ->
            preferences[KEY_HAS_WELCOMED] = true
        }
    }

    suspend fun setDefaultRecordAccountId(accountId: Long?) {
        dataStore.edit { preferences ->
            if (accountId == null) {
                preferences.remove(KEY_DEFAULT_RECORD_ACCOUNT_ID)
            } else {
                preferences[KEY_DEFAULT_RECORD_ACCOUNT_ID] = accountId
            }
        }
    }

    suspend fun setDefaultRecordType(type: String) {
        dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_RECORD_TYPE] = type
        }
    }

    companion object {
        private val KEY_HAS_WELCOMED = booleanPreferencesKey("has_welcomed")
        private val KEY_DEFAULT_RECORD_ACCOUNT_ID = longPreferencesKey("default_record_account_id")
        private val KEY_DEFAULT_RECORD_TYPE = stringPreferencesKey("default_record_type")
        private const val DEFAULT_RECORD_TYPE = "EXPENSE"

        @Volatile
        private var instance: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager =
            instance ?: synchronized(this) {
                instance ?: PreferencesManager(context).also { instance = it }
            }
    }
}
