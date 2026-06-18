package com.ubopod.uboapp.phone.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ubopod.uboapp.phone.ui.connection.ConnectionScreen
import com.ubopod.uboapp.phone.ui.dashboard.DashboardScreen
import com.ubopod.uboapp.phone.ui.device.DeviceScreen
import com.ubopod.uboapp.phone.ui.inputs.InputFormSheet
import com.ubopod.uboapp.phone.ui.onboarding.OnboardingScreen
import com.ubopod.uboapp.phone.ui.settings.DeviceSettingsScreen
import com.ubopod.uboapp.phone.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.connection.ConnectionState

/**
 * Root composable. Routes between onboarding / connection /
 * connecting-spinner / connected-shell based on [DeviceViewModel] state.
 *
 * When connected, presents a [Scaffold] with a [NavigationBar] swapping
 * between [DashboardScreen] and [DeviceScreen]. An [InputFormSheet] is
 * overlaid whenever `activeInputs` is non-empty.
 *
 * Mirrors `ubo-swift-app/ubo-swift-app/ContentView.swift`.
 */
@Composable
public fun ContentScreen(viewModel: DeviceViewModel) {
    val onboarded by viewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()
    val state by viewModel.connectionState.collectAsStateWithLifecycle()

    when {
        !onboarded -> OnboardingScreen(viewModel)
        state == ConnectionState.CONNECTING -> ConnectingScreen()
        state.isConnected || state == ConnectionState.RECONNECTING -> ConnectedShell(viewModel)
        else -> ConnectionScreen(viewModel)
    }
}

@Composable
private fun ConnectingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(
                "Connecting to your Ubo…",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private enum class ConnectedTab(val label: String, val icon: ImageVector) {
    DEVICE("Device", Icons.Filled.Smartphone),
    DASHBOARD("Dashboard", Icons.Filled.Dashboard),
    SETTINGS("Settings", Icons.Filled.Settings),
}

@Composable
private fun ConnectedShell(viewModel: DeviceViewModel) {
    var tab by remember { mutableStateOf(ConnectedTab.DEVICE) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                ConnectedTab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        icon = { Icon(entry.icon, contentDescription = null) },
                        label = { Text(entry.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (tab) {
                ConnectedTab.DEVICE -> DeviceScreen(viewModel)
                ConnectedTab.DASHBOARD -> DashboardScreen(viewModel)
                ConnectedTab.SETTINGS -> DeviceSettingsScreen(viewModel)
            }
        }
    }

    // Active input demands surface as a modal bottom sheet that auto-shows
    // when the device pushes a new InputDescription. Until
    // subscribeToActiveInputs lands in :lib, activeInputs stays empty and
    // this never triggers — but the wiring is in place.
    val activeInputs by viewModel.activeInputs.collectAsStateWithLifecycle()
    val first = activeInputs.firstOrNull()
    if (first != null) {
        InputFormSheet(
            description = first,
            viewModel = viewModel,
            onDismiss = { /* StateFlow update from server will clear it. */ },
        )
    }
}
