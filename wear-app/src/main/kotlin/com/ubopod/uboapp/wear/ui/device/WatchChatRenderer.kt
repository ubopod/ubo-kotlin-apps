package com.ubopod.uboapp.wear.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ubopod.uboapp.wear.ui.common.watchUboIconColor
import com.ubopod.uboapp.wear.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.models.ChatBubbleData
import com.ubopod.ubokotlin.models.ChatViewData
import kotlinx.coroutines.launch

/**
 * Slim watch renderer for [ChatViewData] — the assistant conversation. The
 * core precomputes every bubble; this only draws what it's told. Voice-only:
 * audio bubbles are tapped to toggle playback.
 *
 * Mirrors `ubo-swift-app/ubo Watch App/Views/WatchChatView.swift`.
 */
@Composable
public fun WatchChatRenderer(data: ChatViewData, viewModel: DeviceViewModel) {
    if (data.bubbles.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            Text(
                "No messages yet",
                style = MaterialTheme.typography.caption1,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()

    LaunchedEffect(data.bubbles.lastOrNull()?.messageId, data.scrollOffset) {
        if (data.scrollOffset == 0 && data.bubbles.isNotEmpty()) {
            listState.animateScrollToItem(data.bubbles.lastIndex)
        }
    }

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
    ) {
        items(data.bubbles, key = { it.messageId }) { bubble ->
            WatchChatBubbleRow(bubble) {
                scope.launch { runCatching { viewModel.client.toggleChatAudio(bubble.messageId) } }
            }
        }
    }
}

@Composable
private fun WatchChatBubbleRow(bubble: ChatBubbleData, onToggleAudio: () -> Unit) {
    val isUser = bubble.alignment == "right"
    val isAudio = bubble.kind == "audio"
    val foreground = watchUboIconColor(bubble.color, Color.White)
    val background = watchUboIconColor(bubble.backgroundColor, Color(0xFF2B2F38))

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = if (isUser) androidx.compose.foundation.layout.Arrangement.End
        else androidx.compose.foundation.layout.Arrangement.Start,
    ) {
        var bubbleModifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
        if (isAudio) {
            bubbleModifier = bubbleModifier.clickable(onClick = onToggleAudio)
        }
        Box(modifier = bubbleModifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(
                text = if (isAudio) (if (bubble.isPlaying) "⏸ Audio" else "▶ Audio") else bubble.text,
                color = foreground,
                style = MaterialTheme.typography.caption2,
            )
        }
    }
}
