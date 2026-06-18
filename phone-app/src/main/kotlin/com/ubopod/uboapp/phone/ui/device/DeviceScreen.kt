package com.ubopod.uboapp.phone.ui.device

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ubopod.uboapp.phone.ui.common.HapticStrength
import com.ubopod.uboapp.phone.ui.common.IconView
import com.ubopod.uboapp.phone.ui.common.rememberHaptic
import com.ubopod.uboapp.phone.ui.common.splitLeadingGlyph
import com.ubopod.uboapp.phone.ui.controls.StatusBarOverlay
import com.ubopod.uboapp.phone.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.connection.ConnectionState
import com.ubopod.ubokotlin.models.ViewData
import kotlinx.coroutines.launch

/**
 * Top-level connected screen. Renders the current [ViewData] with the
 * matching renderer; falls back to a "Waiting for view…" placeholder
 * before the first frame arrives.
 *
 * Mirrors `ubo-swift-app/ubo-swift-app/Views/Device/DeviceView.swift`.
 */
@Composable
public fun DeviceScreen(viewModel: DeviceViewModel) {
    val view by viewModel.currentView.collectAsStateWithLifecycle()
    val state by viewModel.connectionState.collectAsStateWithLifecycle()
    val statusBar by viewModel.statusBar.collectAsStateWithLifecycle()
    val systemStats by viewModel.systemStats.collectAsStateWithLifecycle()
    val isMicCapturing by viewModel.isMicCapturing.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Push-to-talk lives here (not the Dashboard) so the chat overlay the
    // core opens on listening renders in the content Box on this same screen.
    // Mic permission flow: ask on first toggle; later toggles just start/stop.
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
        if (granted) {
            scope.launch { viewModel.toggleMicCapture() }
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // No safeDrawingPadding here — `ConnectedShell`'s Scaffold already
    // hands us its `innerPadding`, which accounts for the system bars
    // and the bottom NavigationBar. Stacking `safeDrawingPadding`
    // on top would double-inset the layout (the toolbar would push
    // away from the status bar by 2× its height).
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        DeviceTopBar(
            title = statusBar?.title.orEmpty().ifEmpty { view?.type ?: "Ubo" },
            connectionState = state,
            onBack = { scope.launch { runCatching { viewModel.client.goBack() } } },
            // Disconnect routes the screen away → use the ViewModel-scoped
            // helper so the suspending shutdown survives composition swap.
            onDisconnect = { viewModel.triggerDisconnect() },
        )
        Spacer(Modifier.size(4.dp))
        // Mirror the Pi-side status bar so users have parity context.
        // Pulls CPU/RAM/temperature out of `systemStats`; everything
        // else (recording indicators, progress notifications, icons,
        // clock) comes straight from the Pi's `StatusBarData`.
        StatusBarOverlay(
            bar = statusBar,
            cpuPercent = systemStats?.cpuPercent ?: 0f,
            ramPercent = systemStats?.ramPercent ?: 0f,
            temperature = systemStats?.temperature,
        )
        Spacer(Modifier.size(8.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when (val v = view) {
                is ViewData.Home -> HomeViewRenderer(v.data, viewModel)
                is ViewData.Menu -> MenuViewRenderer(v.data, viewModel)
                is ViewData.Notification -> NotificationViewRenderer(v.data, viewModel)
                is ViewData.Application -> ApplicationViewRenderer(v.data, viewModel)
                is ViewData.Instruction -> InstructionViewRenderer(v.data, viewModel)
                is ViewData.Prompt -> PromptViewRenderer(v.data, viewModel)
                is ViewData.Render -> RenderViewRenderer(v.data, viewModel)
                is ViewData.Chat -> ChatViewRenderer(v.data, viewModel)
                null -> WaitingForView(state)
            }
        }

        Spacer(Modifier.size(8.dp))

        // Push-to-talk: streams this phone's mic to the assistant. The core
        // opens the chat view on listening, which renders in the Box above —
        // so the conversation is visible on this same screen.
        Button(
            onClick = toggleMic,
            colors = if (isMicCapturing) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            } else {
                ButtonDefaults.buttonColors()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                if (isMicCapturing) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = null,
            )
            Spacer(Modifier.size(8.dp))
            Text(if (isMicCapturing) "Stop listening" else "Push to talk")
        }
    }
}

@Composable
private fun DeviceTopBar(
    title: String,
    connectionState: ConnectionState,
    onBack: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val haptic = rememberHaptic()
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                haptic(HapticStrength.LIGHT)
                onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            // Status-bar titles often start with a Nerd-Font PUA glyph
            // (e.g. the device-host title `\u{F005C}ubo-6j.local`). The
            // glyph needs the bundled arimo_nerd.ttf to render — pulling
            // it through IconView keeps the rest of the title in the
            // Material 3 theme font.
            val (leadingGlyph, rest) = splitLeadingGlyph(title)
            Row(
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingGlyph != null) {
                    IconView(
                        icon = leadingGlyph,
                        size = 18.dp,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    rest,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
            if (connectionState == ConnectionState.RECONNECTING) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            IconButton(onClick = {
                haptic(HapticStrength.MEDIUM)
                onDisconnect()
            }) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Disconnect")
            }
        }
    }
}

@Composable
private fun WaitingForView(state: ConnectionState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
        Spacer(Modifier.size(12.dp))
        Text(
            when (state) {
                ConnectionState.CONNECTING -> "Connecting…"
                ConnectionState.RECONNECTING -> "Reconnecting…"
                else -> "Waiting for the device's first view frame."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

