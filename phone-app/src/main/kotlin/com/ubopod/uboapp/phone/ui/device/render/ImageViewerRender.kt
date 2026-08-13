package com.ubopod.uboapp.phone.ui.device.render

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ubopod.uboapp.phone.ui.common.decodeRgb888Frame
import com.ubopod.uboapp.phone.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.models.RenderViewData
import kotlinx.coroutines.flow.catch

/**
 * Props carry only the image's geometry (width/height) — the pixels arrive
 * as `FrameStreamDataEvent`, exactly like [FrameStreamRender], specifically
 * so a picture never puts a multi-megabyte payload on the shared store
 * stream that every client (including MCU ones) would have to swallow.
 *
 * Mirrors the Swift `ImageViewerRenderView` / the Web UI's `ImageViewer`.
 */
@Composable
public fun ImageViewerRender(data: RenderViewData, viewModel: DeviceViewModel) {
    var bitmap by remember(data.streamId) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(data.streamId) {
        viewModel.client.frameStream(data.streamId)
            .catch { /* stream ended; UI keeps the last frame */ }
            .collect { frame ->
                decodeRgb888Frame(frame.data, frame.width, frame.height)?.let { bitmap = it }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = data.title.ifEmpty { "Image" },
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            CircularProgressIndicator()
        }
    }
}
