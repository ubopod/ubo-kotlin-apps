package com.ubopod.uboapp.phone.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ubopod.uboapp.phone.viewmodel.DeviceViewModel
import kotlinx.coroutines.launch

/**
 * Connected-state dashboard. Shows CPU / RAM / temperature gauges driven
 * by `client.systemStats` plus a push-to-talk button. One-tap device
 * actions live on the Actions tab.
 *
 * Mirrors `ubo-swift-app/ubo-swift-app/Views/Dashboard/DashboardView.swift`.
 */
@Composable
public fun DashboardScreen(viewModel: DeviceViewModel) {
    val stats by viewModel.systemStats.collectAsStateWithLifecycle()
    val isMicCapturing by viewModel.isMicCapturing.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Mic permission flow: ask on first toggle; subsequent toggles just
    // start/stop. The launcher result is consumed exactly once per launch.
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) scope.launch { viewModel.toggleMicCapture() }
    }
    val toggleMic: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            scope.launch { viewModel.toggleMicCapture() }
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Dashboard sits inside ConnectedShell's Scaffold which already
    // applies system-bar insets via its innerPadding. No safeDrawingPadding
    // needed here — see DeviceScreen for the same rationale.
    Column(
        modifier = Modifier
            .fillMaxSize()
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

        Spacer(Modifier.height(4.dp))

        // Mic toggle — manual entry into the assistant capture flow.
        // Starting flips on AudioRecord and dispatches
        // AssistantStartListening; stopping does the inverse.
        Button(
            onClick = toggleMic,
            colors = if (isMicCapturing) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            } else {
                ButtonDefaults.buttonColors()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                if (isMicCapturing) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = null,
            )
            Spacer(Modifier.size(8.dp))
            Text(if (isMicCapturing) "Stop listening" else "Push to talk")
        }
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
