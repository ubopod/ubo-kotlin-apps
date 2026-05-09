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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ubopod.uboapp.phone.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.models.AudioDevice
import com.ubopod.ubokotlin.models.UboColor
import kotlinx.coroutines.launch

/**
 * Connected-state dashboard. Shows CPU / RAM / temperature gauges driven
 * by `client.systemStats`, plus a quick-action grid for common
 * non-navigational commands (mute, blank display, LEDs, reboot).
 *
 * Mirrors `ubo-swift-app/ubo-swift-app/Views/Dashboard/DashboardView.swift`.
 *
 * The gauge values are 0 until the upcoming `subscribeToSystemStats` lands
 * in `:lib`; once it does, the StateFlow already wired here just starts
 * receiving updates.
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

        Spacer(Modifier.height(4.dp))
        Text(
            "Quick actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(QuickActions) { action ->
                QuickActionTile(action) {
                    scope.launch { runCatching { action.invoke(viewModel) } }
                }
            }
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

@Composable
private fun QuickActionTile(action: QuickAction, onTap: () -> Unit) {
    Card(
        onClick = onTap,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(action.icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Text(
                action.label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

private fun formatTemperature(c: Float?): String =
    if (c == null) "—" else "${c.toInt()}°C"

private data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val invoke: suspend (DeviceViewModel) -> Unit,
)

private val QuickActions: List<QuickAction> = listOf(
    QuickAction("Toggle mute", Icons.Filled.VolumeOff) { it.client.toggleMute(AudioDevice.OUTPUT) },
    QuickAction("Mute mic", Icons.Filled.VolumeMute) { it.client.setMute(true, AudioDevice.INPUT) },
    QuickAction("Blank screen", Icons.Filled.Bedtime) { it.client.blankDisplay() },
    QuickAction("Wake screen", Icons.Filled.LightMode) { it.client.unblankDisplay() },
    QuickAction("Redraw", Icons.Filled.Refresh) { it.client.requestDisplayRedraw() },
    QuickAction("Rainbow LEDs", Icons.Filled.LightMode) { it.client.rainbowLEDs(rounds = 1, wait = 0.05) },
    QuickAction("Clear LEDs", Icons.Filled.Power) { it.client.clearLEDs() },
    QuickAction("Reboot", Icons.Filled.RestartAlt) { it.client.reboot() },
    QuickAction("Power off", Icons.Filled.Power) { it.client.powerOff() },
)

@Suppress("unused") // referenced from quick-action future hooks
private val LedAccent: UboColor = UboColor.White
