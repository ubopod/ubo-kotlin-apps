package com.ubopod.uboapp.phone.ui.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ubopod.uboapp.phone.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.models.HomeViewData
import kotlinx.coroutines.launch

/**
 * Renders the device's home view: a system-summary card on top with
 * CPU / RAM / volume bars, followed by a vertical list of menu items
 * sharing the same row layout as [MenuViewRenderer]. Tapping a row
 * forwards `selectMenuItem(label = …)` over gRPC.
 *
 * Mirrors the home-rendering branch of
 * `ubo-swift-app/ubo-swift-app/Views/Device/DeviceView.swift`.
 */
@Composable
public fun HomeViewRenderer(data: HomeViewData, viewModel: DeviceViewModel) {
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth()) {
        SystemSummary(cpu = data.cpuPercent, ram = data.ramPercent, volume = data.volumeLevel)

        Spacer(Modifier.height(12.dp))

        if (data.menuItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Home menu is empty.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(data.menuItems) { item ->
                    MenuItemRow(item) {
                        scope.launch {
                            runCatching {
                                // Home-view items often have empty or
                                // non-unique labels (the row is icon-only
                                // on the Pi panel), so MenuChooseByLabel
                                // can't resolve "Notifications" or
                                // "Power" — the action no-ops and the
                                // home view stays put, which looks like
                                // "went back to main menu" to the user.
                                // Prefer icon-based dispatch when an
                                // icon is present, matching the Swift
                                // port's DeviceView.swift:181-188.
                                if (item.icon.isNotEmpty()) {
                                    viewModel.client.selectMenuItemByIcon(item.icon)
                                } else {
                                    viewModel.client.selectMenuItem(label = item.label)
                                }
                            }
                        }
                    }
                    if (item != data.menuItems.last()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemSummary(cpu: Float, ram: Float, volume: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatRow("CPU", cpu)
            StatRow("RAM", ram)
            StatRow("Volume", volume * 100f)
        }
    }
}

@Composable
private fun StatRow(label: String, percent: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 12.dp),
        )
        LinearProgressIndicator(
            progress = { (percent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.weight(1f).height(8.dp),
        )
        Text(
            "${percent.toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
