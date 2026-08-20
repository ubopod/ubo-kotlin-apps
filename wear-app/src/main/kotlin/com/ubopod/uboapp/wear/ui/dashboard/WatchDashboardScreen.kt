package com.ubopod.uboapp.wear.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ubopod.uboapp.wear.ui.dashboard.pages.WatchAppsPage
import com.ubopod.uboapp.wear.ui.dashboard.pages.WatchSensorPage
import com.ubopod.uboapp.wear.ui.dashboard.pages.WatchSystemPage
import com.ubopod.uboapp.wear.ui.dashboard.pages.WatchWeatherDateTimePage
import com.ubopod.uboapp.wear.viewmodel.DeviceViewModel

/**
 * Paginated dashboard mirroring `ubo Watch App/Views/WatchDashboardView.swift`:
 * System (CPU/RAM/Storage/temp/uptime) -> Weather/Date/Time -> Apps -> one
 * page per connected sensor device. The sensor page count is dynamic — it
 * tracks `stats.sensorDevices` directly, so adding/removing a sensor
 * changes the page count without restarting the app. Swipes horizontally
 * between pages; the outer shell (`WatchContentScreen`) still pages
 * horizontally between tabs too, but Wear's edge-swipe-to-dismiss and
 * this in-page pager don't collide since the pager only responds to drags
 * that start on the page content.
 */
@Composable
public fun WatchDashboardScreen(viewModel: DeviceViewModel) {
    val stats by viewModel.systemStats.collectAsStateWithLifecycle()
    val currentStats = stats

    if (currentStats == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Waiting for readings…", style = MaterialTheme.typography.caption2)
        }
        return
    }

    val hasApps = currentStats.dockerApps.isNotEmpty()
    val appsPageIndex = if (hasApps) 2 else -1
    val sensorPageOffset = 2 + (if (hasApps) 1 else 0)
    val pageCount = sensorPageOffset + currentStats.sensorDevices.size
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })

    // No page indicator here — WatchContentScreen already renders one for
    // the outer 5-tab pager. Stacking a second one at the same
    // bottom-center position for this inner pager made both unreadable.
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        when {
            page == 0 -> WatchSystemPage(currentStats)
            page == 1 -> WatchWeatherDateTimePage(currentStats)
            page == appsPageIndex -> WatchAppsPage(currentStats.dockerApps)
            else -> currentStats.sensorDevices.getOrNull(page - sensorPageOffset)?.let { device ->
                WatchSensorPage(device)
            }
        }
    }
}
