package com.ubopod.uboapp.phone.ui.device.render

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ubopod.uboapp.phone.ui.common.bytesProp
import com.ubopod.uboapp.phone.ui.common.stringProp
import com.ubopod.ubokotlin.models.RenderViewData

/**
 * Decode a PNG/JPEG payload from `props["data"]`, `"image"`, or
 * (base64-encoded) `"data_base64"` and render it scaled to fit. Mirrors
 * the Swift `ImageViewerRenderView`.
 */
@Composable
public fun ImageViewerRender(data: RenderViewData) {
    val bitmap = remember(data) {
        val bytes = data.bytesProp("data", "image")
            ?: data.stringProp("data_base64").takeIf { it.isNotEmpty() }?.let { runCatching { Base64.decode(it, Base64.DEFAULT) }.getOrNull() }
        bytes?.let {
            runCatching { BitmapFactory.decodeByteArray(it, 0, it.size) }.getOrNull()
        }?.asImageBitmap()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = data.title.ifEmpty { "Image" },
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(
                "No image data.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
