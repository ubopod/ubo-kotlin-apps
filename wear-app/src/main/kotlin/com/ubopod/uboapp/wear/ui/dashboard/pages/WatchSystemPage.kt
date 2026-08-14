package com.ubopod.uboapp.wear.ui.dashboard.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import com.ubopod.uboapp.wear.ui.dashboard.WatchCompactGauge
import com.ubopod.uboapp.wear.ui.dashboard.WatchDashboardFormat

/**
 * Page 1 of the Wear Dashboard: CPU/RAM/Storage as compact gauges, CPU
 * temperature as a plain number+icon, and uptime as text. Load average is
 * intentionally omitted at this screen size.
 */
@Composable
public fun WatchSystemPage(stats: SystemStats) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("System", style = MaterialTheme.typography.title3)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            WatchCompactGauge(
                fraction = stats.cpuPercent / 100f,
                valueText = "${stats.cpuPercent.toInt()}%",
                label = "CPU",
                color = WatchDashboardFormat.loadSeverity(stats.cpuPercent),
            )
            WatchCompactGauge(
                fraction = stats.ramPercent / 100f,
                valueText = "${stats.ramPercent.toInt()}%",
                label = "RAM",
                color = WatchDashboardFormat.loadSeverity(stats.ramPercent),
            )
            val diskPercent = stats.diskPercent
            val diskTotal = stats.diskTotalBytes
            if (diskPercent != null && diskTotal != null && diskTotal > 0) {
                WatchCompactGauge(
                    fraction = diskPercent / 100f,
                    valueText = "${diskPercent.toInt()}%",
                    label = "Storage",
                    color = WatchDashboardFormat.loadSeverity(diskPercent),
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
