package com.ubopod.uboapp.phone.ui.device.render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ubopod.uboapp.phone.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.models.RenderViewData

/**
 * Live RGB-frame stream from the device. Mirrors the Swift
 * `FrameStreamRenderView`, which subscribes to
 * `connection.frameStream(streamId:)` and decodes each 3-byte-per-pixel
 * RGB frame to a `UIImage` for display.
 *
 * The Kotlin `UboConnection` doesn't yet expose `subscribeToFrameStream`
 * (see the [README roadmap](../../../../../../../../README.md)), so this
 * placeholder renders a black panel with the stream id while we wait for
 * the underlying decoder to land. Tap a stream to make sure the
 * `streamId` survives the round trip.
 */
@Composable
public fun FrameStreamRender(data: RenderViewData, @Suppress("unused") viewModel: DeviceViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
                .padding(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Frame stream pending",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
            )
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
