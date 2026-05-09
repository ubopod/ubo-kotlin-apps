package com.ubopod.uboapp.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Process-safe reader/writer for [SharedSystemStats]. The phone-app's
 * `DeviceViewModel` writes via [save]; the Glance widget reads via [load]
 * (or collects from [flow]) inside its `provideGlance` callback.
 *
 * DataStore Preferences serialises a single proto file under
 * `<app_files>/datastore/`, which is accessible to both the host app
 * process and any widget callback that runs under the same UID.
 */
public object WidgetDataStore {

    public fun flow(context: Context): Flow<SharedSystemStats> =
        context.applicationContext.widgetDataStore.data.map { it.toStats() }

    public suspend fun load(context: Context): SharedSystemStats =
        flow(context).first()

    public suspend fun save(context: Context, stats: SharedSystemStats) {
        context.applicationContext.widgetDataStore.edit { prefs ->
            prefs[KEY_CPU] = stats.cpuPercent
            prefs[KEY_RAM] = stats.ramPercent
            stats.temperature?.let { prefs[KEY_TEMP] = it } ?: prefs.remove(KEY_TEMP)
            prefs[KEY_CONNECTED] = stats.isConnected
            prefs[KEY_HOST] = stats.deviceHost
            prefs[KEY_UPDATED] = stats.lastUpdatedEpochMs
        }
    }

    public suspend fun clear(context: Context) {
        context.applicationContext.widgetDataStore.edit { it.clear() }
    }

    private fun Preferences.toStats(): SharedSystemStats = SharedSystemStats(
        cpuPercent = get(KEY_CPU) ?: 0f,
        ramPercent = get(KEY_RAM) ?: 0f,
        temperature = get(KEY_TEMP),
        isConnected = get(KEY_CONNECTED) ?: false,
        deviceHost = get(KEY_HOST).orEmpty(),
        lastUpdatedEpochMs = get(KEY_UPDATED) ?: 0L,
    )

    private val KEY_CPU = floatPreferencesKey("cpu_percent")
    private val KEY_RAM = floatPreferencesKey("ram_percent")
    private val KEY_TEMP = floatPreferencesKey("temperature_c")
    private val KEY_CONNECTED = booleanPreferencesKey("is_connected")
    private val KEY_HOST = stringPreferencesKey("device_host")
    private val KEY_UPDATED = longPreferencesKey("last_updated_ms")
}

private val Context.widgetDataStore by preferencesDataStore(name = "widget_stats")
