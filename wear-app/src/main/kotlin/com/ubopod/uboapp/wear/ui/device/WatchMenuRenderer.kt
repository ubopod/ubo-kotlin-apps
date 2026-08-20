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
import com.ubopod.uboapp.wear.ui.common.WatchTitleText
import com.ubopod.uboapp.wear.ui.common.rotaryScroll
import com.ubopod.uboapp.wear.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.models.MenuViewData
import kotlinx.coroutines.launch

/**
 * Compact menu renderer: title at top, items as chips, optional
 * page-indicator footer.
 *
 * Mirrors `WatchMenuView` from the Swift port.
 */
@Composable
public fun WatchMenuRenderer(data: MenuViewData, viewModel: DeviceViewModel) {
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()
    val items = data.items.filterNotNull()

    Scaffold(
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp).rotaryScroll(listState),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (data.title.isNotEmpty()) {
                item { WatchTitleText(title = data.title, maxChars = 18) }
            }
            data.heading?.takeIf { it.isNotEmpty() }?.let {
                item {
                    Text(
                        text = it.compactForWatch(maxChars = 22),
                        style = MaterialTheme.typography.caption2,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            item { Spacer(Modifier.height(2.dp)) }
            items(items) { item ->
                MenuChip(item) {
                    scope.launch {
                        runCatching { viewModel.client.selectMenuItem(item) }
                    }
                }
            }
            item {
                Chip(
                    onClick = { scope.launch { runCatching { viewModel.client.goBack() } } },
                    label = { Text("Back") },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth(0.95f),
                )
            }
            if (data.totalPages > 1) {
                item {
                    Text(
                        "${data.pageIndex + 1} / ${data.totalPages}",
                        style = MaterialTheme.typography.caption3,
                    )
                }
            }
        }
    }
}
