package com.ubopod.uboapp.wear.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

private val StrokeWidth = 5.dp

/**
 * Small circular gauge shared by the Wear Dashboard pages (System page's
 * CPU/RAM/Storage, and per-entity gauges on sensor pages). Wear-local
 * counterpart to the watchOS app's `WatchCompactGauge.swift`.
 *
 * The value text shrinks with its own length (see [valueTextStyle]) and is
 * hard-width-clamped to the ring's inner diameter as a fallback — a
 * fixed-size [Box] doesn't clip overflowing content on its own, so without
 * both of these a longer reading (e.g. "34.1") paints outside the ring
 * instead of inside it.
 */
@Composable
public fun WatchCompactGauge(
    fraction: Float,
    valueText: String,
    label: String,
    color: Color,
    size: Dp = 56.dp,
    labelWidth: Dp = size,
    // The unit string as reported by the entity itself (displayUnit/unit
    // from the gRPC reading) — never hardcode a unit here, since it's
    // server-driven (°C vs °F, ppm, µg/m³, lx, ...). Null/blank omits the
    // sub-label entirely, matching System page's %-embedded-in-valueText
    // gauges, which don't need one.
    unit: String? = null,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.caption2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(labelWidth),
        )
        Spacer(Modifier.height(2.dp))
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = fraction.coerceIn(0f, 1f),
                modifier = Modifier.fillMaxSize(),
                strokeWidth = StrokeWidth,
                indicatorColor = color,
                // The default track alpha is nearly invisible against a
                // pure black background, so a mostly-empty gauge (e.g. a
                // low CPU%) reads as a thin crescent instead of a full
                // ring — making it look smaller than a mostly-filled one
                // at the exact same diameter. An explicit, visible track
                // keeps every gauge reading as the same-size circle
                // regardless of its fill amount.
                trackColor = MaterialTheme.colors.onSurface.copy(alpha = 0.25f),
            )
            val hasUnit = !unit.isNullOrBlank()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(size - StrokeWidth * 2 - 6.dp),
            ) {
                Text(
                    valueText,
                    // A bit smaller whenever a unit line is also present,
                    // so both lines fit inside the same ring instead of
                    // the pair overflowing it vertically.
                    style = valueTextStyle(valueText, size, extraScale = if (hasUnit) 0.85f else 1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (hasUnit) {
                    Text(
                        unit!!,
                        style = unitTextStyle(size),
                        color = MaterialTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** [size] a gauge's text scales are calibrated against 1:1 (no scaling). */
private val ReferenceGaugeSize = 56.dp

/**
 * Scales the value's font size two ways: with the gauge's own diameter
 * (a 72dp sensor-page gauge gets visibly bigger text than a 46dp
 * System-page one, instead of every gauge sharing one fixed size
 * regardless of how much room it actually has — "large circle, larger
 * text"), and down as its character count grows, so a 1-2 digit
 * percentage and a 4-5 char decimal reading (e.g. "34.1") both fit
 * inside their own ring instead of the longer one overflowing it.
 */
@Composable
private fun valueTextStyle(text: String, size: Dp, extraScale: Float = 1f) = MaterialTheme.typography.caption1.let { base ->
    val sizeScale = (size.value / ReferenceGaugeSize.value).coerceIn(0.65f, 1.5f)
    val lengthScale = when (text.length) {
        0, 1, 2 -> 1f
        3 -> 0.85f
        4 -> 0.72f
        else -> 0.6f
    }
    base.copy(fontSize = base.fontSize * sizeScale * lengthScale * extraScale)
}

/** The unit sub-label under the value — smaller and dimmer, same idea as the Web UI's gauges. */
@Composable
private fun unitTextStyle(size: Dp) = MaterialTheme.typography.caption3.let { base ->
    val sizeScale = (size.value / ReferenceGaugeSize.value).coerceIn(0.65f, 1.5f)
    base.copy(fontSize = base.fontSize * sizeScale)
}
