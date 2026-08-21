package com.ubopod.uboapp.wear.ui.controls

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Waves
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import com.ubopod.uboapp.wear.ui.common.WatchHapticStrength
import com.ubopod.uboapp.wear.ui.common.rememberWatchHaptic
import com.ubopod.uboapp.wear.ui.common.rotaryScroll
import com.ubopod.uboapp.wear.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.models.AudioDevice
import com.ubopod.ubokotlin.models.Chime
import kotlinx.coroutines.launch

/**
 * Actions tab — Audio / LEDs / Display / Assistant / Power, one flat
 * scrolling list. Folds Volume in as a pushed sub-screen (left-edge
 * swipe to come back) rather than giving it its own top-level tab,
 * matching `WatchActionsView.swift`'s section grouping exactly — no
 * D-pad/remote control section either, since watchOS has no such screen
 * at all.
 */
@Composable
public fun WatchActionsScreen(viewModel: DeviceViewModel) {
    var showVolume by remember { mutableStateOf(false) }

    if (showVolume) {
        SwipeToDismissBox(onDismissed = { showVolume = false }) {
            WatchVolumeScreen(viewModel)
        }
    } else {
        ActionsListScreen(viewModel, onVolumeClick = { showVolume = true })
    }
}

@Composable
private fun ActionsListScreen(viewModel: DeviceViewModel, onVolumeClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    val haptic = rememberWatchHaptic()
    val context = LocalContext.current
    val isMicCapturing by viewModel.isMicCapturing.collectAsStateWithLifecycle()
    val stats by viewModel.systemStats.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()

    var pendingPower by remember { mutableStateOf<PowerAction?>(null) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) scope.launch { viewModel.toggleMicCapture(WATCH_ASSISTANT_TRIGGER) }
    }
    val toggleMic: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) scope.launch { viewModel.toggleMicCapture(WATCH_ASSISTANT_TRIGGER) }
        else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val volumePercent = stats?.playbackVolume?.let { "${(it * 100).toInt()}%" } ?: "—"
    val isPlaybackMuted = stats?.isPlaybackMute == true
    val isCaptureMuted = stats?.isCaptureMute == true

    val actions: List<ActionEntry> = listOf(
        // Audio
        ActionEntry(
            label = "Volume",
            icon = Icons.Filled.VolumeUp,
            trailingText = volumePercent,
        ) { onVolumeClick() },
        ActionEntry("Play Chime", Icons.Filled.NotificationsActive) {
            haptic(WatchHapticStrength.LIGHT)
            scope.launch { runCatching { viewModel.client.playChime(Chime.DONE) } }
        },
        ActionEntry(if (isPlaybackMuted) "Unmute" else "Mute", Icons.Filled.VolumeOff) {
            haptic(WatchHapticStrength.LIGHT)
            scope.launch { runCatching { viewModel.client.toggleMute(AudioDevice.OUTPUT) } }
        },
        ActionEntry(if (isCaptureMuted) "Unmute Mic" else "Mute Mic", Icons.Filled.MicOff) {
            haptic(WatchHapticStrength.LIGHT)
            scope.launch { runCatching { viewModel.client.toggleMute(AudioDevice.INPUT) } }
        },
        // LEDs
        ActionEntry("Rainbow", Icons.Filled.Palette) {
            haptic(WatchHapticStrength.LIGHT)
            scope.launch { runCatching { viewModel.client.rainbowLEDs() } }
        },
        ActionEntry("Pulse", Icons.Filled.Waves) {
            haptic(WatchHapticStrength.LIGHT)
            scope.launch { runCatching { viewModel.client.pulseLEDs(color = com.ubopod.ubokotlin.models.UboColor.Blue) } }
        },
        ActionEntry("LEDs off", Icons.Filled.Palette) {
            haptic(WatchHapticStrength.LIGHT)
            scope.launch { runCatching { viewModel.client.clearLEDs() } }
        },
        // Display
        ActionEntry("Sleep", Icons.Filled.Bedtime) {
            haptic(WatchHapticStrength.LIGHT)
            scope.launch { runCatching { viewModel.client.blankDisplay() } }
        },
        ActionEntry("Wake", Icons.Filled.LightMode) {
            haptic(WatchHapticStrength.LIGHT)
            scope.launch { runCatching { viewModel.client.unblankDisplay() } }
        },
        // Assistant
        ActionEntry(
            label = if (isMicCapturing) "Stop listening" else "Push to talk",
            icon = if (isMicCapturing) Icons.Filled.MicOff else Icons.Filled.Mic,
        ) {
            haptic(WatchHapticStrength.MEDIUM)
            toggleMic()
        },
        // Device-routed session (the pod listens with its own mics).
        // Disabled while the watch mic is streaming so the two entry
        // points can't interleave and desync — mirrors WatchActionsView.
        ActionEntry(
            label = "Assistant on Pod",
            icon = Icons.Filled.Mic,
            enabled = !isMicCapturing,
        ) {
            haptic(WatchHapticStrength.MEDIUM)
            scope.launch { runCatching { viewModel.client.toggleAssistantListening() } }
        },
        // Power
        ActionEntry("Reboot", Icons.Filled.RestartAlt) {
            haptic(WatchHapticStrength.MEDIUM)
            pendingPower = PowerAction.REBOOT
        },
        ActionEntry("Power off", Icons.Filled.Power) {
            haptic(WatchHapticStrength.MEDIUM)
            pendingPower = PowerAction.POWER_OFF
        },
        // Connection — mirrors the phone app's Settings-tab Disconnect
        // button / Device-tab toolbar icon (same glyph, no confirm) and
        // WatchActionsView.swift's equivalent row.
        ActionEntry("Disconnect", Icons.AutoMirrored.Filled.Logout) {
            haptic(WatchHapticStrength.MEDIUM)
            scope.launch { viewModel.disconnect() }
        },
    )

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp).rotaryScroll(listState),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(actions) { entry ->
            Chip(
                onClick = entry.onTap,
                label = { Text(entry.label, maxLines = 1) },
                icon = { Icon(entry.icon, contentDescription = null) },
                secondaryLabel = entry.trailingText?.let { { Text(it) } },
                colors = ChipDefaults.primaryChipColors(),
                enabled = entry.enabled,
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
    val trailingText: String? = null,
    val enabled: Boolean = true,
    val onTap: () -> Unit,
)
