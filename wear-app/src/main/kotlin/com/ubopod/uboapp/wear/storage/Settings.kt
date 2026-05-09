package com.ubopod.uboapp.wear.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
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

    public suspend fun setHost(host: String) {
        context.dataStore.edit { it[KEY_HOST] = host }
    }

    public suspend fun setPort(port: Int) {
        context.dataStore.edit { it[KEY_PORT] = port }
    }

    public companion object {
        public const val DEFAULT_PORT: Int = 50051
        private val KEY_HOST: Preferences.Key<String> = stringPreferencesKey("device_host")
        private val KEY_PORT: Preferences.Key<Int> = intPreferencesKey("device_port")
    }
}

private val Context.dataStore by preferencesDataStore(name = "wear_settings")
