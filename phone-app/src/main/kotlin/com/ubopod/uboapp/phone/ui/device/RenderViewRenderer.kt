package com.ubopod.uboapp.phone.ui.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ubopod.uboapp.phone.ui.common.markupAnnotated
import com.ubopod.uboapp.phone.ui.device.render.FrameStreamRender
import com.ubopod.uboapp.phone.ui.device.render.ImageViewerRender
import com.ubopod.uboapp.phone.ui.device.render.QrCodeCarouselRender
import com.ubopod.uboapp.phone.ui.device.render.QrCodeRender
import com.ubopod.uboapp.phone.ui.device.render.StatusRender
import com.ubopod.uboapp.phone.ui.device.render.TextViewerRender
import com.ubopod.uboapp.phone.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.models.RenderKind
import com.ubopod.ubokotlin.models.RenderViewData

/**
 * Dispatches a [RenderViewData] payload to one of six sub-renderers.
 *
 * Mirrors the `case .render(let data)` branch of the Swift
 * `RenderDeviceView` sub-kind switch.
 */
@Composable
public fun RenderViewRenderer(data: RenderViewData, viewModel: DeviceViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (data.title.isNotEmpty()) {
            Text(
                markupAnnotated(data.title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        when (val kind = data.kind) {
            RenderKind.QrCode -> QrCodeRender(data)
            RenderKind.QrCodeCarousel -> QrCodeCarouselRender(data)
            RenderKind.TextViewer -> TextViewerRender(data)
            RenderKind.ImageViewer -> ImageViewerRender(data)
            RenderKind.Status -> StatusRender(data)
            RenderKind.FrameStream -> FrameStreamRender(data, viewModel)
            is RenderKind.Unknown -> Text(
                "Unknown render kind: ${kind.raw}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
