package com.ubopod.uboapp.phone.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ubopod.uboapp.phone.ui.controls.QuickActionsView
import com.ubopod.uboapp.phone.viewmodel.DeviceViewModel

/**
 * Connected-state dashboard: CPU / RAM / temperature gauges driven by
 * `client.systemStats`, followed by the one-tap [QuickActionsView] grid.
 * Push-to-talk lives on the Device tab (so the chat overlay is visible on
 * the same screen).
 *
 * Mirrors `ubo-swift-app/ubo-swift-app/Views/Dashboard/DashboardView.swift`
 * (gauges + QuickActions in a ScrollView).
 */
@Composable
public fun DashboardScreen(viewModel: DeviceViewModel) {
    val stats by viewModel.systemStats.collectAsStateWithLifecycle()

    // Dashboard sits inside ConnectedShell's Scaffold which already applies
    // system-bar insets via its innerPadding. Scrollable so the gauges +
    // action grid fit on shorter screens.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Dashboard",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GaugeCard(
                label = "CPU",
                percent = stats?.cpuPercent ?: 0f,
                modifier = Modifier.weight(1f),
            )
            GaugeCard(
                label = "RAM",
                percent = stats?.ramPercent ?: 0f,
                modifier = Modifier.weight(1f),
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Temperature", style = MaterialTheme.typography.labelMedium)
                    Text(
                        formatTemperature(stats?.temperature),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Clock", style = MaterialTheme.typography.labelMedium)
                    Text(
                        stats?.clock?.takeIf { it.isNotEmpty() } ?: "—",
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // One-tap device actions (chime, LED presets, sleep/wake, assistant,
        // redraw) — moved here from the former Actions tab.
        QuickActionsView(viewModel)
    }
}

@Composable
private fun GaugeCard(label: String, percent: Float, modifier: Modifier = Modifier) {
    val ratio = (percent / 100f).coerceIn(0f, 1f)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Box(
                modifier = Modifier.size(96.dp).padding(top = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 8.dp,
                )
                Text(
                    "${percent.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun formatTemperature(c: Float?): String =
    if (c == null) "—" else "${c.toInt()}°C"
