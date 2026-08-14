package com.ubopod.uboapp.phone.ui.dashboard.tiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ubopod.ubokotlin.models.SensorDeviceState
import com.ubopod.ubokotlin.models.SensorDeviceStatus
import com.ubopod.ubokotlin.models.SensorEntityReading
import com.ubopod.uboapp.phone.ui.dashboard.DashboardCard
import com.ubopod.uboapp.phone.ui.dashboard.DashboardColors
import com.ubopod.uboapp.phone.ui.dashboard.DashboardFormat
import com.ubopod.uboapp.phone.ui.dashboard.DashboardGauge
import com.ubopod.uboapp.phone.ui.dashboard.DashboardStat
import com.ubopod.uboapp.phone.ui.dashboard.SensorDisplay

/**
 * One tile per connected sensor device, mirroring the Web UI's
 * `SensorCards.tsx`. Entities with a natural range (per [SensorDisplay])
 * render as a row of mini gauges (chunked 3-per-row so it doesn't
 * overflow the card on narrow screens); the rest render as plain stats.
 */
@Composable
public fun SensorDeviceTile(device: SensorDeviceState, modifier: Modifier = Modifier) {
    val metered = device.entities.filter { SensorDisplay.spec(it.key, it.deviceClass).range != null && it.value != null }
    val plain = device.entities.filter { entity -> metered.none { it.key == entity.key } }

    DashboardCard(title = device.label, icon = Icons.Filled.Sensors, modifier = modifier) {
        when (device.status) {
            SensorDeviceStatus.ACTIVE -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (row in metered.chunked(3)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        for (entity in row) GaugeEntity(entity)
                    }
                }
                for (entity in plain) StatEntity(entity)
            }
            SensorDeviceStatus.ERROR -> Text("Sensor error", style = MaterialTheme.typography.bodySmall, color = DashboardColors.critical)
            SensorDeviceStatus.UNSUPPORTED -> Text(
                "Unsupported sensor",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SensorDeviceStatus.AMBIGUOUS -> Text(
                "Ambiguous reading",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SensorDeviceStatus.UNSPECIFIED -> Text("—", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun GaugeEntity(entity: SensorEntityReading) {
    val spec = SensorDisplay.spec(entity.key, entity.deviceClass)
    val range = spec.range
    val value = entity.value
    if (range == null || value == null) return

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DashboardGauge(
            fraction = SensorDisplay.rangeFraction(value, range),
            valueText = DashboardFormat.reading(entity.displayValue ?: entity.value, entity.precision),
            unit = entity.displayUnit ?: entity.unit,
            color = DashboardColors.gaugeAccent,
            size = 56.dp,
            strokeWidth = 5.dp,
        )
        Text(
            entity.name ?: entity.key,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun StatEntity(entity: SensorEntityReading) {
    val spec = SensorDisplay.spec(entity.key, entity.deviceClass)
    DashboardStat(
        label = entity.name ?: entity.key,
        value = DashboardFormat.reading(entity.displayValue ?: entity.value, entity.precision),
        unit = entity.displayUnit ?: entity.unit,
        icon = spec.icon,
    )
}
