package com.ubopod.uboapp.phone.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

/**
 * Convert markup-bearing text into a Compose [AnnotatedString] with bold /
 * italic / underline / colour applied per-segment.
 *
 * Mirrors `ubo-swift-app/ubo-swift-app/Utilities/Markup.swift`. Supported
 * tags: `[b]`, `[i]`, `[u]`, `[color=#hex]`. `[size=N]` and any
 * unrecognised bracketed tag (e.g. `[ref=…]`) are dropped, **not**
 * rendered as literal `[…]`.
 *
 * Returns a plain [AnnotatedString] for inputs with no `[` so unmarked
 * strings stay cheap.
 */
public fun markupAnnotated(raw: String): AnnotatedString {
    if (!raw.contains('[')) return AnnotatedString(raw)
    return buildAnnotatedString {
        for (segment in parseMarkupSegments(raw)) {
            val style = SpanStyle(
                fontWeight = if (segment.bold) FontWeight.Bold else null,
                fontStyle = if (segment.italic) FontStyle.Italic else null,
                textDecoration = if (segment.underline) TextDecoration.Underline else null,
                color = segment.color ?: Color.Unspecified,
            )
            withStyle(style) { append(segment.text) }
        }
    }
}

/**
 * Strip every recognised bracketed tag, leaving the inner text. For
 * places that need a plain [String] (titles, search, accessibility).
 */
public fun stripMarkup(raw: String): String {
    if (!raw.contains('[')) return raw
    return raw.replace(MARKUP_TAG_REGEX, "")
}

private val MARKUP_TAG_REGEX = Regex("""\[/?(?:b|i|u|color|size|ref|anchor)(?:=[^]]+)?]""")

private inline fun AnnotatedString.Builder.withStyle(style: SpanStyle, block: AnnotatedString.Builder.() -> Unit) {
    val mark = pushStyle(style)
    try {
        block()
    } finally {
        pop(mark)
    }
}

private data class MarkupSegment(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val color: Color? = null,
)

private val MARKUP_PATTERN = Regex("""\[(/?)([a-zA-Z]+)(?:=([^]]+))?]""")

private val RECOGNISED_TAGS = setOf("b", "i", "u", "color", "size")

private fun parseMarkupSegments(raw: String): List<MarkupSegment> {
    val matches = MARKUP_PATTERN.findAll(raw).toList()
    if (matches.isEmpty()) return listOf(MarkupSegment(text = raw))

    val segments = mutableListOf<MarkupSegment>()
    val stack = ArrayDeque<MarkupSegment>()
    stack.addLast(MarkupSegment(text = ""))
    var cursor = 0

    for (match in matches) {
        val range = match.range
        if (range.first > cursor) {
            val top = stack.last()
            segments += top.copy(text = raw.substring(cursor, range.first))
        }
        cursor = range.last + 1

        val tag = (match.groupValues[2]).lowercase()
        if (tag !in RECOGNISED_TAGS) continue

        val isClosing = match.groupValues[1] == "/"
        if (isClosing) {
            if (stack.size > 1) stack.removeLast()
            continue
        }

        var top = stack.last()
        when (tag) {
            "b" -> top = top.copy(bold = true)
            "i" -> top = top.copy(italic = true)
            "u" -> top = top.copy(underline = true)
            "color" -> {
                val hex = match.groupValues.getOrNull(3).orEmpty()
                parseHexColor(hex)?.let { top = top.copy(color = it) }
            }
            "size" -> { /* Compose has no per-segment relative size; ignored. */ }
        }
        stack.addLast(top)
    }

    if (cursor < raw.length) {
        val top = stack.last()
        segments += top.copy(text = raw.substring(cursor))
    }
    return segments
}

/** Parse `#rrggbb` or `rrggbb` (or with alpha) into a Compose [Color]. */
private fun parseHexColor(input: String): Color? {
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
