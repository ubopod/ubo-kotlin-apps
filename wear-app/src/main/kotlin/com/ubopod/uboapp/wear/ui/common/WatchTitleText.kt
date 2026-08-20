package com.ubopod.uboapp.wear.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ubopod.uboapp.wear.ui.device.compactForWatch

/**
 * Title text for a device view (menu title, notification title, etc.).
 * These titles can carry a leading Nerd-Font/Material glyph codepoint
 * baked into the string itself (e.g. `" Main"`), same as the
 * server-driven items rendered by [WatchIconView] elsewhere — rendering
 * that raw codepoint through the default font (as plain [Text] does)
 * shows a tofu box instead of the icon. Mirrors `splitLeadingGlyph` +
 * `IconView` in `WatchDeviceView.swift`.
 */
@Composable
public fun WatchTitleText(
    title: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.title3,
    maxChars: Int = 20,
) {
    val (glyph, label) = splitWatchLeadingGlyph(title)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glyph != null) {
            WatchIconView(icon = glyph, size = 14.dp)
        }
        Text(text = label.compactForWatch(maxChars), style = style)
    }
}
