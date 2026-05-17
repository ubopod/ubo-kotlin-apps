package com.ubopod.uboapp.wear.ui.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ubopod.uboapp.wear.ui.common.WatchIconView
import com.ubopod.uboapp.wear.ui.common.watchUboIconColor
import com.ubopod.ubokotlin.models.ProgressNotificationData
import com.ubopod.ubokotlin.models.StatusBarData

/**
 * Compact watch overlay: CPU/RAM/temp inline, server-pushed icons, clock,
 * and a second row for in-progress notifications. Mirrors
 * `ubo Watch App/Views/WatchStatusBarOverlay.swift` (commits `b142fcf` +
 * `571443a`).
 */
@Composable
public fun WatchStatusBarOverlay(
    bar: StatusBarData?,
    cpuPercent: Float,
    ramPercent: Float,
    temperature: Float?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniMetric("CPU", cpuPercent.toInt())
            MiniMetric("RAM", ramPercent.toInt())
            temperature?.let { MiniMetric("T", it.toInt(), suffix = "°") }

            val icons = bar?.icons.orEmpty()
            icons.take(2).forEach {
                WatchIconView(
                    icon = it.symbol,
                    size = 10.dp,
                    tint = watchUboIconColor(it.color, fallback = MaterialTheme.colors.onSurfaceVariant),
                )
            }

            Box(modifier = Modifier.weight(1f))

            val clock = bar?.clock.orEmpty()
            if (clock.isNotEmpty()) {
                Text(
                    text = clock,
                    style = MaterialTheme.typography.caption3.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colors.onSurfaceVariant,
                )
            }
        }
        val progress = bar?.progressNotifications.orEmpty()
        if (progress.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                progress.forEach { ProgressChip(it) }
            }
        }
    }
}

@Composable
private fun MiniMetric(label: String, value: Int, suffix: String = "%") {
    Text(
        "$label ${value}${suffix}",
        style = MaterialTheme.typography.caption3,
        color = MaterialTheme.colors.onSurfaceVariant,
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
