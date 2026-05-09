package com.ubopod.uboapp.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
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
 * Compose-Glance based home-screen widget surfacing the latest
 * [SharedSystemStats] from the phone-app.
 *
 * Mirrors `ubo-swift-app/UboWidgets/UboWidgets.swift`. Single responsive
 * layout for now; the iOS counterpart ships six surfaces — we'll grow
 * this widget if/when the launchers ask for distinct accessory layouts.
 */
public class UboGlanceAppWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val stats = WidgetDataStore.load(context)
        provideContent {
            UboWidgetContent(stats)
        }
    }
}

@Composable
private fun UboWidgetContent(stats: SharedSystemStats) {
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp)
                .cornerRadius(20.dp)
                .background(ColorProvider(Color(0xFF161616), Color(0xFF161616))),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeaderRow(stats)
            Spacer(modifier = GlanceModifier.height(8.dp))
            StatsRow(stats)
            if (stats.isStale) {
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = "stale — phone may be offline",
                    style = TextStyle(color = TextSecondary, fontSize = 10.sp),
                )
            }
        }
    }
}

@Composable
private fun HeaderRow(stats: SharedSystemStats) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(connected = stats.isConnected)
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = stats.deviceHost.ifEmpty { "Ubo" },
                style = TextStyle(color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium),
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
                text = "${it.toInt()}°C",
                style = TextStyle(color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium),
            )
        }
    }
}

@Composable
private fun StatsRow(stats: SharedSystemStats) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatTile(label = "CPU", percent = stats.cpuPercent, modifier = GlanceModifier.defaultWeight())
        Spacer(modifier = GlanceModifier.width(8.dp))
        StatTile(label = "RAM", percent = stats.ramPercent, modifier = GlanceModifier.defaultWeight())
    }
}

@Composable
private fun StatTile(label: String, percent: Float, modifier: GlanceModifier = GlanceModifier) {
    Box(
        modifier = modifier
            .padding(10.dp)
            .cornerRadius(14.dp)
            .background(ColorProvider(Color(0xFF1F1F1F), Color(0xFF1F1F1F))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${percent.toInt()}%",
                style = TextStyle(color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold),
            )
            Text(
                text = label,
                style = TextStyle(color = TextSecondary, fontSize = 10.sp),
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

private val TextPrimary = ColorProvider(Color(0xFFFFFFFF), Color(0xFFFFFFFF))
private val TextSecondary = ColorProvider(Color(0xFFB0B0B0), Color(0xFFB0B0B0))
