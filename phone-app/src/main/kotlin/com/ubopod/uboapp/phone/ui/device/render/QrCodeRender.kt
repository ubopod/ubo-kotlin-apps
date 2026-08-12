package com.ubopod.uboapp.phone.ui.device.render

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubopod.uboapp.phone.ui.common.LinkifiedText
import com.ubopod.uboapp.phone.ui.common.generateQrCodeBitmap
import com.ubopod.uboapp.phone.ui.common.stringProp
import com.ubopod.ubokotlin.models.RenderViewData

/**
 * Render a single QR code. Mirrors `QRCodeRenderView` in the Swift port.
 *
 * Every producer (tailscale/rpi-connect/vscode/hermes setup services) sends
 * the QR-encoded string under `value` — never `data`, `url`, or `payload`,
 * which is what this used to check for and is why the QR code silently
 * failed to render.
 */
@Composable
public fun QrCodeRender(data: RenderViewData) {
    val value = data.stringProp("value")
    val label = data.stringProp("label").ifEmpty { value }
    val caption = data.stringProp("caption").ifEmpty { null }
    val image = remember(value) { generateQrCodeBitmap(value, sizePx = 512) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (data.title.isNotEmpty()) {
            Text(data.title, style = MaterialTheme.typography.titleMedium)
        }
        Box(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = "QR code containing $value",
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
            } else {
                Text(
                    "Empty QR payload",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray,
                )
            }
        }
        if (label.isNotEmpty()) {
            LinkifiedText(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                textAlign = TextAlign.Center,
            )
        }
        caption?.let {
            Text(
                it,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
