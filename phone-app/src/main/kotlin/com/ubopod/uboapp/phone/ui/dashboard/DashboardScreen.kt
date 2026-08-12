package com.ubopod.uboapp.phone.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ubopod.ubokotlin.models.SensorDeviceStatus
import com.ubopod.uboapp.phone.ui.dashboard.tiles.AppsTile
import com.ubopod.uboapp.phone.ui.dashboard.tiles.CompactSensorTile
import com.ubopod.uboapp.phone.ui.dashboard.tiles.SensorDeviceTile
import com.ubopod.uboapp.phone.ui.dashboard.tiles.SystemSection
import com.ubopod.uboapp.phone.ui.dashboard.tiles.TodaySection
import com.ubopod.uboapp.phone.viewmodel.DeviceViewModel

/**
 * Connected-state dashboard: semantic sections mirroring the Web UI
 * dashboard (ubo_app/services/090-web-ui/web-app/src/components/
 * dashboard/Dashboard.tsx) — Today (weather/date/time), System (CPU/RAM/
 * storage/network/uptime), Apps, and per-device Sensors — each its own
 * full-width card in a single column, rather than a page-level grid.
 * `LazyVerticalGrid` was tried first, but it locks every row's height to
 * its tallest cell: a short tile next to a tall one left large dead gaps
 * beneath the short one. Grouping into fewer, purpose-built cards removes
 * the row-height mismatch entirely, and happens to match how information
 * is easiest to scan on a phone besides.
 */
@Composable
public fun DashboardScreen(viewModel: DeviceViewModel) {
    val stats by viewModel.systemStats.collectAsStateWithLifecycle()

    val currentStats = stats
    if (currentStats == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Waiting for the first readings…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Text(
            "Dashboard",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TodaySection(
                weather = currentStats.weather,
                locationCity = currentStats.locationCity,
                locationCountry = currentStats.locationCountry,
                date = currentStats.date,
                clock = currentStats.clock,
            )

            SystemSection(
                cpuPercent = currentStats.cpuPercent,
                ramPercent = currentStats.ramPercent,
                temperature = currentStats.temperature,
                diskPercent = currentStats.diskPercent,
                diskUsedBytes = currentStats.diskUsedBytes,
                diskTotalBytes = currentStats.diskTotalBytes,
                networkUploadBps = currentStats.networkUploadBps,
                networkDownloadBps = currentStats.networkDownloadBps,
                bootTime = currentStats.bootTime,
                loadAverage1 = currentStats.loadAverage1,
                loadAverage5 = currentStats.loadAverage5,
                loadAverage15 = currentStats.loadAverage15,
            )

            if (currentStats.dockerApps.isNotEmpty()) {
                AppsTile(apps = currentStats.dockerApps)
            }

            // Devices with more than one reading (or a non-active status
            // message to show) keep their own full-width card; a device
            // reporting exactly one active reading is grouped instead into
            // a row of compact square tiles below, so it doesn't waste a
            // full card's width on a single number.
            val multiReadingDevices = currentStats.sensorDevices.filter { it.status != SensorDeviceStatus.ACTIVE || it.entities.size != 1 }
            val singleReadingDevices = currentStats.sensorDevices.mapNotNull { device ->
                if (device.status == SensorDeviceStatus.ACTIVE && device.entities.size == 1) {
                    device.label to device.entities.first()
                } else {
                    null
                }
            }

            for (device in multiReadingDevices) {
                SensorDeviceTile(device = device)
            }

            for (row in singleReadingDevices.chunked(4)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for ((deviceLabel, entity) in row) {
                        CompactSensorTile(
                            label = entity.name ?: deviceLabel,
                            entity = entity,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(4 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
