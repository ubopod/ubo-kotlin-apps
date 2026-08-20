package com.ubopod.uboapp.wear.ui.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.ubopod.uboapp.wear.ui.common.WatchTitleText
import com.ubopod.uboapp.wear.ui.common.rotaryScroll
import com.ubopod.uboapp.wear.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.models.ApplicationViewData
import com.ubopod.ubokotlin.models.InstructionViewData
import com.ubopod.ubokotlin.models.NotificationViewData
import com.ubopod.ubokotlin.models.PromptViewData
import com.ubopod.ubokotlin.models.RenderKind
import com.ubopod.ubokotlin.models.RenderPropValue
import com.ubopod.ubokotlin.models.RenderViewData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ----- Notification ----- */

@Composable
public fun WatchNotificationRenderer(data: NotificationViewData, viewModel: DeviceViewModel) {
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()
    Scaffold(positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp).rotaryScroll(listState),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item { WatchTitleText(title = data.title) }
            item {
                Text(data.content, style = MaterialTheme.typography.body2, textAlign = TextAlign.Center)
            }
            items(data.items.filterNotNull()) { item ->
                MenuChip(item) {
                    scope.launch { runCatching { viewModel.client.selectMenuItem(item) } }
                }
            }
            item {
                Chip(
                    onClick = { scope.launch { runCatching { viewModel.client.goBack() } } },
                    label = { Text("Dismiss") },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth(0.95f),
                )
            }
        }
    }
}

/* ----- Application ----- */

@Composable
public fun WatchApplicationRenderer(data: ApplicationViewData, viewModel: DeviceViewModel) {
    val scope = rememberCoroutineScope()
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("App on device", style = MaterialTheme.typography.title3, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(data.applicationId.compactForWatch(20), style = MaterialTheme.typography.caption2, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Button(onClick = { scope.launch { runCatching { viewModel.client.goBack() } } }) {
            Text("Back")
        }
    }
}

/* ----- Instruction ----- */

@Composable
public fun WatchInstructionRenderer(data: InstructionViewData, @Suppress("unused") viewModel: DeviceViewModel) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (data.spinner) {
            CircularProgressIndicator()
            Spacer(Modifier.height(6.dp))
        }
        WatchTitleText(title = data.title)
        Spacer(Modifier.height(4.dp))
        Text(data.instruction, style = MaterialTheme.typography.body2, textAlign = TextAlign.Center)
        if (data.timeoutSeconds > 0) {
            Spacer(Modifier.height(6.dp))
            CountdownText(initialSeconds = data.timeoutSeconds)
        }
    }
}

@Composable
private fun CountdownText(initialSeconds: Int) {
    var remaining by remember(initialSeconds) { mutableIntStateOf(initialSeconds) }
    LaunchedEffect(initialSeconds) {
        while (remaining > 0) {
            delay(1_000)
            remaining -= 1
        }
    }
    Text("${remaining}s", style = MaterialTheme.typography.caption2)
}

/* ----- Prompt ----- */

@Composable
public fun WatchPromptRenderer(data: PromptViewData, viewModel: DeviceViewModel) {
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()
    Scaffold(positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp).rotaryScroll(listState),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item { WatchTitleText(title = data.title) }
            item {
                Text(data.prompt, style = MaterialTheme.typography.body2, textAlign = TextAlign.Center)
            }
            items(data.items) { item ->
                MenuChip(item) {
                    scope.launch { runCatching { viewModel.client.selectMenuItem(item) } }
                }
            }
            if (data.items.isEmpty()) {
                item {
                    Chip(
                        onClick = { scope.launch { runCatching { viewModel.client.goBack() } } },
                        label = { Text("Dismiss") },
                        colors = ChipDefaults.secondaryChipColors(),
                    )
                }
            }
        }
    }
}

/* ----- Render (text-only on the watch) ----- */

@Composable
public fun WatchRenderRenderer(data: RenderViewData, @Suppress("unused") viewModel: DeviceViewModel) {
    val payload: String = when (val v = data.props["data"] ?: data.props["text"] ?: data.props["payload"]) {
        is RenderPropValue.StringValue -> v.value
        is RenderPropValue.IntValue -> v.value.toString()
        else -> ""
    }
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (data.title.isNotEmpty()) {
            WatchTitleText(title = data.title)
            Spacer(Modifier.height(4.dp))
        }
        Text(
            text = describeRenderKind(data.kind, payload),
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Center,
        )
    }
}

private fun describeRenderKind(kind: RenderKind, payload: String): String = when (kind) {
    RenderKind.QrCode, RenderKind.QrCodeCarousel ->
        "Open the phone app to scan the QR code." + if (payload.isNotEmpty()) "\n$payload" else ""
    RenderKind.TextViewer -> payload.ifEmpty { "(no content)" }
    RenderKind.ImageViewer -> "Image (open phone app)"
    RenderKind.Status -> payload.ifEmpty { "Status" }
    RenderKind.FrameStream -> "Live stream (open phone app)"
    RenderKind.Readings -> "Readings (open phone app)"
    is RenderKind.Unknown -> "Unsupported view: ${kind.raw}"
}
