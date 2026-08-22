package com.ubopod.uboapp.wear.ui.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import com.ubopod.uboapp.wear.storage.UboWearSettings
import com.ubopod.uboapp.wear.ui.common.WatchTextField
import com.ubopod.uboapp.wear.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.connection.ConnectionState
import com.ubopod.ubokotlin.connection.DiscoveredDevice
import kotlinx.coroutines.launch

/**
 * Manual host/port entry for the wear app, plus network discovery, recent
 * connections, and new-device Wi-Fi setup — mirrors the phone app's
 * `ConnectionScreen.kt` section order (form → Connect → Found on network →
 * Recent Connections → New Device Setup), scrolled through in one
 * `ScalingLazyColumn` instead of separate screens.
 */
@Composable
public fun WatchConnectionScreen(viewModel: DeviceViewModel) {
    var showWifiSetup by remember { mutableStateOf(false) }

    if (showWifiSetup) {
        SwipeToDismissBox(onDismissed = { showWifiSetup = false }) {
            WatchWifiQrCodeScreen()
        }
    } else {
        ConnectionFormScreen(viewModel, onWifiSetupClick = { showWifiSetup = true })
    }
}

@Composable
private fun ConnectionFormScreen(viewModel: DeviceViewModel, onWifiSetupClick: () -> Unit) {
    val savedHost by viewModel.savedHost.collectAsStateWithLifecycle()
    val savedPort by viewModel.savedPort.collectAsStateWithLifecycle()
    val savedUseTls by viewModel.savedUseTls.collectAsStateWithLifecycle()
    val state by viewModel.connectionState.collectAsStateWithLifecycle()
    val discovered by viewModel.discovered.collectAsStateWithLifecycle()
    val recentConnections by viewModel.recentConnections.collectAsStateWithLifecycle()

    var host by remember(savedHost) { mutableStateOf(savedHost) }
    var portText by remember(savedPort) { mutableStateOf(savedPort.toString()) }
    var useTls by remember(savedUseTls) { mutableStateOf(savedUseTls) }
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()

    // Auto-connecting on cold-start is handled once by DeviceViewModel's
    // init block, not here — this composable remounts every time the
    // router falls back to it (including right after a failed connect
    // attempt), so an auto-connect tied to its own composition would
    // retry in a tight loop on any fast failure.

    DisposableEffect(Unit) {
        viewModel.startDiscovery()
        onDispose { viewModel.stopDiscovery() }
    }

    fun connect(connectHost: String, connectPort: Int, connectUseTls: Boolean) {
        scope.launch { runCatching { viewModel.connect(connectHost.trim(), connectPort, connectUseTls) } }
    }

    Scaffold(
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "Connect to your Ubo",
                    style = MaterialTheme.typography.title3,
                    textAlign = TextAlign.Center,
                )
            }
            item { LabeledTextField(label = "Host", value = host, onChange = { host = it }, isPort = false) }
            item {
                LabeledTextField(
                    label = "Port",
                    value = portText,
                    onChange = { portText = it.filter { ch -> ch.isDigit() } },
                    isPort = true,
                )
            }
            item {
                ToggleChip(
                    checked = useTls,
                    onCheckedChange = { useTls = it },
                    label = { Text("Use TLS") },
                    toggleControl = { Switch(checked = useTls) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Chip(
                    onClick = { connect(host, portText.toIntOrNull() ?: UboWearSettings.DEFAULT_PORT, useTls) },
                    label = { Text("Connect") },
                    colors = ChipDefaults.primaryChipColors(),
                    enabled = host.isNotBlank() && state != ConnectionState.CONNECTING,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Discovered devices (mDNS/NSD) — always shown, with a
            // "searching" placeholder when empty, mirrors ConnectionScreen.kt.
            item {
                Text(
                    "Found on network",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                )
            }
            if (discovered.isEmpty()) {
                item {
                    Text(
                        "Searching…",
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.onSurfaceVariant,
                    )
                }
            } else {
                for (device: DiscoveredDevice in discovered.sortedBy { it.name }) {
                    item {
                        Chip(
                            onClick = { connect(device.host, device.port, false) },
                            label = { Text(device.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            secondaryLabel = { Text("${device.host}:${device.port}", maxLines = 1) },
                            colors = ChipDefaults.secondaryChipColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // Recent Connections (up to 3).
            if (recentConnections.isNotEmpty()) {
                item {
                    Text(
                        "Recent Connections",
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.onSurfaceVariant,
                    )
                }
                for (recent in recentConnections) {
                    item {
                        Chip(
                            onClick = { connect(recent.host, recent.port, recent.useTls) },
                            label = { Text(recent.host, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            secondaryLabel = {
                                Text("Port ${recent.port}${if (recent.useTls) " · TLS" else ""}", maxLines = 1)
                            },
                            colors = ChipDefaults.secondaryChipColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // Setting up a brand new Ubo: it isn't on any network yet, so
            // this has to work before any connection exists.
            item {
                Text(
                    "New Device Setup",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                )
            }
            item {
                Chip(
                    onClick = onWifiSetupClick,
                    // Short enough to fit the chip at default scale; the
                    // "New Device Setup" header above supplies the context
                    // the longer wording used to carry. Two lines so a large
                    // accessibility font wraps instead of truncating.
                    label = { Text("Set up Wi-Fi", maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun LabeledTextField(label: String, value: String, onChange: (String) -> Unit, isPort: Boolean) {
    WatchTextField(
        label = label,
        value = value,
        onValueChange = onChange,
        placeholder = if (isPort) UboWearSettings.DEFAULT_PORT.toString() else "192.168.1.10",
        keyboardOptions = if (isPort) {
            KeyboardOptions(keyboardType = KeyboardType.Number)
        } else {
            KeyboardOptions.Default
        },
    )
}
