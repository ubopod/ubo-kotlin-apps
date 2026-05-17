package com.ubopod.uboapp.wear.ui.controls

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import com.ubopod.uboapp.wear.ui.common.WatchHapticStrength
import com.ubopod.uboapp.wear.ui.common.rememberWatchHaptic
import com.ubopod.uboapp.wear.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.models.AudioDevice
import com.ubopod.ubokotlin.models.Chime
import com.ubopod.ubokotlin.models.UboColor
import kotlinx.coroutines.launch

/**
 * Watch counterpart of the phone-app's QuickActions + a power-section.
 * Compact ScalingLazyColumn of one-tap actions; the destructive power
 * actions present an [Alert] confirm. Push-to-talk lives here so the
 * user can engage it from a single tap rather than navigating to the
 * Device tab.
 *
 * Mirrors `ubo Watch App/Views/WatchActionsView.swift` (commit
 * `4a02f40 feat(audio,power): mic mute + Watch power controls`).
 */
@Composable
public fun WatchActionsScreen(viewModel: DeviceViewModel) {
    val scope = rememberCoroutineScope()
    val haptic = rememberWatchHaptic()
    val context = LocalContext.current
    val isMicCapturing by viewModel.isMicCapturing.collectAsStateWithLifecycle()

    var pendingPower by remember { mutableStateOf<PowerAction?>(null) }

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
        if (granted) scope.launch { viewModel.toggleMicCapture() }
        else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val actions: List<ActionEntry> = listOf(
        ActionEntry(
            label = if (isMicCapturing) "Stop listening" else "Push to talk",
            icon = if (isMicCapturing) Icons.Filled.MicOff else Icons.Filled.Mic,
            onTap = {
                haptic(WatchHapticStrength.MEDIUM)
                toggleMic()
            },
        ),
        ActionEntry("Chime", Icons.Filled.NotificationsActive) {
            haptic(WatchHapticStrength.LIGHT)
            scope.launch { runCatching { viewModel.client.playChime(Chime.DONE) } }
        },
        ActionEntry("Toggle mute", Icons.Filled.VolumeOff) {
            haptic(WatchHapticStrength.LIGHT)
            scope.launch { runCatching { viewModel.client.toggleMute(AudioDevice.OUTPUT) } }
        },
        ActionEntry("Rainbow", Icons.Filled.Palette) {
            haptic(WatchHapticStrength.LIGHT)
            scope.launch { runCatching { viewModel.client.rainbowLEDs() } }
        },
        ActionEntry("LEDs off", Icons.Filled.Palette) {
            haptic(WatchHapticStrength.LIGHT)
            scope.launch { runCatching { viewModel.client.setLEDColor(UboColor.Black) } }
        },
        ActionEntry("Reboot", Icons.Filled.RestartAlt) {
            haptic(WatchHapticStrength.MEDIUM)
            pendingPower = PowerAction.REBOOT
        },
        ActionEntry("Power off", Icons.Filled.Power) {
            haptic(WatchHapticStrength.MEDIUM)
            pendingPower = PowerAction.POWER_OFF
        },
    )

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(actions) { entry ->
            Chip(
                onClick = entry.onTap,
                label = { Text(entry.label, maxLines = 1) },
                icon = { Icon(entry.icon, contentDescription = null) },
                colors = ChipDefaults.primaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    pendingPower?.let { action ->
        Dialog(
            showDialog = true,
            onDismissRequest = { pendingPower = null },
        ) {
            Alert(
                title = {
                    Text(
                        if (action == PowerAction.REBOOT) "Reboot device?" else "Power off device?",
                        style = MaterialTheme.typography.title3,
                    )
                },
                negativeButton = {
                    Chip(
                        onClick = { pendingPower = null },
                        label = { Text("Cancel") },
                        colors = ChipDefaults.secondaryChipColors(),
                    )
                },
                positiveButton = {
                    Chip(
                        onClick = {
                            pendingPower = null
                            scope.launch {
                                runCatching {
                                    when (action) {
                                        PowerAction.REBOOT -> viewModel.client.reboot()
                                        PowerAction.POWER_OFF -> viewModel.client.powerOff()
                                    }
                                }
                                viewModel.disconnect()
                            }
                        },
                        label = { Text(if (action == PowerAction.REBOOT) "Reboot" else "Power off") },
                    )
                },
            ) {
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

private enum class PowerAction { REBOOT, POWER_OFF }

private data class ActionEntry(
    val label: String,
    val icon: ImageVector,
    val onTap: () -> Unit,
)
