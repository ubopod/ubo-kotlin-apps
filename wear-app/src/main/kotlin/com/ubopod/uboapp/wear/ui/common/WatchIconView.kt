package com.ubopod.uboapp.wear.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ubopod.uboapp.wear.R

/**
 * Watch counterpart of the phone-app's `IconView` — renders either a
 * Nerd-Font Private-Use glyph or a Material Icon depending on the
 * payload shape. Bundled `arimo_nerd.ttf` lives at
 * `wear-app/res/font/arimo_nerd.ttf`.
 *
 * Mirrors `ubo Watch App/Views/WatchIconView.swift`.
 */
public val WatchNerdFontFamily: FontFamily = FontFamily(Font(R.font.arimo_nerd))

@Composable
public fun WatchIconView(
    icon: String,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    tint: Color = MaterialTheme.colors.onBackground,
) {
    if (isUboNerdGlyph(icon)) {
        Text(
            text = icon,
            style = TextStyle(
                fontFamily = WatchNerdFontFamily,
                fontSize = size.value.sp,
                color = tint,
            ),
            modifier = modifier.size(size),
        )
    } else if (icon.isNotEmpty()) {
        Icon(
            imageVector = watchSymbol(icon),
            contentDescription = icon,
            tint = tint,
            modifier = modifier.size(size),
        )
    }
}

public fun isUboNerdGlyph(s: String): Boolean {
    if (s.isEmpty()) return false
    val cp = s.codePointAt(0)
    return cp in 0xE000..0xF8FF || cp in 0xF0000..0xFFFFD || cp in 0x100000..0x10FFFD
}

public fun splitWatchLeadingGlyph(s: String): Pair<String?, String> {
    if (s.isEmpty()) return null to s
    val cp = s.codePointAt(0)
    val pua = cp in 0xE000..0xF8FF || cp in 0xF0000..0xFFFFD || cp in 0x100000..0x10FFFD
    if (!pua) return null to s
    val charCount = Character.charCount(cp)
    return s.substring(0, charCount) to s.substring(charCount)
}

@Composable
public fun watchUboIconColor(hex: String, fallback: Color = MaterialTheme.colors.onBackground): Color {
    val n = hex.lowercase()
    if (n.isEmpty() || n == "#ffffff" || n == "#fff") return fallback
    return parseHexToCompose(hex) ?: fallback
}

private fun parseHexToCompose(input: String): Color? {
    val s = input.trim().removePrefix("#")
    if (s.length != 6 && s.length != 8) return null
    val v = s.toLongOrNull(radix = 16) ?: return null
    return if (s.length == 6) {
        Color(
            red = ((v shr 16) and 0xFF).toInt(),
            green = ((v shr 8) and 0xFF).toInt(),
            blue = (v and 0xFF).toInt(),
            alpha = 255,
        )
    } else {
        Color(
            red = ((v shr 24) and 0xFF).toInt(),
            green = ((v shr 16) and 0xFF).toInt(),
            blue = ((v shr 8) and 0xFF).toInt(),
            alpha = (v and 0xFF).toInt(),
        )
    }
}

private fun watchSymbol(icon: String): ImageVector = when (icon.lowercase()) {
    "info" -> Icons.Filled.Info
    "warning", "alert" -> Icons.Filled.Warning
    "error", "fail", "failure" -> Icons.Filled.Block
    "success", "ok", "check", "checkmark" -> Icons.Filled.CheckCircle
    "wifi" -> Icons.Filled.Wifi
    "ssh" -> Icons.Filled.Terminal
    "vpn" -> Icons.Filled.Lock
    "docker" -> Icons.Filled.Inventory2
    "settings", "gear" -> Icons.Filled.Settings
    "power" -> Icons.Filled.PowerSettingsNew
    else -> Icons.Filled.RadioButtonUnchecked
}
