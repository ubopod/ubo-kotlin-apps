package com.ubopod.uboapp.wear.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ubopod.uboapp.wear.viewmodel.DeviceViewModel

/**
 * Compact watch dashboard — CPU / RAM circular gauges + temperature +
 * clock readout. Mirrors `ubo Watch App/Views/WatchDashboardView.swift`.
 */
@Composable
public fun WatchDashboardScreen(viewModel: DeviceViewModel) {
    val stats by viewModel.systemStats.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Dashboard", style = MaterialTheme.typography.title3)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            MiniGauge("CPU", stats?.cpuPercent ?: 0f)
            MiniGauge("RAM", stats?.ramPercent ?: 0f)
        }

        val temp = stats?.temperature
        Text(
            if (temp != null) "${temp.toInt()}°C" else "—°C",
            style = MaterialTheme.typography.caption1,
            color = MaterialTheme.colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            stats?.clock?.takeIf { it.isNotEmpty() } ?: "—",
            style = MaterialTheme.typography.body2,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun MiniGauge(label: String, percent: Float) {
    val ratio = (percent / 100f).coerceIn(0f, 1f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.caption2)
        Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = ratio,
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 5.dp,
            )
            Text(
                "${percent.toInt()}%",
                style = MaterialTheme.typography.caption1,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
