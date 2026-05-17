package com.ubopod.uboapp.wear.tile

import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.Layout
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.guava.future

/**
 * Wear OS Tile rendering the latest `SystemStats` written by the wear-app's
 * `DeviceViewModel` to [WearStatsStore]. Counterpart to watchOS WidgetKit's
 * `accessoryCircular` / `accessoryRectangular` / `accessoryInline`
 * complication families.
 *
 * Mirrors the iOS widget surface set; the Tile API uses ProtoLayout
 * rather than Compose, so the layout is built declaratively here. Static
 * single-frame timeline — the wear-app pushes refreshes by calling
 * `TileService.getUpdater(ctx).requestUpdate(UboTileService::class.java)`.
 */
public class UboTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onTileResourcesRequest(
        request: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> = Futures.immediateFuture(
        ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build(),
    )

    override fun onTileRequest(
        request: RequestBuilders.TileRequest,
    ): ListenableFuture<TileBuilders.Tile> = scope.future {
        val snapshot = WearStatsStore.load(applicationContext)
        val layout = Layout.Builder().setRoot(renderTile(snapshot)).build()
        val entry = TimelineBuilders.TimelineEntry.Builder()
            .setLayout(layout)
            .build()
        TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(TimelineBuilders.Timeline.Builder().addTimelineEntry(entry).build())
            .build()
    }

    private fun renderTile(snapshot: WearStatsStore.Snapshot): LayoutElement {
        val accent = if (snapshot.connected) 0xFF34C759.toInt() else 0xFFFF3B30.toInt()
        val muted = snapshot.isStale
        val primaryColor = if (muted) 0xFFB0B0B0.toInt() else 0xFFFFFFFF.toInt()
        val secondaryColor = 0xFFB0B0B0.toInt()

        val cpuLine = chip("CPU", "${snapshot.cpuPercent.toInt()}%", primaryColor, secondaryColor)
        val ramLine = chip("RAM", "${snapshot.ramPercent.toInt()}%", primaryColor, secondaryColor)
        val tempLine = snapshot.temperature?.let {
            chip("TEMP", "${it.toInt()}°C", primaryColor, secondaryColor)
        }

        val statusText = Text.Builder()
            .setText(snapshot.host.ifEmpty { "Ubo" } + " · " + if (snapshot.connected) "connected" else "offline")
            .setFontStyle(
                FontStyle.Builder()
                    .setSize(sp(12f))
                    .setColor(argb(if (snapshot.connected) accent else secondaryColor))
                    .build(),
            )
            .build()

        val column = Column.Builder()
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
            .addContent(statusText)
            .addContent(Spacer.Builder().setHeight(dp(6f)).build())
            .addContent(cpuLine)
            .addContent(Spacer.Builder().setHeight(dp(3f)).build())
            .addContent(ramLine)
            .apply {
                if (tempLine != null) {
                    addContent(Spacer.Builder().setHeight(dp(3f)).build())
                    addContent(tempLine)
                }
                if (muted) {
                    addContent(Spacer.Builder().setHeight(dp(4f)).build())
                    addContent(
                        Text.Builder()
                            .setText("stale")
                            .setFontStyle(
                                FontStyle.Builder()
                                    .setSize(sp(10f))
                                    .setColor(argb(secondaryColor))
                                    .build(),
                            )
                            .build(),
                    )
                }
            }
            .build()

        return Box.Builder()
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
            .setWidth(androidx.wear.protolayout.DimensionBuilders.expand())
            .setHeight(androidx.wear.protolayout.DimensionBuilders.expand())
            .addContent(column)
            .build()
    }

    private fun chip(label: String, value: String, primary: Int, secondary: Int): Row {
        val labelText = Text.Builder()
            .setText("$label ")
            .setFontStyle(FontStyle.Builder().setSize(sp(11f)).setColor(argb(secondary)).build())
            .build()
        val valueText = Text.Builder()
            .setText(value)
            .setFontStyle(FontStyle.Builder().setSize(sp(14f)).setColor(argb(primary)).build())
            .build()
        return Row.Builder()
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .addContent(labelText)
            .addContent(valueText)
            .build()
    }

    private companion object {
        private const val RESOURCES_VERSION = "1"
    }
}
