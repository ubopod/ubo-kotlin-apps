package com.ubopod.uboapp.phone.ui.device.render

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ubopod.uboapp.phone.ui.common.decodeRgb888Frame
import com.ubopod.uboapp.phone.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.models.RenderViewData
import kotlinx.coroutines.flow.catch

/**
 * Live RGB-frame stream from the device (video playback, camera
 * viewfinder). Subscribes to `UboClient.frameStream(streamId)` and decodes
 * each 3-byte-per-pixel RGB frame to a [Bitmap] for display.
 *
 * Mirrors the Swift `FrameStreamRenderView`.
 */
@Composable
public fun FrameStreamRender(data: RenderViewData, viewModel: DeviceViewModel) {
    var bitmap by remember(data.streamId) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(data.streamId) {
        viewModel.client.frameStream(data.streamId)
            .catch { /* stream ended; UI keeps the last frame */ }
            .collect { frame ->
                decodeRgb888Frame(frame.data, frame.width, frame.height)?.let { bitmap = it }
            }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            val currentBitmap = bitmap
            if (currentBitmap != null) {
                // fillMaxSize(), not fillMaxWidth(): the box above already
                // has a definite square size (fillMaxWidth + aspectRatio),
                // but Image only reliably scales UP a small bitmap to fill
                // its container when both dimensions are constrained —
                // fillMaxWidth() alone rendered every frame at close to its
                // native pixel size (a 240px preview frame as a fingertip-
                // sized image on a 1000px-wide box) despite ContentScale.Fit.
                Image(
                    bitmap = currentBitmap.asImageBitmap(),
                    contentDescription = data.title.ifEmpty { "Stream" },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                CircularProgressIndicator(color = Color.White)
            }
        }
        if (data.streamId.isNotEmpty()) {
            Text(
                "stream_id = ${data.streamId}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
