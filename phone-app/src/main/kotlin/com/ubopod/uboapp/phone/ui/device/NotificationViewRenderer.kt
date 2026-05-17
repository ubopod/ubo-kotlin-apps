package com.ubopod.uboapp.phone.ui.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ubopod.uboapp.phone.ui.common.HapticStrength
import com.ubopod.uboapp.phone.ui.common.IconView
import com.ubopod.uboapp.phone.ui.common.markupAnnotated
import com.ubopod.uboapp.phone.ui.common.partitionNotificationItems
import com.ubopod.uboapp.phone.ui.common.rememberHaptic
import com.ubopod.uboapp.phone.ui.common.uboIconColor
import com.ubopod.uboapp.phone.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.models.NotificationViewData
import kotlinx.coroutines.launch

/**
 * Renders an overlay notification with Web-UI parity item partitioning:
 *   - **Extra information** sits in a callout with an optional accent
 *     icon (the `extra_info` item, when present);
 *   - **Main actions** become full-width buttons in the body;
 *   - **Dismiss** is a single footer button — shown when the device
 *     sent an explicit dismiss item or when there are no main actions.
 *
 * Mirrors the notification branch of
 * `ubo-swift-app/.../Views/Device/DeviceView.swift` (commit `97667df`).
 */
@Composable
public fun NotificationViewRenderer(data: NotificationViewData, viewModel: DeviceViewModel) {
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptic()
    val accent = uboIconColor(data.color, fallback = MaterialTheme.colorScheme.primary)
    val partitioned = partitionNotificationItems(data.items)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconView(icon = data.icon, size = 56.dp, tint = accent)

        Text(
            markupAnnotated(data.title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            markupAnnotated(data.content),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (data.extraInformation.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        markupAnnotated(data.extraInformation),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    partitioned.extraInfo?.let { action ->
                        IconButton(onClick = {
                            haptic(HapticStrength.LIGHT)
                            scope.launch {
                                runCatching { viewModel.client.selectMenuItem(label = action.label) }
                            }
                        }) {
                            Icon(
                                Icons.Filled.RecordVoiceOver,
                                contentDescription = action.label,
                                tint = accent,
                            )
                        }
                    }
                }
            }
        }

        // Main action buttons — dismiss / extra_info already filtered out.
        if (partitioned.mainActions.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                partitioned.mainActions.forEach { item ->
                    Card(
                        onClick = {
                            haptic(HapticStrength.LIGHT)
                            scope.launch {
                                runCatching { viewModel.client.selectMenuItem(label = item.label) }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                markupAnnotated(item.label),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // Dismiss is offered when the device sent an explicit dismiss
        // item, or when there's nothing else actionable to keep the
        // overlay closable.
        if (partitioned.hasDismiss || partitioned.mainActions.isEmpty()) {
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = {
                    haptic(HapticStrength.LIGHT)
                    scope.launch { runCatching { viewModel.client.goBack() } }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Dismiss") }
        }
        Spacer(Modifier.size(8.dp))
    }
}
