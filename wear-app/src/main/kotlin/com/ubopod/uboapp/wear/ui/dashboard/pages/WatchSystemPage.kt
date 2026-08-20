package com.ubopod.uboapp.wear.ui.dashboard.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ubopod.ubokotlin.models.SystemStats
import com.ubopod.uboapp.wear.ui.common.rotaryScroll
import com.ubopod.uboapp.wear.ui.dashboard.WatchCompactGauge
import com.ubopod.uboapp.wear.ui.dashboard.WatchDashboardFormat

// 3x this + spacing must fit inside the Column's ~160dp content width
// (192dp round screen minus 16dp padding per side) with real margin to
// spare — see the Row comment below for why this used to be 56dp and
// silently overflowed.
private val SystemGaugeSize = 46.dp

/**
 * Page 1 of the Wear Dashboard: CPU/RAM/Storage as compact gauges, CPU
 * temperature as a plain number+icon, and uptime as text. Load average is
 * intentionally omitted at this screen size.
 */
@Composable
public fun WatchSystemPage(stats: SystemStats) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).rotaryScroll(scrollState).padding(horizontal = 16.dp).padding(top = 30.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("System", style = MaterialTheme.typography.title3)

        // 3x GaugeSize + 2 gaps (56dp each, spacedBy 8dp) needs 184dp, but
        // this Column only has ~160dp to give it (192dp round screen minus
        // 16dp padding per side). A wrap-content Row asked to fit content
        // wider than its constraint doesn't shrink its fixed-size children
        // — Arrangement.spacedBy's centering math goes negative instead,
        // pushing the first/last item outward past the row's own reported
        // bounds. RAM (middle) renders undamaged; CPU and Disk get their
        // outer edges squeezed/clipped, which is what actually made them
        // look smaller than RAM, not any difference in gauge size. Sizing
        // gauges to something that provably fits, on a Row that actually
        // fills the available width with even spacing, fixes both the
        // clipping and guarantees RAM sits exactly centered with CPU/Disk
        // equidistant from it.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            WatchCompactGauge(
                fraction = stats.cpuPercent / 100f,
                valueText = "${stats.cpuPercent.toInt()}%",
                label = "CPU",
                color = WatchDashboardFormat.loadSeverity(stats.cpuPercent),
                size = SystemGaugeSize,
            )
            WatchCompactGauge(
                fraction = stats.ramPercent / 100f,
                valueText = "${stats.ramPercent.toInt()}%",
                label = "RAM",
                color = WatchDashboardFormat.loadSeverity(stats.ramPercent),
                size = SystemGaugeSize,
            )
            val diskPercent = stats.diskPercent
            val diskTotal = stats.diskTotalBytes
            if (diskPercent != null && diskTotal != null && diskTotal > 0) {
                WatchCompactGauge(
                    fraction = diskPercent / 100f,
                    valueText = "${diskPercent.toInt()}%",
                    // "Storage" doesn't fit this gauge's label width at
                    // this font size (truncates to "Stor…"); "Disk" does,
                    // keeping all three gauges visually matched.
                    label = "Disk",
                    color = WatchDashboardFormat.loadSeverity(diskPercent),
                    size = SystemGaugeSize,
                )
            }
        }

        (stats.temperatureDisplayValue ?: stats.temperature)?.let { temperature ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Thermostat, contentDescription = null, modifier = Modifier)
                Text(
                    "%.1f${stats.temperatureDisplayUnit ?: "°C"}".format(temperature),
                    style = MaterialTheme.typography.caption1,
                )
            }
        }

        val bootTime = stats.bootTime
        if (bootTime != null && bootTime > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier)
                Text(WatchDashboardFormat.uptime(bootTime), style = MaterialTheme.typography.caption1)
            }
        }
    }
}
