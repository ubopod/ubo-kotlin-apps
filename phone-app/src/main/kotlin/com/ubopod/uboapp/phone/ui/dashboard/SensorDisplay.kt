package com.ubopod.uboapp.phone.ui.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * How to render a sensor reading, keyed by the Home Assistant
 * `device_class` the sensor registry assigns it. Mirrors the Web UI's
 * `sensor-display.ts`. A range is deliberately absent for anything with
 * no natural bounds — those render as plain stats, since a meter implies
 * a limit and inventing one would be a lie.
 */
public data class SensorDisplaySpec(val icon: ImageVector, val range: ClosedFloatingPointRange<Float>?)

public object SensorDisplay {
    private val byDeviceClass: Map<String, SensorDisplaySpec> = mapOf(
        "temperature" to SensorDisplaySpec(Icons.Filled.Thermostat, -10f..50f),
        "humidity" to SensorDisplaySpec(Icons.Filled.Opacity, 0f..100f),
        "pressure" to SensorDisplaySpec(Icons.Filled.Speed, 950f..1050f),
        "illuminance" to SensorDisplaySpec(Icons.Filled.WbSunny, 0f..1000f),
        "carbon_dioxide" to SensorDisplaySpec(Icons.Filled.Air, 400f..2000f),
        "volatile_organic_compounds_parts" to SensorDisplaySpec(Icons.Filled.Air, 0f..1000f),
        "aqi" to SensorDisplaySpec(Icons.Filled.Air, 1f..5f),
        "pm1" to SensorDisplaySpec(Icons.Filled.Air, 0f..100f),
        "pm25" to SensorDisplaySpec(Icons.Filled.Air, 0f..100f),
        "pm10" to SensorDisplaySpec(Icons.Filled.Air, 0f..100f),
        "distance" to SensorDisplaySpec(Icons.Filled.Straighten, null),
    )

    private val byKey: Map<String, SensorDisplaySpec> = mapOf(
        "gas_resistance" to SensorDisplaySpec(Icons.Filled.Air, null),
        "voc_index" to SensorDisplaySpec(Icons.Filled.Air, null),
        "altitude" to SensorDisplaySpec(Icons.Filled.Height, null),
    )

    private val fallback = SensorDisplaySpec(Icons.Outlined.Circle, null)

    public fun spec(key: String, deviceClass: String?): SensorDisplaySpec =
        (deviceClass?.let { byDeviceClass[it] }) ?: byKey[key] ?: fallback

    /** Position a reading within its range, 0-1, for the meter fill. */
    public fun rangeFraction(value: Float, range: ClosedFloatingPointRange<Float>): Float {
        val span = range.endInclusive - range.start
        if (span <= 0f) return 0f
        return (value - range.start) / span
    }
}
