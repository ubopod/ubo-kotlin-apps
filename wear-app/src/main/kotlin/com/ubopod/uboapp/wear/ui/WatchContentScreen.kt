package com.ubopod.uboapp.wear.ui

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ubopod.uboapp.wear.ui.connection.WatchConnectionScreen
import com.ubopod.uboapp.wear.ui.device.WatchDeviceScreen
import com.ubopod.uboapp.wear.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.connection.ConnectionState

/**
 * Root composable for the wear app. Routes between connection /
 * connecting-spinner / connected-device based on [DeviceViewModel] state.
 *
 * Mirrors `ubo Watch App/Views/WatchContentView.swift`. Onboarding is
 * intentionally elided here — the watch goes directly to the connection
 * screen on first launch.
 */
@Composable
public fun WatchContentScreen(viewModel: DeviceViewModel) {
    val state by viewModel.connectionState.collectAsStateWithLifecycle()

    when {
        state == ConnectionState.CONNECTING -> ConnectingScreen()
        state.isConnected || state == ConnectionState.RECONNECTING -> WatchDeviceScreen(viewModel)
        else -> WatchConnectionScreen(viewModel)
    }
}

@Composable
private fun ConnectingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))
            Text(
                "Connecting…",
                style = MaterialTheme.typography.body2,
            )
        }
    }
}
