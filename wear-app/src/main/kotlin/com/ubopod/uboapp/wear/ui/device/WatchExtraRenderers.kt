package com.ubopod.uboapp.wear.ui.device

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.ubopod.uboapp.wear.ui.common.decodeRgb888Frame
import com.ubopod.uboapp.wear.ui.common.generateQrCodeBitmap
import com.ubopod.uboapp.wear.ui.common.rotaryScroll
import com.ubopod.uboapp.wear.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.models.ApplicationViewData
import com.ubopod.ubokotlin.models.InstructionViewData
import com.ubopod.ubokotlin.models.NotificationViewData
import com.ubopod.ubokotlin.models.PromptViewData
import com.ubopod.ubokotlin.models.RenderKind
import com.ubopod.ubokotlin.models.RenderPropValue
import com.ubopod.ubokotlin.models.RenderViewData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
public fun WatchRenderRenderer(data: RenderViewData, viewModel: DeviceViewModel) {
    when (data.kind) {
        RenderKind.QrCode -> WatchQrCodeRender(data)
        RenderKind.QrCodeCarousel -> WatchQrCodeCarouselRender(data)
        RenderKind.FrameStream -> WatchFrameStreamRender(data, viewModel)
        else -> {
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

/**
 * Every producer (tailscale/rpi-connect/vscode/hermes setup services) sends
 * the QR-encoded string under `value` — never `data`, `url`, or `payload`.
 * Mirrors the phone app's `RenderProps.kt` / the Swift port's
 * `QRCodeRenderView`.
 */
private fun RenderViewData.stringProp(key: String): String = when (val v = props[key]) {
    is RenderPropValue.StringValue -> v.value
    else -> ""
}

private fun RenderViewData.stringListProp(key: String): List<String> =
    (props[key] as? RenderPropValue.ListValue)?.value.orEmpty()
        .mapNotNull { (it as? RenderPropValue.StringValue)?.value }

/**
 * A real scannable QR bitmap, not a placeholder — the watch screen is close
 * enough in size to the pod's own 1.56" display and the ESP32 display, both
 * of which render actual QR codes at this scale. No hyperlink/value text
 * underneath: there's no browser here to act on it, so it would just cost
 * the QR the room it needs (same reasoning as the pod GUI's
 * QRCodeRenderPage, which drops URL-shaped labels for the same reason).
 * `caption` is kept since it's not a link — it's a code the user types
 * after scanning (e.g. an OAuth device code).
 */
@Composable
private fun WatchQrCodeRender(data: RenderViewData) {
    val value = data.stringProp("value")
    val caption = data.stringProp("caption")
    val image = remember(value) { generateQrCodeBitmap(value) }
    val scrollState = rememberScrollState()

    // Scrollable rather than a fixed vertically-centered box: title/caption
    // length is server-driven (varies per producer — a device code here, a
    // longer link elsewhere), and a QR big enough to scan plus two lines of
    // title and caption doesn't reliably fit in the ~140dp left after
    // reserving room for the curved status bar overlay above. Scrolling
    // means a longer instance never silently clips instead of needing a
    // fresh size guess per case.
    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(scrollState)
            .rotaryScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(top = 36.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (data.title.isNotEmpty()) {
            QrPageLabel(data.title, MaterialTheme.typography.caption2)
        }
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = "QR code",
                filterQuality = FilterQuality.None,
                modifier = Modifier.fillMaxWidth(0.52f).aspectRatio(1f).background(Color.White).padding(2.dp),
            )
        } else {
            Text("Empty QR payload", style = MaterialTheme.typography.caption2)
        }
        if (caption.isNotEmpty()) {
            QrPageLabel(caption, MaterialTheme.typography.caption2.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

@Composable
private fun WatchQrCodeCarouselRender(data: RenderViewData) {
    val values = remember(data) { data.stringListProp("values") }

    if (values.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("No QR data", style = MaterialTheme.typography.caption2)
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { values.size })
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(scrollState)
            .rotaryScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(top = 36.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (data.title.isNotEmpty()) {
            QrPageLabel(data.title, MaterialTheme.typography.caption2)
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { index ->
            val image = remember(values[index]) { generateQrCodeBitmap(values[index]) }
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = "QR code ${index + 1} of ${values.size}",
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.fillMaxWidth(0.52f).aspectRatio(1f).background(Color.White).padding(2.dp),
                )
            }
        }
    }
}

/**
 * QR page title/caption text, capped to a fraction of the available width
 * rather than a fixed dp inset. The QR image pushes this close to the
 * round bezel's curve, where the flat side-padding other pages use isn't
 * enough — the true safe width shrinks the closer the row sits to the top
 * or bottom edge, so a width fraction holds regardless of exactly how
 * close that ends up being, instead of relying on one dp guess.
 */
@Composable
private fun QrPageLabel(text: String, style: TextStyle) {
    Text(
        text,
        style = style,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth(0.62f),
    )
}

/**
 * Live RGB-frame stream from the device (video playback, camera
 * viewfinder). Subscribes to `UboClient.frameStream(streamId)` and decodes
 * each 3-byte-per-pixel RGB frame to a [Bitmap] for display — the watch
 * previously fell back to "Live stream (open phone app)" text here.
 * Mirrors the phone app's `FrameStreamRender`.
 */
@Composable
private fun WatchFrameStreamRender(data: RenderViewData, viewModel: DeviceViewModel) {
    var bitmap by remember(data.streamId) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(data.streamId) {
        viewModel.client.frameStream(data.streamId)
            .catch { /* stream ended; UI keeps the last frame */ }
            .collect { frame ->
                // Off the main thread: this collect runs on LaunchedEffect's
                // (main-thread) scope, and decoding is a width*height pixel
                // loop repeated on every incoming frame — at live-stream
                // frame rates that's enough main-thread work, often enough,
                // to make the edge-swipe-to-dismiss gesture unrecognizable
                // (confirmed: swipe-back stopped working specifically on
                // this screen once real decoding replaced the text
                // fallback). Only hopping back to set `bitmap` touches the
                // UI thread.
                val decoded = withContext(Dispatchers.Default) {
                    decodeRgb888Frame(frame.data, frame.width, frame.height)
                }
                decoded?.let { bitmap = it }
            }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        if (data.title.isNotEmpty()) {
            WatchTitleText(title = data.title)
        }
        Box(
            modifier = Modifier.fillMaxWidth(0.8f).aspectRatio(1f).background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            val currentBitmap = bitmap
            if (currentBitmap != null) {
                // fillMaxSize(), not fillMaxWidth(): this Box already has a
                // definite square size (fillMaxWidth + aspectRatio), but
                // Image only reliably scales UP a small bitmap to fill its
                // container when both dimensions are constrained.
                Image(
                    bitmap = currentBitmap.asImageBitmap(),
                    contentDescription = data.title.ifEmpty { "Stream" },
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                CircularProgressIndicator()
            }
        }
    }
}
