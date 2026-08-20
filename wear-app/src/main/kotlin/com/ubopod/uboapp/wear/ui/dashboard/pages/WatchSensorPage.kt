package com.ubopod.uboapp.wear.ui.dashboard.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.text.style.TextAlign
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

// Same reasoning as the System page's SystemGaugeSize: 3x this + spacing
// must fit inside the Column's ~160dp content width with margin to spare.
// Shared across pages so every gauge in the app reads as the same size.
private val SensorGaugeSize = 46.dp

/**
 * One page per connected sensor device — the Wear Dashboard's page count
 * grows/shrinks as sensors are added or removed, since pages are
 * generated from `stats.sensorDevices` directly (see WatchDashboardScreen).
 */
@Composable
public fun WatchSensorPage(device: SensorDeviceState) {
    val scrollState = rememberScrollState()
    // A device with exactly one gauge-style reading is short enough to
    // never need scrolling, so it reads much better on a round screen
    // vertically centered as a block — genuinely concentric with the
    // bezel — rather than pinned to the top like a page that might
    // overflow.
    val isSingleGauge = device.status == SensorDeviceStatus.ACTIVE &&
        device.entities.size == 1 &&
        isGaugeEligible(device.entities.first())

    val content: @Composable ColumnScope.() -> Unit = {
        Text(
            device.label,
            style = MaterialTheme.typography.title3,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            // A device name that's just long enough to stay under the
            // Column's own width constraint (so ellipsis never triggers)
            // can still be wider than what's actually safe at this row's
            // height on a round screen — the chord here is narrower than
            // the full 160dp the flat side padding assumes. This trims a
            // little more width symmetrically so it's the ellipsis that
            // kicks in, not the bezel clipping a letter.
            modifier = Modifier.padding(horizontal = 6.dp),
        )

        when (device.status) {
            SensorDeviceStatus.ACTIVE -> for (chunk in chunkEntities(device.entities)) {
                when (chunk) {
                    is EntityChunk.GaugeRun -> GaugeRow(chunk.entities)
                    is EntityChunk.Plain -> PlainEntityRow(chunk.entity)
                }
            }
            SensorDeviceStatus.ERROR -> Text("Sensor error", style = MaterialTheme.typography.caption2)
            SensorDeviceStatus.UNSUPPORTED -> Text("Unsupported", style = MaterialTheme.typography.caption2)
            SensorDeviceStatus.AMBIGUOUS -> Text("Ambiguous reading", style = MaterialTheme.typography.caption2)
            SensorDeviceStatus.UNSPECIFIED -> Text("—", style = MaterialTheme.typography.caption2)
        }
    }

    if (isSingleGauge) {
        // No verticalScroll here on purpose: it measures its content with
        // an unbounded height, so Arrangement.spacedBy(_, CenterVertically)
        // has no extra space to center within — the Column would still
        // report fillMaxSize() to its parent, but content stays pinned to
        // the top exactly as if no centering were requested at all. Since
        // this branch is guaranteed short (one title + one gauge), it
        // doesn't need to scroll, so it can skip straight to a real
        // fixed-height Column where centering actually has something to
        // center against.
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            content = content,
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).rotaryScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 30.dp, bottom = 8.dp),
            // Centers the title and any gauge rows (their Columns are
            // only as wide as their own content, so without this they
            // hug the left edge instead of sitting centered on the round
            // screen). The label/value rows below stay full-width
            // regardless, since fillMaxWidth on a Row overrides the
            // parent's horizontal alignment.
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
    }
}

private fun isGaugeEligible(entity: SensorEntityReading): Boolean {
    val spec = WatchSensorDisplay.spec(entity.key, entity.deviceClass)
    return spec.range != null && entity.value != null
}

private sealed interface EntityChunk {
    data class GaugeRun(val entities: List<SensorEntityReading>) : EntityChunk
    data class Plain(val entity: SensorEntityReading) : EntityChunk
}

/**
 * Groups consecutive gauge-eligible entities into runs of up to 3 (mirrors
 * the System page's CPU/RAM/Storage row), interspersed with the
 * non-gauge entities rendered individually in their original order — e.g.
 * PMSA003I's PM1.0/PM2.5/PM10 become one 3-up gauge row; ENS160's
 * eCO2/TVOC/AQI become one 3-up row followed by its plain "Data Validity"
 * row, matching the Web UI's per-card layout.
 */
private fun chunkEntities(entities: List<SensorEntityReading>): List<EntityChunk> {
    val chunks = mutableListOf<EntityChunk>()
    var i = 0
    while (i < entities.size) {
        val entity = entities[i]
        if (isGaugeEligible(entity)) {
            val run = mutableListOf<SensorEntityReading>()
            while (i < entities.size && run.size < 3 && isGaugeEligible(entities[i])) {
                run.add(entities[i])
                i++
            }
            chunks.add(EntityChunk.GaugeRun(run))
        } else {
            chunks.add(EntityChunk.Plain(entity))
            i++
        }
    }
    return chunks
}

@Composable
private fun GaugeRow(entities: List<SensorEntityReading>) {
    // A lone gauge (a lone reading tucked between two plain rows, not the
    // page's only entity) still has the full row width to itself via
    // SpaceEvenly, so it gets the same generous label width the truly
    // single-gauge page uses. A run of 2-3 side-by-side needs its label
    // constrained to its own slice of the row instead, same as the System
    // page's CPU/RAM/Disk.
    val labelWidth = if (entities.size == 1) 150.dp else SensorGaugeSize
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        for (entity in entities) {
            val spec = WatchSensorDisplay.spec(entity.key, entity.deviceClass)
            val range = spec.range ?: continue
            val entityValue = entity.value ?: continue
            WatchCompactGauge(
                fraction = WatchSensorDisplay.rangeFraction(entityValue, range),
                valueText = WatchDashboardFormat.reading(entity.displayValue ?: entity.value, entity.precision),
                // Server-driven — never hardcode a unit string here.
                unit = entity.displayUnit ?: entity.unit,
                label = entity.name ?: entity.key,
                color = MaterialTheme.colors.primary,
                size = SensorGaugeSize,
                labelWidth = labelWidth,
            )
        }
    }
}

@Composable
private fun PlainEntityRow(entity: SensorEntityReading) {
    val spec = WatchSensorDisplay.spec(entity.key, entity.deviceClass)
    val label = entity.name ?: entity.key
    val valueText = WatchDashboardFormat.reading(entity.displayValue ?: entity.value, entity.precision)

    Row(
        // A page with a gauge row above pushes later plain rows further
        // down — close enough to the bottom curve on some multi-entity
        // devices that the flat 16dp column padding isn't quite enough
        // clearance, and the value text's right edge clips against the
        // bezel. This extra margin is the same round-safe adjustment the
        // title above uses, applied here for the same reason.
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
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
