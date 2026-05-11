package com.keacs.app.data.local

import android.content.Context
import com.keacs.app.BuildConfig
import com.keacs.app.domain.agent.AgentSettings
import com.keacs.app.domain.agent.AgentModelServiceMode
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.util.UUID

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

    val agentSettings: Flow<AgentSettings> = dataStore.data.map { preferences ->
        AgentSettings(
            enabled = preferences[KEY_AGENT_ENABLED] ?: false,
            serviceMode = AgentModelServiceMode.fromStorageValue(
                preferences[KEY_AGENT_MODEL_SERVICE_MODE],
            ),
            officialServiceUrl = preferences[KEY_AGENT_OFFICIAL_SERVICE_URL]
                ?: BuildConfig.AGENT_OFFICIAL_SERVICE_URL,
            customBaseUrl = preferences[KEY_AGENT_CUSTOM_BASE_URL].orEmpty(),
            customApiKey = preferences[KEY_AGENT_CUSTOM_API_KEY].orEmpty(),
            customModelName = preferences[KEY_AGENT_CUSTOM_MODEL_NAME].orEmpty(),
            deviceId = preferences[KEY_AGENT_DEVICE_ID].orEmpty(),
            dataScope = preferences[KEY_AGENT_DATA_SCOPE] ?: DEFAULT_AGENT_DATA_SCOPE,
        )
    }

    val agentConversationSnapshot: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_AGENT_CONVERSATION_SNAPSHOT].orEmpty()
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

    suspend fun setAgentEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_AGENT_ENABLED] = enabled
        }
    }

    suspend fun setAgentModelServiceMode(mode: AgentModelServiceMode) {
        dataStore.edit { preferences ->
            preferences[KEY_AGENT_MODEL_SERVICE_MODE] = mode.storageValue
        }
    }

    suspend fun setAgentCustomBaseUrl(baseUrl: String) {
        dataStore.edit { preferences ->
            preferences[KEY_AGENT_CUSTOM_BASE_URL] = baseUrl.trim()
        }
    }

    suspend fun setAgentCustomApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            preferences[KEY_AGENT_CUSTOM_API_KEY] = apiKey.trim()
        }
    }

    suspend fun setAgentCustomModelName(modelName: String) {
        dataStore.edit { preferences ->
            preferences[KEY_AGENT_CUSTOM_MODEL_NAME] = modelName.trim()
        }
    }

    suspend fun setAgentConversationSnapshot(snapshot: String) {
        dataStore.edit { preferences ->
            if (snapshot.isBlank()) {
                preferences.remove(KEY_AGENT_CONVERSATION_SNAPSHOT)
            } else {
                preferences[KEY_AGENT_CONVERSATION_SNAPSHOT] = snapshot
            }
        }
    }

    suspend fun clearAgentConversationSnapshot() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_AGENT_CONVERSATION_SNAPSHOT)
        }
    }

    suspend fun ensureAgentDeviceId() {
        dataStore.edit { preferences ->
            if (preferences[KEY_AGENT_DEVICE_ID].isNullOrBlank()) {
                preferences[KEY_AGENT_DEVICE_ID] = sha256(UUID.randomUUID().toString())
            }
        }
    }

    companion object {
        private val KEY_HAS_WELCOMED = booleanPreferencesKey("has_welcomed")
        private val KEY_DEFAULT_RECORD_ACCOUNT_ID = longPreferencesKey("default_record_account_id")
        private val KEY_DEFAULT_RECORD_TYPE = stringPreferencesKey("default_record_type")
        private val KEY_AGENT_ENABLED = booleanPreferencesKey("agent_enabled")
        private val KEY_AGENT_MODEL_SERVICE_MODE = stringPreferencesKey("agent_model_service_mode")
        private val KEY_AGENT_OFFICIAL_SERVICE_URL = stringPreferencesKey("agent_official_service_url")
        private val KEY_AGENT_CUSTOM_BASE_URL = stringPreferencesKey("agent_custom_base_url")
        private val KEY_AGENT_CUSTOM_API_KEY = stringPreferencesKey("agent_custom_api_key")
        private val KEY_AGENT_CUSTOM_MODEL_NAME = stringPreferencesKey("agent_custom_model_name")
        private val KEY_AGENT_DEVICE_ID = stringPreferencesKey("agent_device_id")
        private val KEY_AGENT_DATA_SCOPE = stringPreferencesKey("agent_data_scope")
        private val KEY_AGENT_CONVERSATION_SNAPSHOT = stringPreferencesKey("agent_conversation_snapshot")
        private const val DEFAULT_RECORD_TYPE = "EXPENSE"
        private const val DEFAULT_AGENT_DATA_SCOPE = "minimal"

        @Volatile
        private var instance: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager =
            instance ?: synchronized(this) {
                instance ?: PreferencesManager(context).also { instance = it }
            }

        private fun sha256(value: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
