package com.ubopod.uboapp.phone.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ubopod.uboapp.phone.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.models.AudioDevice
import com.ubopod.ubokotlin.models.DisplayBlankTimeout
import com.ubopod.ubokotlin.models.UboColor
import kotlinx.coroutines.launch

/**
 * Mirror of `ubo-swift-app/.../Views/Settings/DeviceSettingsView.swift`.
 *
 * Six sections — Connection / RGB LED Ring / Display / Audio / Power /
 * Disconnect — bound to the connected client. The Audio section's volume
 * slider follows `systemStats.playbackVolume` live whenever the user
 * isn't currently dragging, matching the Swift port's editing-aware
 * binding so the hardware buttons and other clients keep the slider in
 * sync without fighting the drag.
 */
@Composable
public fun DeviceSettingsScreen(viewModel: DeviceViewModel) {
    var showWifiQrCodeScreen by remember { mutableStateOf(false) }
    if (showWifiQrCodeScreen) {
        WifiQrCodeScreen(onBack = { showWifiQrCodeScreen = false })
        return
    }

    val stats by viewModel.systemStats.collectAsStateWithLifecycle()
    val host by viewModel.savedHost.collectAsStateWithLifecycle()
    val port by viewModel.savedPort.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var pendingPowerAction by remember { mutableStateOf<PowerAction?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        SettingsSection(title = "Connection") {
            LabeledRow("Host", host.ifEmpty { "—" })
            LabeledRow("Port", port.toString())
            LabeledRow("Status", if (isConnected) "Connected" else "Disconnected")
        }

        SettingsSection(title = "RGB LED ring") { LedSection(viewModel, scope) }
        SettingsSection(title = "Display") { DisplaySection(viewModel, scope) }
        SettingsSection(title = "Audio", footer = AUDIO_FOOTER) {
            AudioSection(viewModel, stats, scope)
        }
        SettingsSection(title = "Power", footer = "These actions disconnect you from the device.") {
            PowerSection(onTrigger = { pendingPowerAction = it })
        }
        SettingsSection(
            title = "Wi-Fi Connection",
            footer = "Generate a QR code with Wi-Fi credentials that the Ubo Pod's camera can scan to join a network.",
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showWifiQrCodeScreen = true },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Generate QR Code", modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, contentDescription = null)
            }
        }
        SettingsSection(title = null) {
            OutlinedButton(
                onClick = { viewModel.triggerDisconnect() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Disconnect") }
        }
    }

    pendingPowerAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingPowerAction = null },
            title = { Text("Confirm action") },
            text = {
                Text(
                    when (action) {
                        PowerAction.REBOOT -> "Reboot the device?"
                        PowerAction.POWER_OFF -> "Power off the device?"
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingPowerAction = null
                    scope.launch {
                        runCatching {
                            when (action) {
                                PowerAction.REBOOT -> viewModel.client.reboot()
                                PowerAction.POWER_OFF -> viewModel.client.powerOff()
                            }
                        }
                        viewModel.disconnect()
                    }
                }) {
                    Text(if (action == PowerAction.REBOOT) "Reboot" else "Power off")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPowerAction = null }) { Text("Cancel") }
            },
        )
    }
}

// ---- Sections ----

@Composable
private fun LedSection(viewModel: DeviceViewModel, scope: kotlinx.coroutines.CoroutineScope) {
    // Local UI state — there's no per-LED state slice in the proto, so we
    // hold these intermediate values until the user releases the
    // slider / toggles the switch (mirrors Swift's @State approach).
    var brightness by remember { mutableFloatStateOf(1f) }
    var enabled by remember { mutableStateOf(true) }

    SettingsRow {
        Text("LEDs enabled", modifier = Modifier.weight(1f))
        Switch(checked = enabled, onCheckedChange = {
            enabled = it
            scope.launch { runCatching { viewModel.client.setLEDEnabled(it) } }
        })
    }
    SettingsColumn {
        Text("Brightness: ${(brightness * 100).toInt()}%")
        Slider(
            value = brightness,
            onValueChange = { brightness = it },
            onValueChangeFinished = {
                scope.launch { runCatching { viewModel.client.setLEDBrightness(brightness) } }
            },
            valueRange = 0f..1f,
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ColorButton("Red", Color(0xFFEF4444)) {
            scope.launch { runCatching { viewModel.client.setLEDColor(UboColor.Red) } }
        }
        ColorButton("Green", Color(0xFF10B981)) {
            scope.launch { runCatching { viewModel.client.setLEDColor(UboColor.Green) } }
        }
        ColorButton("Blue", Color(0xFF3B82F6)) {
            scope.launch { runCatching { viewModel.client.setLEDColor(UboColor.Blue) } }
        }
        OutlinedButton(
            onClick = { scope.launch { runCatching { viewModel.client.clearLEDs() } } },
            modifier = Modifier.weight(1f),
        ) { Text("Off") }
    }
}

@Composable
private fun ColorButton(label: String, accent: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = accent),
    ) { Text(label, color = Color.White) }
}

