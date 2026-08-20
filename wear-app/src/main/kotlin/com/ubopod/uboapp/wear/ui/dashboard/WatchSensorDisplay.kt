package com.ubopod.uboapp.wear.ui.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.ui.graphics.vector.ImageVector

/** Wear-local counterpart to the phone app's `SensorDisplay.kt`, condensed to the common device classes. */
public data class WatchSensorSpec(val icon: ImageVector, val range: ClosedFloatingPointRange<Float>?)

public object WatchSensorDisplay {
    private val byDeviceClass: Map<String, WatchSensorSpec> = mapOf(
        "temperature" to WatchSensorSpec(Icons.Filled.Thermostat, -10f..50f),
        "humidity" to WatchSensorSpec(Icons.Filled.Opacity, 0f..100f),
        "pressure" to WatchSensorSpec(Icons.Filled.Speed, 950f..1050f),
        "illuminance" to WatchSensorSpec(Icons.Filled.WbSunny, 0f..1000f),
        "carbon_dioxide" to WatchSensorSpec(Icons.Filled.Air, 400f..2000f),
        "aqi" to WatchSensorSpec(Icons.Filled.Air, 1f..5f),
        // PMSA003I's particulate-matter readings (registry.default.json) —
        // typical indoor/ambient µg/m³ scale, coarse enough to fill the
        // gauge meaningfully without needing a precise AQI breakpoint
        // table for a small watch ring.
        "pm1" to WatchSensorSpec(Icons.Filled.Air, 0f..100f),
        "pm25" to WatchSensorSpec(Icons.Filled.Air, 0f..100f),
        "pm10" to WatchSensorSpec(Icons.Filled.Air, 0f..150f),
        // ENS160's TVOC entity — 0-2200 ppb covers "excellent" through
        // "poor" on ENS160's own IAQ scale; everyday indoor readings stay
        // well under this, unhealthy/severe territory starts above it.
        "volatile_organic_compounds_parts" to WatchSensorSpec(Icons.Filled.Air, 0f..2200f),
    )
    private val fallback = WatchSensorSpec(Icons.Outlined.Circle, null)

    public fun spec(key: String, deviceClass: String?): WatchSensorSpec =
        (deviceClass?.let { byDeviceClass[it] }) ?: fallback

    public fun rangeFraction(value: Float, range: ClosedFloatingPointRange<Float>): Float {
        val span = range.endInclusive - range.start
        if (span <= 0f) return 0f
        return (value - range.start) / span
    }
}
