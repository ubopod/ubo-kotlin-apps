package com.ubopod.uboapp.wear.ui.dashboard

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Compact formatting helpers for the Wear Dashboard pages. A Wear-local
 * counterpart to the phone app's `DashboardFormat.kt` — separate per
 * target, matching the existing iOS/watchOS split in this codebase.
 */
public object WatchDashboardFormat {
    private val byteUnits = listOf("B", "KB", "MB", "GB", "TB")

    public fun bytes(count: Long): String {
        var value = max(0L, count).toDouble()
        var unit = 0
        while (value >= 1024 && unit < byteUnits.size - 1) {
            value /= 1024
            unit += 1
        }
        val digits = if (value < 10 && unit > 0) 1 else 0
        return "%.${digits}f %s".format(value, byteUnits[unit])
    }

    /** Compact uptime, e.g. "3d 4h" / "4h 12m" / "12m". */
    public fun uptime(bootTime: Float): String {
        if (bootTime <= 0f) return "—"
        val seconds = max(0.0, System.currentTimeMillis() / 1000.0 - bootTime)
        val days = (seconds / 86400).toInt()
        val hours = ((seconds % 86400) / 3600).toInt()
        val minutes = ((seconds % 3600) / 60).toInt()
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }

    public fun loadSeverity(percent: Float): Color = when {
        percent >= 90f -> Color(0xFFD0_3B_3B)
        percent >= 80f -> Color(0xFFEC_83_5A)
        percent >= 60f -> Color(0xFFFA_B2_19)
        else -> Color(0xFF0C_A3_0C)
    }

    public fun reading(value: Float?, precision: Long?): String {
        if (value == null) return "—"
        if (precision != null) return "%.${max(0L, precision)}f".format(value)
        return if (abs(value) < 100) "%.1f".format(value) else value.roundToInt().toString()
    }
}