@Composable
private fun DisplaySection(viewModel: DeviceViewModel, scope: kotlinx.coroutines.CoroutineScope) {
    var timeout by remember { mutableStateOf(DisplayBlankTimeout.FIVE_MINUTES) }
    var menuOpen by remember { mutableStateOf(false) }

    SettingsRow {
        Text("Auto-sleep timeout", modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { menuOpen = true }) {
            Text(timeoutLabel(timeout))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DisplayBlankTimeout.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(timeoutLabel(option)) },
                    onClick = {
                        timeout = option
                        menuOpen = false
                        scope.launch {
                            runCatching { viewModel.client.setDisplayTimeout(option) }
                        }
                    },
                )
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = { scope.launch { runCatching { viewModel.client.blankDisplay() } } },
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Filled.Bedtime, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Sleep now")
        }
        OutlinedButton(
            onClick = { scope.launch { runCatching { viewModel.client.unblankDisplay() } } },
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Filled.WbSunny, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Wake")
        }
    }
}

@Composable
private fun AudioSection(
    viewModel: DeviceViewModel,
    stats: com.ubopod.ubokotlin.models.SystemStats?,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    var slider by remember { mutableFloatStateOf(stats?.playbackVolume ?: 0f) }
    var isEditing by remember { mutableStateOf(false) }

    // Sync slider to server-confirmed volume whenever the user isn't
    // touching it. Otherwise dragging would fight live updates.
    LaunchedEffect(stats?.playbackVolume, isEditing) {
        if (!isEditing) {
            stats?.playbackVolume?.let { slider = it }
        }
    }

    val muted = stats?.isPlaybackMute == true

    SettingsColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = volumeIcon(muted, slider),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Text("Volume", modifier = Modifier.weight(1f))
            Text(
                "${(slider * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = slider,
            valueRange = 0f..1f,
            onValueChange = {
                isEditing = true
                slider = it
            },
            onValueChangeFinished = {
                val target = slider
                isEditing = false
                scope.launch {
                    runCatching { viewModel.client.setVolume(target, AudioDevice.OUTPUT) }
                }
            },
        )
    }
    SettingsRow {
        Text("Mute", modifier = Modifier.weight(1f))
        Switch(
            checked = muted,
            onCheckedChange = { newValue ->
                scope.launch {
                    runCatching { viewModel.client.setMute(newValue, AudioDevice.OUTPUT) }
                }
            },
        )
    }
    SettingsRow {
        Text("Mute device microphone", modifier = Modifier.weight(1f))
        Switch(
            checked = stats?.isCaptureMute == true,
            onCheckedChange = { newValue ->
                scope.launch {
                    runCatching { viewModel.client.setMute(newValue, AudioDevice.INPUT) }
                }
            },
        )
    }
    OutlinedButton(
        onClick = {
            scope.launch {
                runCatching { viewModel.client.playChime(com.ubopod.ubokotlin.models.Chime.DONE) }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Play test chime") }
}

@Composable
private fun PowerSection(onTrigger: (PowerAction) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = { onTrigger(PowerAction.REBOOT) },
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Filled.RestartAlt, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Reboot")
        }
        Button(
            onClick = { onTrigger(PowerAction.POWER_OFF) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Filled.Power, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Power off")
        }
    }
}

// ---- Helpers ----

private enum class PowerAction { REBOOT, POWER_OFF }

@Composable
private fun SettingsSection(
    title: String?,
    footer: String? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        title?.let {
            Text(
                it,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) { content() }
        }
        footer?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun SettingsColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun timeoutLabel(t: DisplayBlankTimeout): String = when (t) {
    DisplayBlankTimeout.ONE_MINUTE -> "1 minute"
    DisplayBlankTimeout.FIVE_MINUTES -> "5 minutes"
    DisplayBlankTimeout.TEN_MINUTES -> "10 minutes"
    DisplayBlankTimeout.THIRTY_MINUTES -> "30 minutes"
    DisplayBlankTimeout.ONE_HOUR -> "1 hour"
    DisplayBlankTimeout.OFF -> "Never"
}

private fun volumeIcon(muted: Boolean, level: Float) = when {
    muted || level <= 0f -> Icons.Filled.VolumeOff
    level < 0.5f -> Icons.Filled.VolumeDown
    else -> Icons.Filled.VolumeUp
}

private const val AUDIO_FOOTER: String =
    "Volume changes propagate to and from the device — hardware buttons, the Watch, and " +
        "other connected clients stay in sync. Muting the device microphone stops the Pi from " +
        "listening; the push-to-talk button on the Dashboard streams audio from this phone separately."
