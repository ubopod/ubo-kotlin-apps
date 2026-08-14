package com.ubopod.uboapp.phone.ui.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ubopod.uboapp.phone.ui.common.IconView
import com.ubopod.uboapp.phone.ui.common.uboIconColor
import com.ubopod.ubokotlin.models.ProgressNotificationData
import com.ubopod.ubokotlin.models.StatusBarData

/**
 * Phone-side status overlay matching what the Pi paints across the top
 * of its panel: a metric chip row (CPU / RAM / temperature, plus any
 * server-pushed icons), the clock, and a second row for in-progress
 * notifications (Docker pulls, OTA updates, etc.).
 *
 * Mirrors `ubo-swift-app/.../Views/Device/StatusBarOverlay.swift`. The
 * progress row lives on its own line so long-running tasks can't squeeze
 * the metric chips into wrapping (Swift commit `571443a`).
 */
@Composable
public fun StatusBarOverlay(
    bar: StatusBarData?,
    cpuPercent: Float,
    ramPercent: Float,
    temperature: Float?,
    temperatureUnit: String? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MetricsRow(
                cpuPercent = cpuPercent,
                ramPercent = ramPercent,
                temperature = temperature,
                temperatureUnit = temperatureUnit,
                bar = bar,
            )
            val progress = bar?.progressNotifications.orEmpty()
            if (progress.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    progress.forEach { ProgressBarChip(it) }
                }
            }
        }
    }
}

@Composable
private fun MetricsRow(
    cpuPercent: Float,
    ramPercent: Float,
    temperature: Float?,
    temperatureUnit: String?,
    bar: StatusBarData?,
) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetricChip(Icons.Filled.Speed, "${cpuPercent.toInt()}%", tint)
        MetricChip(Icons.Filled.Memory, "${ramPercent.toInt()}%", tint)
        temperature?.let { MetricChip(Icons.Filled.Thermostat, "${it.toInt()}${temperatureUnit ?: "°C"}", tint) }

        // Recording / replaying / mic-recording indicators — mirrors
        // the iOS overlay's leading symbols.
        if (bar?.isRecording == true) MetricChip(Icons.Filled.WifiTethering, "rec", tint)
        if (bar?.isReplaying == true) MetricChip(Icons.Filled.WifiTethering, "play", tint)
        if (bar?.isRecordingAudio == true) MetricChip(Icons.Filled.Mic, "mic", tint)

        val icons = bar?.icons.orEmpty()
        if (icons.isNotEmpty()) {
            HorizontalDivider(
                modifier = Modifier.height(12.dp).width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            icons.forEach { ico ->
                IconView(
                    icon = ico.symbol,
                    size = 14.dp,
                    tint = uboIconColor(ico.color, fallback = tint),
                )
            }
        }

        Box(modifier = Modifier.weight(1f))

        val clock = bar?.clock.orEmpty()
        if (clock.isNotEmpty()) {
            Text(
                text = clock,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                color = tint,
            )
        }
    }
}

@Composable
private fun MetricChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.height(14.dp),
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

@Composable
private fun ProgressBarChip(pn: ProgressNotificationData) {
    val accent = uboIconColor(pn.color, fallback = MaterialTheme.colorScheme.primary)
    Box(modifier = Modifier.width(60.dp)) {
        val progress = pn.progress
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                color = accent,
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
        } else {
            // `null` ⇒ indeterminate spinner the same width as a
            // determinate bar so the row's columns line up visually
            // regardless of progress shape.
            CircularProgressIndicator(
                color = accent,
                strokeWidth = 2.dp,
                modifier = Modifier.height(12.dp),
            )
        }
    }
}
