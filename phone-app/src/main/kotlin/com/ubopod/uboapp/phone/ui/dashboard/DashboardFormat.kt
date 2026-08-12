package com.ubopod.uboapp.phone.ui.dashboard

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Formatting helpers ported from the Web UI's `format.ts`
 * (ubo_app/services/090-web-ui/web-app/src/components/dashboard/format.ts).
 */
public object DashboardFormat {
    private val byteUnits = listOf("B", "KB", "MB", "GB", "TB")

    /** Humanize a byte count, e.g. 1536 -> "1.5 KB". */
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

    /** Humanize a transfer rate in bytes/second. */
    public fun rate(bytesPerSecond: Float): String = "${bytes(bytesPerSecond.toLong())}/s"

    /**
     * Render an uptime from the device's boot time (epoch seconds). Clamped
     * to zero rather than rendering a negative span if the client clock is
     * behind the device's.
     */
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

    /** Format a sensor reading at the precision its registry entry asks for. */
    public fun reading(value: Float?, precision: Long?): String {
        if (value == null) return "—"
        if (precision != null) return "%.${max(0L, precision)}f".format(value)
        return if (abs(value) < 100) "%.1f".format(value) else value.roundToInt().toString()
    }
}
