package com.ubopod.uboapp.wear.ui.dashboard.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ubopod.ubokotlin.models.SensorDeviceState
import com.ubopod.ubokotlin.models.SensorDeviceStatus
import com.ubopod.ubokotlin.models.SensorEntityReading
import com.ubopod.uboapp.wear.ui.common.rotaryScroll
import com.ubopod.uboapp.wear.ui.dashboard.WatchCompactGauge
import com.ubopod.uboapp.wear.ui.dashboard.WatchDashboardFormat
import com.ubopod.uboapp.wear.ui.dashboard.WatchSensorDisplay

/**
 * One page per connected sensor device — the Wear Dashboard's page count
 * grows/shrinks as sensors are added or removed, since pages are
 * generated from `stats.sensorDevices` directly (see WatchDashboardScreen).
 */
@Composable
public fun WatchSensorPage(device: SensorDeviceState) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).rotaryScroll(scrollState).padding(horizontal = 16.dp).padding(top = 30.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // A few extra dp on the title only — see WatchAppsPage for why.
        Text(
            device.label,
            style = MaterialTheme.typography.title3,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 6.dp),
        )

        when (device.status) {
            SensorDeviceStatus.ACTIVE -> for (entity in device.entities) {
                EntityRow(entity)
            }
            SensorDeviceStatus.ERROR -> Text("Sensor error", style = MaterialTheme.typography.caption2)
            SensorDeviceStatus.UNSUPPORTED -> Text("Unsupported", style = MaterialTheme.typography.caption2)
            SensorDeviceStatus.AMBIGUOUS -> Text("Ambiguous reading", style = MaterialTheme.typography.caption2)
            SensorDeviceStatus.UNSPECIFIED -> Text("—", style = MaterialTheme.typography.caption2)
        }
    }
}

@Composable
private fun EntityRow(entity: SensorEntityReading) {
    val spec = WatchSensorDisplay.spec(entity.key, entity.deviceClass)
    val label = entity.name ?: entity.key
    val valueText = WatchDashboardFormat.reading(entity.displayValue ?: entity.value, entity.precision)
    val entityValue = entity.value

    if (spec.range != null && entityValue != null) {
        WatchCompactGauge(
            fraction = WatchSensorDisplay.rangeFraction(entityValue, spec.range),
            valueText = valueText,
            label = label,
            color = MaterialTheme.colors.primary,
            size = 40.dp,
        )
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(spec.icon, contentDescription = null, modifier = Modifier.size(12.dp))
            Text(
                label,
                style = MaterialTheme.typography.caption2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                valueText + ((entity.displayUnit ?: entity.unit)?.let { " $it" } ?: ""),
                style = MaterialTheme.typography.caption2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
