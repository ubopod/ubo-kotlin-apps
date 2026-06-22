package com.ubopod.uboapp.wear.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persisted host/port for the wear app. Slimmer than the phone app's
 * version: no onboarding flag (the watch goes straight to the
 * connection screen), no device-specific extras.
 *
 * Mirrors `phone-app/storage/Settings.kt` but namespaced under the
 * `wear_settings` DataStore so the two apps don't collide if installed
 * on the same paired device.
 */
public class UboWearSettings(private val context: Context) {

    public val savedHost: Flow<String> = context.dataStore.data
        .map { it[KEY_HOST].orEmpty() }

    public val savedPort: Flow<Int> = context.dataStore.data
        .map { it[KEY_PORT] ?: DEFAULT_PORT }

    public val savedUseTls: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_USE_TLS] ?: false }

    public suspend fun setHost(host: String) {
        context.dataStore.edit { it[KEY_HOST] = host }
    }

    public suspend fun setPort(port: Int) {
        context.dataStore.edit { it[KEY_PORT] = port }
    }

    public suspend fun setUseTls(useTls: Boolean) {
        context.dataStore.edit { it[KEY_USE_TLS] = useTls }
    }

    /**
     * Stable per-install id identifying this watch as a microphone source.
     * Sent on `startAssistantListening` and every streamed mic sample so the
     * core binds the listening session to this app's mic and ignores the
     * device's built-in mic (mirrors the Web UI's `web-ui:` audio source).
     * Generated and persisted on first call.
     */
    public suspend fun getOrCreateAudioSourceId(): String {
        var generated: String? = null
        context.dataStore.edit { prefs ->
            val existing = prefs[KEY_AUDIO_SOURCE_ID]
            if (existing.isNullOrEmpty()) {
                generated = "wear:${java.util.UUID.randomUUID()}"
                prefs[KEY_AUDIO_SOURCE_ID] = generated!!
            } else {
                generated = existing
            }
        }
        return generated!!
    }

    public companion object {
        public const val DEFAULT_PORT: Int = 50051
        private val KEY_HOST: Preferences.Key<String> = stringPreferencesKey("device_host")
        private val KEY_PORT: Preferences.Key<Int> = intPreferencesKey("device_port")
        private val KEY_USE_TLS: Preferences.Key<Boolean> = booleanPreferencesKey("device_use_tls")
        private val KEY_AUDIO_SOURCE_ID: Preferences.Key<String> = stringPreferencesKey("audio_source_id")
    }
}

private val Context.dataStore by preferencesDataStore(name = "wear_settings")
