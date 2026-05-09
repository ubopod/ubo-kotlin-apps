package com.ubopod.uboapp.widget

/**
 * Snapshot of the device's system stats published by the phone-app and
 * read by the home-screen widget.
 *
 * Mirrors `ubo-swift-app/Shared/SharedSystemStats.swift`. On iOS the
 * struct goes through an App Group's UserDefaults; on Android we use
 * a process-safe DataStore (see [WidgetDataStore]).
 */
public data class SharedSystemStats(
    val cpuPercent: Float = 0f,
    val ramPercent: Float = 0f,
    val temperature: Float? = null,
    val isConnected: Boolean = false,
    val deviceHost: String = "",
    val lastUpdatedEpochMs: Long = 0L,
) {
    /**
     * `true` when the snapshot is older than 5 minutes — the launcher
     * may show a "stale" state instead of the cached numbers.
     */
    public val isStale: Boolean
        get() {
            if (lastUpdatedEpochMs == 0L) return true
            return System.currentTimeMillis() - lastUpdatedEpochMs > STALE_THRESHOLD_MS
        }

    public companion object {
        public const val STALE_THRESHOLD_MS: Long = 5 * 60 * 1000
    }
}
