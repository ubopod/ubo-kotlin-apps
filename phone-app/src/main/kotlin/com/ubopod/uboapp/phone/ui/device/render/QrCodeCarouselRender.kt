package com.ubopod.uboapp.phone.ui.device.render

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ubopod.uboapp.phone.ui.common.LinkifiedText
import com.ubopod.uboapp.phone.ui.common.generateQrCodeBitmap
import com.ubopod.ubokotlin.models.RenderPropValue
import com.ubopod.ubokotlin.models.RenderViewData

/**
 * Render an indexed carousel of QR codes. Mirrors the Swift
 * `QRCodeCarouselRenderView`.
 *
 * Parallel arrays: `values` are the QR-encoded strings, `labels` the
 * (optionally shorter) text shown under each one — never `payloads`/
 * `data`/`urls`, which no producer (the Docker port carousel is the only
 * one) has ever sent.
 */
@Composable
public fun QrCodeCarouselRender(data: RenderViewData) {
    val values = remember(data) { data.stringListProp("values") }
    val labels = remember(data) { data.stringListProp("labels") }
    if (values.isEmpty()) {
        Text(
            "No QR payloads",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        return
    }

    val pagerState = rememberPagerState(pageCount = { values.size })
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (data.title.isNotEmpty()) {
            Text(data.title, style = MaterialTheme.typography.titleMedium)
        }
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 24.dp),
        ) { index ->
            val value = values[index]
            val label = labels.getOrNull(index)?.takeIf { it.isNotEmpty() } ?: value
            val image = remember(value) { generateQrCodeBitmap(value, 512) }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    image?.let {
                        Image(
                            bitmap = it,
                            contentDescription = "QR code ${index + 1} of ${values.size}",
                            filterQuality = FilterQuality.None,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        )
                    }
                }
                LinkifiedText(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    textAlign = TextAlign.Center,
                )
            }
        }
        Text(
            "${pagerState.currentPage + 1} / ${values.size}",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
        )
    }
}

private fun RenderViewData.stringListProp(key: String): List<String> =
    when (val v = props[key]) {
        is RenderPropValue.ListValue -> v.value.mapNotNull { (it as? RenderPropValue.StringValue)?.value }
        is RenderPropValue.StringValue -> listOf(v.value)
        else -> emptyList()
    }.filter { it.isNotEmpty() }
