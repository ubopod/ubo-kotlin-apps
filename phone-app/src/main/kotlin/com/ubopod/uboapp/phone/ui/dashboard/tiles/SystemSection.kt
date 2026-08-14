package com.ubopod.uboapp.phone.ui.dashboard.tiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ubopod.uboapp.phone.ui.dashboard.DashboardCard
import com.ubopod.uboapp.phone.ui.dashboard.DashboardColors
import com.ubopod.uboapp.phone.ui.dashboard.DashboardFormat
import com.ubopod.uboapp.phone.ui.dashboard.DashboardGauge
import com.ubopod.uboapp.phone.ui.dashboard.DashboardStat

/**
 * CPU + RAM + Storage + Network + Uptime merged into one card, for the
 * same reason as [TodaySection]: a page-level grid of separately-sized
 * system tiles produces large dead gaps when row heights don't match.
 */
@Composable
public fun SystemSection(
    cpuPercent: Float,
    ramPercent: Float,
    temperature: Float?,
    temperatureUnit: String? = null,
    diskPercent: Float?,
    diskUsedBytes: Long?,
    diskTotalBytes: Long?,
    networkUploadBps: Float?,
    networkDownloadBps: Float?,
    bootTime: Float?,
    loadAverage1: Float?,
    loadAverage5: Float?,
    loadAverage15: Float?,
) {
    val hasDisk = diskTotalBytes != null && diskTotalBytes > 0

    DashboardCard(title = "System", icon = Icons.Filled.DeveloperBoard) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                GaugeColumn("CPU", cpuPercent, DashboardColors.loadSeverity(cpuPercent))
                GaugeColumn("RAM", ramPercent, DashboardColors.loadSeverity(ramPercent))
                if (hasDisk) {
                    val diskPct = diskPercent ?: 0f
                    GaugeColumn("Storage", diskPct, DashboardColors.loadSeverity(diskPct))
                }
            }

            HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (temperature != null) {
                    DashboardStat(
                        label = "Temperature",
                        value = "%.1f".format(temperature),
                        unit = temperatureUnit ?: "°C",
                        icon = Icons.Filled.Thermostat,
                    )
                }
                if (diskUsedBytes != null && diskTotalBytes != null && hasDisk) {
                    DashboardStat(label = "Storage used", value = "${DashboardFormat.bytes(diskUsedBytes)} / ${DashboardFormat.bytes(diskTotalBytes)}")
                }
                DashboardStat(
                    label = "Upload",
                    value = DashboardFormat.rate(networkUploadBps ?: 0f),
                    icon = Icons.Filled.ArrowUpward,
                    valueColor = DashboardColors.good,
                )
                DashboardStat(
                    label = "Download",
                    value = DashboardFormat.rate(networkDownloadBps ?: 0f),
                    icon = Icons.Filled.ArrowDownward,
                    valueColor = DashboardColors.neutralAccent,
                )
                if (bootTime != null && bootTime > 0) {
                    DashboardStat(label = "Uptime", value = DashboardFormat.uptime(bootTime), icon = Icons.Filled.History)
                    if (loadAverage1 != null && loadAverage5 != null && loadAverage15 != null) {
                        DashboardStat(label = "Load avg", value = "%.2f  %.2f  %.2f".format(loadAverage1, loadAverage5, loadAverage15))
                    }
                }
            }
        }
    }
}

@Composable
private fun GaugeColumn(label: String, percent: Float, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            DashboardGauge(fraction = percent / 100f, valueText = "${percent.toInt()}", unit = "%", color = color, size = 60.dp)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
