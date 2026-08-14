package com.ubopod.uboapp.wear.ui.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ubopod.uboapp.wear.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.connection.ConnectionState
import com.ubopod.ubokotlin.models.ViewData

/**
 * Top-level connected screen for the wear app. Renders the current
 * [ViewData] with the matching compact renderer; falls back to a
 * spinner before the first frame arrives.
 *
 * Mirrors `ubo Watch App/Views/WatchDeviceView.swift`.
 */
@Composable
public fun WatchDeviceScreen(viewModel: DeviceViewModel) {
    val view by viewModel.currentView.collectAsStateWithLifecycle()
    val state by viewModel.connectionState.collectAsStateWithLifecycle()
    val statusBar by viewModel.statusBar.collectAsStateWithLifecycle()
    val stats by viewModel.systemStats.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        WatchStatusBarOverlay(
            bar = statusBar,
            cpuPercent = stats?.cpuPercent ?: 0f,
            ramPercent = stats?.ramPercent ?: 0f,
            temperature = stats?.temperatureDisplayValue ?: stats?.temperature,
            temperatureUnit = stats?.temperatureDisplayUnit,
        )
        Box(modifier = Modifier.fillMaxSize()) {
            when (val v = view) {
                is ViewData.Home -> WatchHomeRenderer(v.data, viewModel)
                is ViewData.Menu -> WatchMenuRenderer(v.data, viewModel)
                is ViewData.Notification -> WatchNotificationRenderer(v.data, viewModel)
                is ViewData.Application -> WatchApplicationRenderer(v.data, viewModel)
                is ViewData.Instruction -> WatchInstructionRenderer(v.data, viewModel)
                is ViewData.Prompt -> WatchPromptRenderer(v.data, viewModel)
                is ViewData.Render -> WatchRenderRenderer(v.data, viewModel)
                is ViewData.Chat -> WatchChatRenderer(v.data, viewModel)
                null -> WaitingForView(state)
            }
        }
    }
}

@Composable
private fun WaitingForView(state: ConnectionState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(8.dp))
        Text(
            text = when (state) {
                ConnectionState.CONNECTING -> "Connecting…"
                ConnectionState.RECONNECTING -> "Reconnecting…"
                else -> "Waiting for view"
            },
            style = MaterialTheme.typography.caption1,
            textAlign = TextAlign.Center,
        )
    }
}
