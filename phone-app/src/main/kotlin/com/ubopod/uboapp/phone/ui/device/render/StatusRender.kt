package com.ubopod.uboapp.phone.ui.device.render

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ubopod.uboapp.phone.ui.common.IconView
import com.ubopod.uboapp.phone.ui.common.markupAnnotated
import com.ubopod.uboapp.phone.ui.common.stringProp
import com.ubopod.ubokotlin.models.RenderViewData

/**
 * Render a status panel: an icon (from `props["icon"]`), the optional
 * title, and a status string from `props["status"]` / `"text"`. Mirrors
 * the Swift `StatusRenderView`.
 */
@Composable
public fun StatusRender(data: RenderViewData) {
    val iconKey = data.stringProp("icon")
    val statusText = data.stringProp("status", "text", "message")
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconView(icon = iconKey, size = 48.dp, tint = MaterialTheme.colorScheme.primary)
        if (data.title.isNotEmpty()) {
            Text(
                markupAnnotated(data.title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
        if (statusText.isNotEmpty()) {
            Text(
                markupAnnotated(statusText),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
