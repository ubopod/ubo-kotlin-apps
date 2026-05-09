package com.ubopod.uboapp.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Receiver registered in the consuming app's manifest. Glance's
 * [GlanceAppWidgetReceiver] handles the standard
 * `APPWIDGET_UPDATE` / `APPWIDGET_OPTIONS_CHANGED` broadcasts and
 * routes them through [glanceAppWidget].
 */
public class UboWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UboGlanceAppWidget()
}
