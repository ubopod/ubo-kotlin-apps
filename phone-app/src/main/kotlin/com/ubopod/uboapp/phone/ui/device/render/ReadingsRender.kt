package com.ubopod.uboapp.phone.ui.device.render

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ubopod.uboapp.phone.ui.common.stringListProp
import com.ubopod.uboapp.phone.ui.dashboard.DashboardColors
import com.ubopod.uboapp.phone.ui.dashboard.DashboardGauge
import com.ubopod.uboapp.phone.ui.dashboard.DashboardStat
import com.ubopod.uboapp.phone.ui.dashboard.SensorDisplay
import com.ubopod.ubokotlin.models.RenderViewData

private data class Reading(
    val label: String,
    val value: String,
    val unit: String?,
    val key: String,
    val deviceClass: String?,
)

/**
 * Render a sensor's readings — entities with a [SensorDisplay] range meter
 * as a row of gauges, the rest as plain stat rows. Reuses the same
 * `SensorDisplay`/`DashboardGauge` machinery as the Dashboard's
 * `SensorDeviceTile`, keyed through the same `key`/`device_class` the
 * server now sends. Mirrors the Swift `ReadingsRenderView`.
 */
@Composable
public fun ReadingsRender(data: RenderViewData) {
    val labels = data.stringListProp("labels")
    val values = data.stringListProp("values")
    val units = data.stringListProp("units")
    val keys = data.stringListProp("keys")
    val deviceClasses = data.stringListProp("device_classes")

    val readings = labels.indices.map { index ->
        Reading(
            label = labels[index],
            value = values.getOrElse(index) { "" },
            unit = units.getOrNull(index)?.ifEmpty { null },
            key = keys.getOrElse(index) { "" },
            deviceClass = deviceClasses.getOrNull(index)?.ifEmpty { null },
        )
    }

    if (readings.isEmpty()) {
        Text(
            "No readings yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val metered = readings.filter { SensorDisplay.spec(it.key, it.deviceClass).range != null && it.value.toFloatOrNull() != null }
    val plain = readings.filter { reading -> metered.none { it.key == reading.key } }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (metered.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (row in metered.chunked(3)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        for (reading in row) GaugeReading(reading)
                    }
                }
            }
        }
        if (plain.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (reading in plain) {
                        val spec = SensorDisplay.spec(reading.key, reading.deviceClass)
                        DashboardStat(label = reading.label, value = reading.value, unit = reading.unit, icon = spec.icon)
                    }
                }
            }
        }
    }
}

@Composable
private fun GaugeReading(reading: Reading) {
    val spec = SensorDisplay.spec(reading.key, reading.deviceClass)
    val range = spec.range
    val value = reading.value.toFloatOrNull()
    if (range == null || value == null) return

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DashboardGauge(
            fraction = SensorDisplay.rangeFraction(value, range),
            valueText = reading.value,
            unit = reading.unit,
            color = DashboardColors.gaugeAccent,
            size = 72.dp,
            strokeWidth = 7.dp,
        )
        Text(
            reading.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
