package com.ubopod.uboapp.wear.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.CurvedScope
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.foundation.curvedRow
import androidx.wear.compose.foundation.padding as curvedPadding
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.curvedText
import com.ubopod.uboapp.wear.ui.common.WatchIconView
import com.ubopod.uboapp.wear.ui.common.watchUboIconColor
import com.ubopod.ubokotlin.models.ProgressNotificationData
import com.ubopod.ubokotlin.models.StatusBarData

/**
 * Compact watch overlay: CPU/RAM/temp + a server-pushed icon, arced along
 * the top of the bezel, plus a second row for in-progress notifications.
 * Mirrors `ubo Watch App/Views/WatchStatusBarOverlay.swift` (commits
 * `b142fcf` + `571443a`).
 *
 * A flat `Row` doesn't work here: on a round screen the usable width at
 * y=0 is ~0 (the apex of the circle), so a straight status bar either
 * clips at both ends or has to be pushed down far enough to look like an
 * ugly gap under the bezel. `CurvedLayout` sidesteps the problem entirely
 * by laying the text out along the arc itself — the same primitive that
 * makes the page indicator curve correctly. Round-only — no flat-Row
 * fallback for square watches.
 */
@Composable
public fun WatchStatusBarOverlay(
    bar: StatusBarData?,
    cpuPercent: Float,
    ramPercent: Float,
    temperature: Float?,
    temperatureUnit: String? = null,
    modifier: Modifier = Modifier,
) {
    // MaterialTheme reads need an actual @Composable context; the curved
    // DSL lambdas below (curvedRow's content) aren't one, so resolve
    // these here and hand them in as plain values.
    val metricStyle = CurvedTextStyle(MaterialTheme.typography.caption3)
    val metricColor = MaterialTheme.colors.onSurfaceVariant

    Box(modifier = modifier.fillMaxSize()) {
        CurvedLayout(
            modifier = Modifier.fillMaxSize(),
            anchor = 270f,
        ) {
            curvedRow {
                curvedMetric("CPU", cpuPercent.toInt(), style = metricStyle, color = metricColor)
                curvedMetric("RAM", ramPercent.toInt(), style = metricStyle, color = metricColor)
                temperature?.let {
                    curvedMetric("T", it.toInt(), suffix = temperatureUnit ?: "°", style = metricStyle, color = metricColor)
                }

                val icons = bar?.icons.orEmpty()
                icons.take(1).forEach { icon ->
                    curvedComposable(modifier = CurvedModifier.curvedPadding(angular = 6.dp)) {
                        WatchIconView(
                            icon = icon.symbol,
                            size = 10.dp,
                            tint = watchUboIconColor(icon.color, fallback = metricColor),
                        )
                    }
                }
            }
        }

        val progress = bar?.progressNotifications.orEmpty()
        if (progress.isNotEmpty()) {
            Row(
                // Sits just under the curved arc — 44dp clears the
                // metrics text at this radius comfortably.
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 44.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                progress.forEach { ProgressChip(it) }
            }
        }
    }
}

private fun CurvedScope.curvedMetric(
    label: String,
    value: Int,
    suffix: String = "%",
    style: CurvedTextStyle,
    color: androidx.compose.ui.graphics.Color,
) {
    curvedText(
        text = "$label ${value}${suffix}",
        modifier = CurvedModifier.curvedPadding(angular = 4.dp),
        style = style,
        color = color,
    )
}

@Composable
private fun ProgressChip(pn: ProgressNotificationData) {
    val accent = watchUboIconColor(pn.color, fallback = MaterialTheme.colors.primary)
    val track = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
    val progress = pn.progress?.coerceIn(0f, 1f)
    // Wear Compose Material doesn't ship a LinearProgressIndicator, so
    // we paint a manual track + fill. Indeterminate progress is left as
    // an empty track (no spinner equivalent at this size).
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(4.dp)
            .background(track, RoundedCornerShape(2.dp)),
    ) {
        if (progress != null) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(accent, RoundedCornerShape(2.dp)),
            )
        }
    }
}
