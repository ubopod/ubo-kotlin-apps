package com.ubopod.uboapp.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.HorizontalPageIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PageIndicatorState
import androidx.wear.compose.material.Text
import com.ubopod.uboapp.wear.ui.connection.WatchConnectionScreen
import com.ubopod.uboapp.wear.ui.controls.WatchActionsScreen
import com.ubopod.uboapp.wear.ui.controls.WatchRemoteScreen
import com.ubopod.uboapp.wear.ui.controls.WatchVolumeScreen
import com.ubopod.uboapp.wear.ui.dashboard.WatchDashboardScreen
import com.ubopod.uboapp.wear.ui.device.WatchDeviceScreen
import com.ubopod.uboapp.wear.ui.inputs.WatchInputFormSheet
import com.ubopod.uboapp.wear.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.connection.ConnectionState

/**
 * Wear root composable. Routes between connection / connecting /
 * connected and, once connected, shows a horizontally pageable shell
 * with five tabs (Device / Dashboard / Remote / Actions / Volume) to
 * mirror the watchOS tab carousel.
 *
 * Mirrors `ubo Watch App/Views/WatchContentView.swift`.
 */
@Composable
public fun WatchContentScreen(viewModel: DeviceViewModel) {
    val state by viewModel.connectionState.collectAsStateWithLifecycle()
    when {
        state == ConnectionState.CONNECTING -> ConnectingScreen()
        state.isConnected || state == ConnectionState.RECONNECTING -> ConnectedShell(viewModel)
        else -> WatchConnectionScreen(viewModel)
    }
}

@Composable
private fun ConnectingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))
            Text("Connecting…", style = MaterialTheme.typography.body2)
        }
    }
}

@Composable
private fun ConnectedShell(viewModel: DeviceViewModel) {
    val pageCount = 5
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })
    val activeInputs by viewModel.activeInputs.collectAsStateWithLifecycle()
    val firstInput = activeInputs.firstOrNull()

    if (firstInput != null) {
        WatchInputFormSheet(firstInput, viewModel, onDismiss = { /* server state clears it */ })
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> WatchDeviceScreen(viewModel)
                1 -> WatchDashboardScreen(viewModel)
                2 -> WatchRemoteScreen(viewModel)
                3 -> WatchActionsScreen(viewModel)
                4 -> WatchVolumeScreen(viewModel)
            }
        }
        HorizontalPageIndicator(
            pageIndicatorState = object : PageIndicatorState {
                override val pageOffset: Float get() = pagerState.currentPageOffsetFraction
                override val selectedPage: Int get() = pagerState.currentPage
                override val pageCount: Int get() = pageCount
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
