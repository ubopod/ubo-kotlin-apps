package com.ubopod.uboapp.wear.ui.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Switch
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import com.ubopod.uboapp.wear.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.connection.ConnectionState
import kotlinx.coroutines.launch

/**
 * Manual host/port entry for the wear app. Keyboard-driven input on a
 * watch is awkward at best; in practice users will set the host on the
 * paired phone first, then the watch picks up the saved value via its
 * own DataStore. This screen exists for the rare manual-edit case.
 *
 * Mirrors a slimmed-down `ConnectionView` from the watchOS port.
 */
@Composable
public fun WatchConnectionScreen(viewModel: DeviceViewModel) {
    val savedHost by viewModel.savedHost.collectAsStateWithLifecycle()
    val savedPort by viewModel.savedPort.collectAsStateWithLifecycle()
    val savedUseTls by viewModel.savedUseTls.collectAsStateWithLifecycle()
    val state by viewModel.connectionState.collectAsStateWithLifecycle()

    var host by remember(savedHost) { mutableStateOf(savedHost) }
    var portText by remember(savedPort) { mutableStateOf(savedPort.toString()) }
    var useTls by remember(savedUseTls) { mutableStateOf(savedUseTls) }
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()

    LaunchedEffect(Unit) {
        if (host.isNotEmpty() && state == ConnectionState.DISCONNECTED) {
            viewModel.connectWithSavedSettings()
        }
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
                Button(
                    onClick = {
                        val port = portText.toIntOrNull() ?: 50051
                        scope.launch {
                            runCatching { viewModel.connect(host.trim(), port, useTls) }
                        }
                    },
                    enabled = host.isNotBlank() && state != ConnectionState.CONNECTING,
                ) {
                    Text("Connect")
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun LabeledTextField(label: String, value: String, onChange: (String) -> Unit, isPort: Boolean) {
    androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.caption2)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = MaterialTheme.colors.onSurface, fontSize = MaterialTheme.typography.body2.fontSize),
            cursorBrush = SolidColor(MaterialTheme.colors.primary),
            keyboardOptions = if (isPort) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        )
    }
}
