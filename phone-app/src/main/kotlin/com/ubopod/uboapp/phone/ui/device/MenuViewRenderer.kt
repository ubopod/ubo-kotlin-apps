package com.ubopod.uboapp.phone.ui.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ubopod.uboapp.phone.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.models.MenuViewData
import kotlinx.coroutines.launch

/**
 * Renders a [MenuViewData] as a vertical list. Tapping an item dispatches
 * via `UboClient.selectMenuItem(item)` over gRPC. Row layout shared with
 * [HomeViewRenderer] via [MenuItemRow].
 *
 * Mirrors the menu-rendering branch of
 * `ubo-swift-app/ubo-swift-app/Views/Device/DeviceView.swift`.
 */
@Composable
public fun MenuViewRenderer(data: MenuViewData, viewModel: DeviceViewModel) {
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxWidth()) {
        if (!data.heading.isNullOrEmpty() || !data.subHeading.isNullOrEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                data.heading?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                data.subHeading?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Pagination is driven server-side now (Swift commit `44c84ab`);
        // the page chip used to live here but only added noise — the
        // device already shows its own pagination indicator and the
        // user can't change it from the client anyway.
        if (data.items.isEmpty()) {
            Text(
                "No items in this menu.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            itemsIndexed(data.items) { index, item ->
                if (item == null) {
                    Spacer(Modifier.size(8.dp))
                } else {
                    MenuItemRow(item) {
                        scope.launch {
                            runCatching { viewModel.client.selectMenuItem(item) }
                        }
                    }
                    if (index < data.items.lastIndex && data.items[index + 1] != null) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
        }
    }
}
