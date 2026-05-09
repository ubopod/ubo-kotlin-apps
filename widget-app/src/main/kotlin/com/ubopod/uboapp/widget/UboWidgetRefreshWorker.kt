package com.ubopod.uboapp.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit

/**
 * Periodic refresh of the Glance widget. WorkManager runs this every
 * ~30 minutes (the system can throttle). The worker also fires when the
 * phone-app calls [enqueue].
 *
 * The phone-app calls [refreshNow] directly after writing fresh
 * [SharedSystemStats] so the launcher updates promptly while the device
 * is connected; this Worker only matters when the phone-app process is
 * gone.
 */
public class UboWidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        runCatching {
            UboGlanceAppWidget().updateAll(applicationContext)
        }
        return Result.success()
    }

    public companion object {
        private const val UNIQUE_NAME = "ubo-widget-refresh"

        public fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<UboWidgetRefreshWorker>(30, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        /**
         * Force an immediate widget refresh — invoked by the phone-app
         * after it writes fresh stats to [WidgetDataStore].
         */
        public suspend fun refreshNow(context: Context) {
            runCatching {
                UboGlanceAppWidget().updateAll(context)
                // Also nudge any widgets the user has placed.
                GlanceAppWidgetManager(context).getGlanceIds(UboGlanceAppWidget::class.java)
            }
        }
    }
}
