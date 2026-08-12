package com.ubopod.uboapp.phone.ui.common

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink

/**
 * Renders [text] with any URLs in it as real, tappable links — the native
 * counterpart to the Web UI's `LinkifiedText` (ubo_app/services/090-web-ui/
 * web-app/src/inputs.tsx) and Swift's `LinkifiedText.swift`. Input
 * prompts/subtitles routinely carry a page the user has to visit (e.g. an
 * OAuth authorization URL a few hundred characters long with PKCE state);
 * shown as plain text that's either unreadable or, worse, gets shoved into
 * a single-line title and truncated. `LinkAnnotation.Url` opens the link via
 * the platform's normal URI handling, no custom click/gesture code needed.
 */
@Composable
public fun LinkifiedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(text, linkColor) { buildLinkedString(text, linkColor) }
    Text(
        text = annotated,
        modifier = modifier,
        style = style,
        color = color,
        textAlign = textAlign,
    )
}

private val urlPattern = Regex("""https?://\S+""")

// Beyond this, a URL stops being readable and starts wrecking the layout.
private const val MAX_LINK_TEXT_LENGTH = 48

private fun buildLinkedString(text: String, linkColor: Color): AnnotatedString {
    val matches = urlPattern.findAll(text).toList()
    if (matches.isEmpty()) return AnnotatedString(text)

    val linkStyles = TextLinkStyles(style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
    return buildAnnotatedString {
        var lastEnd = 0
        for (match in matches) {
            if (match.range.first > lastEnd) {
                append(text.substring(lastEnd, match.range.first))
            }
            val url = match.value
            withLink(LinkAnnotation.Url(url, linkStyles)) {
                append(shortened(url))
            }
            lastEnd = match.range.last + 1
        }
        if (lastEnd < text.length) append(text.substring(lastEnd))
    }
}

/**
 * OAuth authorization URLs run to several hundred characters of PKCE
 * challenge and state, so the visible text is shortened to host + path
 * while the link target keeps the whole thing.
 */
private fun shortened(url: String): String {
    if (url.length <= MAX_LINK_TEXT_LENGTH) return url
    val parsed = runCatching { java.net.URI(url) }.getOrNull()
    val host = parsed?.host
    if (host != null) {
        val short = host + parsed.path.orEmpty()
        return if (short.length <= MAX_LINK_TEXT_LENGTH) "$short…" else "$host…"
    }
    return url.take(MAX_LINK_TEXT_LENGTH) + "…"
}
