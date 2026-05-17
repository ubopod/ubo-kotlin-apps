package com.ubopod.uboapp.phone.storage

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
 * Persisted host/port + onboarding-completion flag for the phone app.
 * Mirrors Swift `DeviceViewModel.savedHost` / `.savedPort` (UserDefaults)
 * and `@AppStorage("hasCompletedOnboarding")`.
 */
public class UboSettings(private val context: Context) {

    public val savedHost: Flow<String> = context.dataStore.data
        .map { it[KEY_HOST].orEmpty() }

    public val savedPort: Flow<Int> = context.dataStore.data
        .map { it[KEY_PORT] ?: DEFAULT_PORT }

    public val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_ONBOARDED] ?: false }

    /**
     * Stable per-install id used by [com.ubopod.uboapp.phone.service.CameraSourceRegistrar]
     * to register this phone as a remote camera source. Empty until
     * [getOrCreateCameraSourceId] is called the first time.
     */
    public val cameraSourceId: Flow<String> = context.dataStore.data
        .map { it[KEY_CAMERA_SOURCE_ID].orEmpty() }

    public suspend fun setHost(host: String) {
        context.dataStore.edit { it[KEY_HOST] = host }
    }

    public suspend fun setPort(port: Int) {
        context.dataStore.edit { it[KEY_PORT] = port }
    }

    public suspend fun markOnboardingComplete() {
        context.dataStore.edit { it[KEY_ONBOARDED] = true }
    }

    /**
     * Read the stored camera source id, or generate-and-persist a UUID
     * on first call. Cheap to invoke; the DataStore call is suspending.
     */
    public suspend fun getOrCreateCameraSourceId(): String {
        var generated: String? = null
        context.dataStore.edit { prefs ->
            val existing = prefs[KEY_CAMERA_SOURCE_ID]
            if (existing.isNullOrEmpty()) {
                generated = java.util.UUID.randomUUID().toString()
                prefs[KEY_CAMERA_SOURCE_ID] = generated!!
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
        private val KEY_ONBOARDED: Preferences.Key<Boolean> = booleanPreferencesKey("has_completed_onboarding")
        private val KEY_CAMERA_SOURCE_ID: Preferences.Key<String> = stringPreferencesKey("camera_source_id")
    }
}

private val Context.dataStore by preferencesDataStore(name = "ubo_settings")
