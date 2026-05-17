package com.ubopod.uboapp.phone.ui.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.WbIridescent
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ubopod.uboapp.phone.ui.common.HapticStrength
import com.ubopod.uboapp.phone.ui.common.rememberHaptic
import com.ubopod.uboapp.phone.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.models.Chime
import com.ubopod.ubokotlin.models.UboColor
import kotlinx.coroutines.launch

/**
 * One-tap device actions (chime, mute, LED presets, sleep / wake,
 * assistant). Volume + mute are intentionally not here — they live in
 * the Settings screen so the slider can bind live to
 * `state.audio.playback_volume`.
 *
 * Mirrors `ubo-swift-app/.../Views/Controls/QuickActionsView.swift`.
 */
@Composable
public fun QuickActionsView(viewModel: DeviceViewModel, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptic()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Quick actions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(QuickActions) { action ->
                ActionTile(action) {
                    haptic(HapticStrength.LIGHT)
                    scope.launch { runCatching { action.invoke(viewModel) } }
                }
            }
        }
    }
}

@Composable
private fun ActionTile(action: QuickAction, onTap: () -> Unit) {
    Card(
        onClick = onTap,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(action.icon, contentDescription = null, tint = action.tint, modifier = Modifier.size(28.dp))
            Text(
                action.label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

private data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val invoke: suspend (DeviceViewModel) -> Unit,
)

private val QuickActions: List<QuickAction> = listOf(
    QuickAction("Chime", Icons.Filled.NotificationsActive, Color(0xFF1F8FFF)) {
        it.client.playChime(Chime.DONE)
    },
    QuickAction("Rainbow", Icons.Filled.Palette, Color(0xFF9B5DE5)) {
        it.client.rainbowLEDs()
    },
    QuickAction("Pulse", Icons.Filled.WbIridescent, Color(0xFFEC4899)) {
        it.client.pulseLEDs(UboColor.Blue)
    },
    QuickAction("LEDs off", Icons.Filled.Lightbulb, Color(0xFF9CA3AF)) {
        it.client.clearLEDs()
    },
    QuickAction("Sleep", Icons.Filled.Bedtime, Color(0xFF06B6D4)) {
        it.client.blankDisplay()
    },
    QuickAction("Wake", Icons.Filled.WbSunny, Color(0xFFFACC15)) {
        it.client.unblankDisplay()
    },
    QuickAction("Assistant", Icons.Filled.RecordVoiceOver, Color(0xFF10B981)) {
        it.client.toggleAssistantListening()
    },
    QuickAction("Redraw", Icons.Filled.Bolt, Color(0xFFF59E0B)) {
        it.client.requestDisplayRedraw()
    },
)
