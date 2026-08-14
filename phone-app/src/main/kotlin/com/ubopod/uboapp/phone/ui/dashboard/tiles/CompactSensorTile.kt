package com.ubopod.uboapp.phone.ui.dashboard.tiles

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ubopod.ubokotlin.models.SensorEntityReading
import com.ubopod.uboapp.phone.ui.dashboard.DashboardColors
import com.ubopod.uboapp.phone.ui.dashboard.DashboardFormat
import com.ubopod.uboapp.phone.ui.dashboard.DashboardGauge
import com.ubopod.uboapp.phone.ui.dashboard.SensorDisplay

private val TILE_HEIGHT = 108.dp

/**
 * A small square tile for a sensor device that reports exactly one
 * reading (e.g. an ambient-light or single-temperature sensor). Giving
 * each of these its own full-width [DashboardCard] wasted most of the
 * card's width on nothing; grouped instead into a row of up to four,
 * matching multi-reading devices' density. Fixed height so a row of
 * tiles always aligns — the earlier full-grid layout's dead-space bug
 * came from letting the grid guess at wildly varying tile heights.
 */
@Composable
public fun CompactSensorTile(label: String, entity: SensorEntityReading, modifier: Modifier = Modifier) {
    val spec = SensorDisplay.spec(entity.key, entity.deviceClass)
    // `fraction` is computed against the raw (Celsius/metric) value and
    // SensorDisplay's Celsius/metric-scaled range table — the gauge fill
    // percentage doesn't depend on which unit the text shows. The text
    // itself uses the server-converted display value/unit.
    val valueText = DashboardFormat.reading(entity.displayValue ?: entity.value, entity.precision)
    val displayUnit = entity.displayUnit ?: entity.unit
    val range = spec.range
    val value = entity.value

    Card(
        modifier = modifier.fillMaxWidth().height(TILE_HEIGHT),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (range != null && value != null) {
                DashboardGauge(
                    fraction = SensorDisplay.rangeFraction(value, range),
                    valueText = valueText,
                    unit = displayUnit,
                    color = DashboardColors.gaugeAccent,
                    size = 48.dp,
                    strokeWidth = 5.dp,
                )
            } else {
                Icon(
                    imageVector = spec.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(valueText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (displayUnit != null) {
                    Text(displayUnit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
