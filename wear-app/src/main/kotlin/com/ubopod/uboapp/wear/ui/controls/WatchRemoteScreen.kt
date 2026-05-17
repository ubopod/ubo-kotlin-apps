package com.ubopod.uboapp.wear.ui.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ubopod.uboapp.wear.ui.common.WatchHapticStrength
import com.ubopod.uboapp.wear.ui.common.rememberWatchHaptic
import com.ubopod.uboapp.wear.viewmodel.DeviceViewModel
import kotlinx.coroutines.launch

/**
 * Compact watch keypad — D-pad (Up/Back+Home/Down) plus L1/L2/L3 row.
 * Mirrors `ubo Watch App/Views/WatchRemoteView.swift`.
 */
@Composable
public fun WatchRemoteScreen(viewModel: DeviceViewModel) {
    val scope = rememberCoroutineScope()
    val haptic = rememberWatchHaptic()

    val pressKey: (suspend () -> Unit) -> Unit = { call ->
        haptic(WatchHapticStrength.LIGHT)
        scope.launch { runCatching { call() } }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        WatchKeyButton(Icons.Filled.KeyboardArrowUp) {
            pressKey { viewModel.client.scrollUp() }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WatchKeyButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft) {
                pressKey { viewModel.client.goBack() }
            }
            HomeCircleButton {
                haptic(WatchHapticStrength.MEDIUM)
                scope.launch { runCatching { viewModel.client.goHome() } }
            }
            Box(modifier = Modifier.size(36.dp))
        }
        WatchKeyButton(Icons.Filled.KeyboardArrowDown) {
            pressKey { viewModel.client.scrollDown() }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SideKey("L1") { pressKey { viewModel.client.pressL1() } }
            SideKey("L2") { pressKey { viewModel.client.pressL2() } }
            SideKey("L3") { pressKey { viewModel.client.pressL3() } }
        }
    }
}

@Composable
private fun WatchKeyButton(icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        colors = ButtonDefaults.secondaryButtonColors(),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun HomeCircleButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        colors = ButtonDefaults.primaryButtonColors(),
    ) {
        Icon(Icons.Filled.Home, contentDescription = "home", modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun SideKey(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(0.28f),
        colors = ButtonDefaults.secondaryButtonColors(),
    ) {
        Text(label, style = MaterialTheme.typography.caption1)
    }
}
