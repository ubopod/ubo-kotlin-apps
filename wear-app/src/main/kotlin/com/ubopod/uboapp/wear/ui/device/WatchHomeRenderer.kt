package com.ubopod.uboapp.wear.ui.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.ubopod.uboapp.wear.ui.common.WatchIconView
import com.ubopod.uboapp.wear.ui.common.watchUboIconColor
import com.ubopod.uboapp.wear.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.models.HomeViewData
import com.ubopod.ubokotlin.models.MenuItemData
import kotlinx.coroutines.launch

/**
 * Compact rendering of the device's home view: a top header summarising
 * CPU / RAM / volume, then a scrolling list of menu items as chips.
 *
 * Mirrors `WatchHomeView` from the Swift port.
 */
@Composable
public fun WatchHomeRenderer(data: HomeViewData, viewModel: DeviceViewModel) {
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()

    Scaffold(
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Text(
                    text = formatStatsHeader(data),
                    style = MaterialTheme.typography.caption2,
                    textAlign = TextAlign.Center,
                )
            }
            item { Spacer(Modifier.height(4.dp)) }
            items(data.menuItems) { item ->
                MenuChip(item) {
                    scope.launch {
                        runCatching {
                            if (!item.actionId.isNullOrEmpty()) {
                                viewModel.client.selectMenuItem(item)
                            } else if (item.icon.isNotEmpty()) {
                                // Home items can be icon-only on the Pi
                                // panel — prefer icon-based dispatch so
                                // entries like Notifications / Power
                                // resolve correctly (mirrors the phone-app
                                // HomeViewRenderer + Swift WatchHomeView).
                                viewModel.client.selectMenuItemByIcon(item.icon)
                            } else {
                                viewModel.client.selectMenuItem(label = item.label)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MenuChip(item: MenuItemData, onTap: () -> Unit) {
    val icon = item.icon
    Chip(
        onClick = onTap,
        label = {
            Text(
                text = item.label.ifEmpty { item.key }.compactForWatch(),
                maxLines = 1,
            )
        },
        icon = if (icon.isNotEmpty()) {
            { WatchIconView(icon = icon, tint = watchUboIconColor(item.color)) }
        } else {
            null
        },
        colors = ChipDefaults.primaryChipColors(),
        modifier = Modifier.fillMaxWidth(0.95f),
    )
}

private fun formatStatsHeader(data: HomeViewData): String =
    "CPU ${data.cpuPercent.toInt()}%  •  RAM ${data.ramPercent.toInt()}%  •  Vol ${(data.volumeLevel * 100).toInt()}%"

/** Truncate to a length that fits a watch chip without wrapping. */
internal fun String.compactForWatch(maxChars: Int = 14): String =
    if (length <= maxChars) this else "${take(maxChars - 1)}…"
