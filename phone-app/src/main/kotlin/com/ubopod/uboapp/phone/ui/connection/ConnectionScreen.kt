package com.ubopod.uboapp.phone.ui.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ubopod.uboapp.phone.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.UboError
import com.ubopod.ubokotlin.connection.DiscoveredDevice
import com.ubopod.ubokotlin.connection.ConnectionState

/**
 * Manual host/port entry plus a Bonjour/mDNS discovery list mirroring
 * `ubo-swift-app/ubo-swift-app/Views/Connection/ConnectionView.swift`. Tap
 * a discovered device to auto-fill the form and connect.
 */
@Composable
public fun ConnectionScreen(viewModel: DeviceViewModel) {
    val savedHost by viewModel.savedHost.collectAsStateWithLifecycle()
    val savedPort by viewModel.savedPort.collectAsStateWithLifecycle()
    val discovered by viewModel.discovered.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val lastError by viewModel.client.lastError.collectAsStateWithLifecycle()

    var host by remember(savedHost) { mutableStateOf(savedHost) }
    var portText by remember(savedPort) { mutableStateOf(savedPort.toString()) }

    // Lifecycle the discovery scan to this screen's composition.
    DisposableEffect(Unit) {
        viewModel.startDiscovery()
        onDispose { viewModel.stopDiscovery() }
    }

    // The auto-reconnect on cold-start is handled by DeviceViewModel.init
    // running in viewModelScope, so a composition swap mid-probe doesn't
    // cancel the in-flight attempt.

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Connect to your Ubo",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text("Host or IP") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = portText,
            onValueChange = { portText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Port") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                val port = portText.toIntOrNull() ?: 50051
                // Use the ViewModel-scoped trigger so the in-flight probe
                // survives ContentScreen's CONNECTING-state route swap.
                viewModel.triggerConnect(host.trim(), port)
            },
            enabled = host.isNotBlank() && connectionState != ConnectionState.CONNECTING,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Cable, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Connect")
        }

        lastError?.let { error ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = error.message ?: error::class.simpleName.orEmpty(),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        HorizontalDivider()

        Text(
            "Found on network",
            style = MaterialTheme.typography.titleMedium,
        )

        if (discovered.isEmpty()) {
            Text(
                "Searching… make sure your phone and Ubo device are on the same Wi-Fi network.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            discovered.sortedBy { it.name }.forEach { device ->
                DiscoveredDeviceRow(device) {
                    host = device.host
                    portText = device.port.toString()
                    viewModel.triggerConnect(device.host, device.port)
                }
            }
        }
    }
}

@Composable
private fun DiscoveredDeviceRow(device: DiscoveredDevice, onTap: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextButton(
            onClick = onTap,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.SignalWifi4Bar, contentDescription = null)
                Column(modifier = Modifier.weight(1f)) {
                    Text(device.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        "${device.host}:${device.port}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
