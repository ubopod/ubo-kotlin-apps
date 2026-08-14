package com.ubopod.uboapp.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

/**
 * Compose-Glance based home-screen widget rendering [SharedSystemStats]
 * from the phone-app. Uses [SizeMode.Responsive] to pick one of three
 * layouts based on the launcher slot — Small (2x2), Medium (4x2),
 * Large (4x4) — mirroring the watchOS WidgetKit's three system sizes.
 *
 * Mirrors `ubo-swift-app/UboWidgets/UboWidgets.swift` (`.systemSmall`,
 * `.systemMedium`, `.systemLarge`).
 */
public class UboGlanceAppWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SMALL_SIZE, MEDIUM_SIZE, LARGE_SIZE),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val stats = WidgetDataStore.load(context)
        provideContent {
            UboWidgetContent(stats)
        }
    }

    public companion object {
        public val SMALL_SIZE: androidx.compose.ui.unit.DpSize =
            androidx.compose.ui.unit.DpSize(120.dp, 120.dp)
        public val MEDIUM_SIZE: androidx.compose.ui.unit.DpSize =
            androidx.compose.ui.unit.DpSize(240.dp, 120.dp)
        public val LARGE_SIZE: androidx.compose.ui.unit.DpSize =
            androidx.compose.ui.unit.DpSize(240.dp, 240.dp)
    }
}

@Composable
private fun UboWidgetContent(stats: SharedSystemStats) {
    val size = LocalSize.current
    GlanceTheme {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(20.dp)
                .background(ColorProvider(Color(0xFF161616), Color(0xFF161616)))
                .padding(12.dp),
        ) {
            when {
                size.width < 200.dp -> SmallLayout(stats)
                size.height < 200.dp -> MediumLayout(stats)
                else -> LargeLayout(stats)
            }
        }
    }
}

@Composable
private fun SmallLayout(stats: SharedSystemStats) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(stats.isConnected)
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = stats.deviceHost.ifEmpty { "Ubo" },
                style = TextStyle(color = textPrimary(stats), fontSize = 11.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
        StatTile(label = "CPU", percent = stats.cpuPercent, stale = stats.isStale)
        Spacer(modifier = GlanceModifier.height(4.dp))
        StatTile(label = "RAM", percent = stats.ramPercent, stale = stats.isStale)
    }
}

@Composable
private fun MediumLayout(stats: SharedSystemStats) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        HeaderRow(stats)
        Spacer(modifier = GlanceModifier.height(8.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatTile(label = "CPU", percent = stats.cpuPercent, stale = stats.isStale, modifier = GlanceModifier.defaultWeight())
            Spacer(modifier = GlanceModifier.width(8.dp))
            StatTile(label = "RAM", percent = stats.ramPercent, stale = stats.isStale, modifier = GlanceModifier.defaultWeight())
        }
        if (stats.isStale) {
            Spacer(modifier = GlanceModifier.height(6.dp))
            StaleHint()
        }
    }
}

@Composable
private fun LargeLayout(stats: SharedSystemStats) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        HeaderRow(stats)
        Spacer(modifier = GlanceModifier.height(12.dp))
        StatTile(label = "CPU", percent = stats.cpuPercent, stale = stats.isStale, modifier = GlanceModifier.fillMaxWidth(), large = true)
        Spacer(modifier = GlanceModifier.height(8.dp))
        StatTile(label = "RAM", percent = stats.ramPercent, stale = stats.isStale, modifier = GlanceModifier.fillMaxWidth(), large = true)
        stats.temperature?.let {
            Spacer(modifier = GlanceModifier.height(8.dp))
            StatTile(
                label = "Temp",
                percent = it,
                stale = stats.isStale,
                modifier = GlanceModifier.fillMaxWidth(),
                large = true,
                suffix = stats.temperatureUnit ?: "°C",
                accentOverride = temperatureAccent(it, stats.temperatureUnit),
            )
        }
        if (stats.isStale) {
            Spacer(modifier = GlanceModifier.height(8.dp))
            StaleHint()
        }
    }
}

@Composable
private fun HeaderRow(stats: SharedSystemStats) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(stats.isConnected)
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = stats.deviceHost.ifEmpty { "Ubo" },
                style = TextStyle(color = textPrimary(stats), fontSize = 13.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
            Text(
                text = if (stats.isConnected) "Connected" else "Disconnected",
                style = TextStyle(color = TextSecondary, fontSize = 11.sp),
                maxLines = 1,
            )
        }
        stats.temperature?.let {
            Text(
                text = "${it.toInt()}${stats.temperatureUnit ?: "°C"}",
                style = TextStyle(color = textPrimary(stats), fontSize = 12.sp, fontWeight = FontWeight.Medium),
            )
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    percent: Float,
    stale: Boolean,
    modifier: GlanceModifier = GlanceModifier,
    large: Boolean = false,
    suffix: String = "%",
    accentOverride: androidx.glance.unit.ColorProvider? = null,
) {
    val accent = if (stale) ColorProvider(Color(0xFF4A4A4A), Color(0xFF4A4A4A))
                 else accentOverride ?: gaugeAccent(percent)
    Box(
        modifier = modifier
            .padding(if (large) 14.dp else 10.dp)
            .cornerRadius(14.dp)
            .background(accent),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${percent.toInt()}${suffix}",
                style = TextStyle(
                    color = TextPrimary,
                    fontSize = if (large) 28.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = label,
                style = TextStyle(color = TextSecondary, fontSize = if (large) 11.sp else 10.sp),
            )
        }
    }
}

@Composable
private fun StatusDot(connected: Boolean) {
    Box(
        modifier = GlanceModifier
            .size(10.dp)
            .cornerRadius(5.dp)
            .background(
                ColorProvider(
                    if (connected) Color(0xFF34C759) else Color(0xFFFF3B30),
                    if (connected) Color(0xFF34C759) else Color(0xFFFF3B30),
                ),
            ),
    ) {}
}

@Composable
private fun StaleHint() {
    Text(
        text = "stale — phone may be offline",
        style = TextStyle(color = TextSecondary, fontSize = 10.sp),
    )
}

private fun gaugeAccent(percent: Float) = run {
    val color = when {
        percent >= 85f -> Color(0xFFCC4040) // red
        percent >= 65f -> Color(0xFFB07A20) // amber
        else -> Color(0xFF1F1F1F) // neutral
    }
    ColorProvider(color, color)
}

/**
 * [stats].temperature is already converted to the device's effective unit
 * system server-side; the hot/warm thresholds move with it, since they only
 * mean anything in Celsius otherwise.
 */
private fun temperatureAccent(temperature: Float, unit: String?) = run {
    val (hot, warm) = if (unit == "°F") 158f to 122f else 70f to 50f
    val color = when {
        temperature >= hot -> Color(0xFFCC4040)
        temperature >= warm -> Color(0xFFB07A20)
        else -> Color(0xFF1F1F1F)
    }
    ColorProvider(color, color)
}

private fun textPrimary(stats: SharedSystemStats) =
    if (stats.isStale) TextSecondary else TextPrimary

private val TextPrimary = ColorProvider(Color(0xFFFFFFFF), Color(0xFFFFFFFF))
private val TextSecondary = ColorProvider(Color(0xFFB0B0B0), Color(0xFFB0B0B0))
