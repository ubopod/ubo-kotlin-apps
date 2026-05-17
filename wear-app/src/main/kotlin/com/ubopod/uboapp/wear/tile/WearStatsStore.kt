package com.ubopod.uboapp.wear.tile

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ubopod.ubokotlin.models.SystemStats
import kotlinx.coroutines.flow.first

/**
 * Watch-local snapshot of `SystemStats` written by the wear-app's
 * `DeviceViewModel` and read by [UboTileService] so the tile can render
 * without an active gRPC connection. Mirrors the phone-app /
 * widget-app `WidgetDataStore` pattern but stays local to the watch —
 * the tile and the wear-app share the same process so a plain
 * DataStore is the cheapest sync channel.
 */
public object WearStatsStore {
    private val KEY_CPU: Preferences.Key<Float> = floatPreferencesKey("tile_cpu_percent")
    private val KEY_RAM: Preferences.Key<Float> = floatPreferencesKey("tile_ram_percent")
    private val KEY_TEMP: Preferences.Key<Float> = floatPreferencesKey("tile_temperature_c")
    private val KEY_CONNECTED: Preferences.Key<Boolean> = booleanPreferencesKey("tile_connected")
    private val KEY_HOST: Preferences.Key<String> = stringPreferencesKey("tile_host")
    private val KEY_UPDATED: Preferences.Key<Long> = longPreferencesKey("tile_last_updated_ms")

    public suspend fun save(context: Context, stats: SystemStats?, host: String, connected: Boolean) {
        context.tileStore.edit { prefs ->
            prefs[KEY_CPU] = stats?.cpuPercent ?: 0f
            prefs[KEY_RAM] = stats?.ramPercent ?: 0f
            stats?.temperature?.let { prefs[KEY_TEMP] = it }
            if (stats?.temperature == null) prefs.remove(KEY_TEMP)
            prefs[KEY_CONNECTED] = connected
            prefs[KEY_HOST] = host
            prefs[KEY_UPDATED] = System.currentTimeMillis()
        }
    }

    public suspend fun load(context: Context): Snapshot {
        val prefs = context.tileStore.data.first()
        return Snapshot(
            cpuPercent = prefs[KEY_CPU] ?: 0f,
            ramPercent = prefs[KEY_RAM] ?: 0f,
            temperature = prefs[KEY_TEMP],
            connected = prefs[KEY_CONNECTED] ?: false,
            host = prefs[KEY_HOST].orEmpty(),
            lastUpdatedMs = prefs[KEY_UPDATED] ?: 0L,
        )
    }

    public data class Snapshot(
        val cpuPercent: Float,
        val ramPercent: Float,
        val temperature: Float?,
        val connected: Boolean,
        val host: String,
        val lastUpdatedMs: Long,
    ) {
        public val isStale: Boolean
            get() = lastUpdatedMs == 0L || System.currentTimeMillis() - lastUpdatedMs > 5 * 60_000L
    }

    private val Context.tileStore by preferencesDataStore(name = "ubo_wear_tile")
}
