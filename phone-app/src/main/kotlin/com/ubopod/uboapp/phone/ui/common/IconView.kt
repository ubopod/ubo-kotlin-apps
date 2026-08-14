package com.ubopod.uboapp.phone.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubopod.uboapp.phone.R

/**
 * Compose-side font reference for the bundled Nerd Font glyph asset.
 *
 * Mirrors the Swift `UboIconFont.family = "Arimo Nerd Font"` constant.
 * The TTF lives at `res/font/arimo_nerd.ttf` so callers can route a
 * `Text` through it via this [FontFamily] without going through the
 * platform font registry.
 */
public val UboNerdFontFamily: FontFamily = FontFamily(Font(R.font.arimo_nerd))

/**
 * Renders the icon strings the Ubo core emits.
 *
 * The Python core writes icons as Nerd-Font Unicode-Private-Use codepoints
 * (e.g. `9` is wifi). When the payload starts with a PUA glyph we
 * render it directly via the bundled `arimo_nerd.ttf`; otherwise the icon
 * is a semantic key (`"wifi"`, `"settings"`, …) which we look up against
 * Material Icons via [SymbolMapper].
 *
 * Mirrors `ubo-swift-app/ubo-swift-app/Views/Icons/IconView.swift`.
 */
@Composable
public fun IconView(
    icon: String,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    if (isUboNerdGlyph(icon)) {
        // The Nerd Font glyph case: render the codepoint as text so the
        // bundled TTF picks it up. Material `Icon` only takes ImageVectors.
        Text(
            text = icon,
            style = TextStyle(
                fontFamily = UboNerdFontFamily,
                fontSize = size.value.sp,
                color = tint,
            ),
            modifier = modifier.size(size),
        )
    } else if (icon.isNotEmpty()) {
        Icon(
            imageVector = SymbolMapper.imageVector(icon),
            contentDescription = icon,
            tint = tint,
            modifier = modifier.size(size),
        )
    }
}

/**
 * True when the first codepoint of [s] is in a Unicode Private-Use Area —
 * the convention the Ubo core uses for Nerd-Font glyphs.
 *
 * Mirrors Swift `isUboNerdGlyph(_:)`.
 */
public fun isUboNerdGlyph(s: String): Boolean {
    if (s.isEmpty()) return false
    val cp = s.codePointAt(0)
    return cp in 0xE000..0xF8FF ||
        cp in 0xF0000..0xFFFFD ||
        cp in 0x100000..0x10FFFD
}

/**
 * Split a label that may begin with a Nerd-Font glyph (the Python core
 * does this for status-bar titles like `\u{F005C}ubo-6j.local`) into the
 * leading glyph and the remaining text.
 *
 * Returns `(null, original)` when there's no leading PUA codepoint.
 *
 * Mirrors Swift `splitLeadingGlyph(_:)`.
 */
public fun splitLeadingGlyph(s: String): Pair<String?, String> {
    if (s.isEmpty()) return null to s
    val cp = s.codePointAt(0)
    val isPua = cp in 0xE000..0xF8FF ||
        cp in 0xF0000..0xFFFFD ||
        cp in 0x100000..0x10FFFD
    if (!isPua) return null to s
    val charCount = Character.charCount(cp)
    return s.substring(0, charCount) to s.substring(charCount)
}

/**
 * Resolve a `MenuItemData.color`-style hex string to a [Color], treating
 * the GUI client's default `#ffffff` as "use the system primary color".
 * Empty / unparseable strings also fall through to the system primary.
 *
 * Mirrors Swift `uboIconColor(forHex:fallback:)`.
 */
@Composable
public fun uboIconColor(hex: String, fallback: Color = MaterialTheme.colorScheme.onSurface): Color {
    val normalised = hex.lowercase()
    if (normalised.isEmpty() || normalised == "#ffffff" || normalised == "#fff") return fallback
    return parseHexToCompose(hex) ?: fallback
}

/**
 * Resolve a `MenuItemData.backgroundColor`-style hex string to a tinted
 * background [Color] for the icon badge, or null when unset/unparseable
 * (no badge). Mirrors Swift `MenuItemRow`'s
 * `item.backgroundColor.flatMap({ Color(hex: $0) })?.opacity(0.2)`.
 */
public fun uboIconBackgroundColor(hex: String?): Color? =
    hex?.let { parseHexToCompose(it)?.copy(alpha = 0.2f) }

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

/**
 * Default size constant — matches the Swift call sites' `size: 16` default.
 */
public val DEFAULT_ICON_SIZE: TextUnit = 16.sp
