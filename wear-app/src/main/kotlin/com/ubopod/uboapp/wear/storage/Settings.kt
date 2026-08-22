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
 * One entry in [UboWearSettings.recentConnections] — a pod the user has
 * connected to before, so they can switch back without re-typing
 * host/port/TLS. Mirrors `phone-app/storage/Settings.kt`'s `RecentConnection`.
 */
public data class RecentConnection(
    val host: String,
    val port: Int,
    val useTls: Boolean,
)

/** Number of recent connections remembered — matches [RecentConnection] slots below. */
private const val RECENT_CONNECTIONS_LIMIT = 3

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

    /** The last [RECENT_CONNECTIONS_LIMIT] distinct (host, port) pairs, most recent first. */
    public val recentConnections: Flow<List<RecentConnection>> = context.dataStore.data
        .map { it.toRecentConnections() }

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
     * Move (or insert) `host:port` to the front of [recentConnections],
     * refreshing its TLS setting to whatever was just used, and trims back
     * down to [RECENT_CONNECTIONS_LIMIT].
     */
    public suspend fun recordRecentConnection(host: String, port: Int, useTls: Boolean) {
        context.dataStore.edit { prefs ->
            val updated = (
                listOf(RecentConnection(host, port, useTls)) +
                    prefs.toRecentConnections().filterNot { it.host == host && it.port == port }
                ).take(RECENT_CONNECTIONS_LIMIT)
            for (index in 0 until RECENT_CONNECTIONS_LIMIT) {
                val entry = updated.getOrNull(index)
                if (entry == null) {
                    prefs.remove(KEY_RECENT_HOST[index])
                    prefs.remove(KEY_RECENT_PORT[index])
                    prefs.remove(KEY_RECENT_TLS[index])
                } else {
                    prefs[KEY_RECENT_HOST[index]] = entry.host
                    prefs[KEY_RECENT_PORT[index]] = entry.port
                    prefs[KEY_RECENT_TLS[index]] = entry.useTls
                }
            }
        }
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
        public const val DEFAULT_PORT: Int = 50053
        private val KEY_HOST: Preferences.Key<String> = stringPreferencesKey("device_host")
        private val KEY_PORT: Preferences.Key<Int> = intPreferencesKey("device_port")
        private val KEY_USE_TLS: Preferences.Key<Boolean> = booleanPreferencesKey("device_use_tls")
        private val KEY_AUDIO_SOURCE_ID: Preferences.Key<String> = stringPreferencesKey("audio_source_id")

        // DataStore Preferences has no native list type, so the last N
        // connections live in N parallel indexed key sets rather than one
        // serialized blob — matches the phone app's approach.
        private val KEY_RECENT_HOST: List<Preferences.Key<String>> =
            List(RECENT_CONNECTIONS_LIMIT) { stringPreferencesKey("recent_host_$it") }
        private val KEY_RECENT_PORT: List<Preferences.Key<Int>> =
            List(RECENT_CONNECTIONS_LIMIT) { intPreferencesKey("recent_port_$it") }
        private val KEY_RECENT_TLS: List<Preferences.Key<Boolean>> =
            List(RECENT_CONNECTIONS_LIMIT) { booleanPreferencesKey("recent_tls_$it") }
    }

    private fun Preferences.toRecentConnections(): List<RecentConnection> =
        (0 until RECENT_CONNECTIONS_LIMIT).mapNotNull { index ->
            val host = this[KEY_RECENT_HOST[index]] ?: return@mapNotNull null
            val port = this[KEY_RECENT_PORT[index]] ?: return@mapNotNull null
            val useTls = this[KEY_RECENT_TLS[index]] ?: false
            RecentConnection(host, port, useTls)
        }
}

private val Context.dataStore by preferencesDataStore(name = "wear_settings")
