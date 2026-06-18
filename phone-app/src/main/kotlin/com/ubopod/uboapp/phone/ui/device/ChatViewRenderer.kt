package com.ubopod.uboapp.phone.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ubopod.uboapp.phone.ui.common.uboIconColor
import com.ubopod.uboapp.phone.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.models.ChatBubbleData
import com.ubopod.ubokotlin.models.ChatViewData
import kotlinx.coroutines.launch

/**
 * Renders [ChatViewData] — the assistant conversation overlay. The core
 * precomputes every bubble (alignment, colors, waveform, playing state), so
 * this renderer only draws what it's told. Chat is voice-only for now (no text
 * composer); audio bubbles are tapped to toggle playback, mirroring the
 * device's L1/L2/L3 button binding.
 *
 * Mirrors `ubo-swift-app/ubo-swift-app/Views/Device/ChatDeviceView.swift`.
 */
@Composable
public fun ChatViewRenderer(data: ChatViewData, viewModel: DeviceViewModel) {
    if (data.bubbles.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "No messages yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Start talking to the assistant.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Auto-scroll to the newest bubble unless the user has scrolled back.
    LaunchedEffect(data.bubbles.lastOrNull()?.messageId, data.scrollOffset) {
        if (data.scrollOffset == 0 && data.bubbles.isNotEmpty()) {
            listState.animateScrollToItem(data.bubbles.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(data.bubbles, key = { it.messageId }) { bubble ->
            ChatBubbleRow(bubble) {
                scope.launch { runCatching { viewModel.client.toggleChatAudio(bubble.messageId) } }
            }
        }
    }
}

@Composable
private fun ChatBubbleRow(bubble: ChatBubbleData, onToggleAudio: () -> Unit) {
    val isUser = bubble.alignment == "right"
    val isAudio = bubble.kind == "audio"
    val foreground = uboIconColor(bubble.color, Color.White)
    val background = uboIconColor(bubble.backgroundColor, Color(0xFF2B2F38))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        var bubbleModifier = Modifier
            .widthIn(max = 320.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
        if (isAudio) {
            bubbleModifier = bubbleModifier.clickable(onClick = onToggleAudio)
        }

        Box(modifier = bubbleModifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            if (isAudio) {
                Waveform(bars = bubble.waveform, color = foreground, isPlaying = bubble.isPlaying)
            } else {
                Text(bubble.text, color = foreground, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * Static bar visualization for an audio bubble. `isPlaying` only changes
 * opacity (no animation) so the rendered frame stays deterministic, matching
 * the other clients.
 */
@Composable
private fun Waveform(bars: List<Float>, color: Color, isPlaying: Boolean) {
    Row(
        modifier = Modifier.height(28.dp).widthIn(min = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val barColor = color.copy(alpha = if (isPlaying) 1f else 0.45f)
        bars.forEach { value ->
            val h = (value.coerceIn(0f, 1f) * 28f).coerceAtLeast(4f)
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(h.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor),
            )
        }
    }
}
