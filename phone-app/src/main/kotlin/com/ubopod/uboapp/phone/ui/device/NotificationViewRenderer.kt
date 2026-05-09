package com.ubopod.uboapp.phone.ui.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.ubopod.uboapp.phone.ui.common.IconView
import com.ubopod.uboapp.phone.ui.common.markupAnnotated
import com.ubopod.uboapp.phone.ui.common.uboIconColor
import com.ubopod.uboapp.phone.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.models.NotificationViewData
import kotlinx.coroutines.launch

/**
 * Renders an overlay notification: a large icon, the title and content,
 * optional extra-information block, and any action items as buttons.
 * "Dismiss" routes to `goBack()` mirroring the Swift renderer.
 *
 * Mirrors the notification branch of
 * `ubo-swift-app/ubo-swift-app/Views/Device/DeviceView.swift`.
 */
@Composable
public fun NotificationViewRenderer(data: NotificationViewData, viewModel: DeviceViewModel) {
    val scope = rememberCoroutineScope()
    val accent = uboIconColor(data.color, fallback = MaterialTheme.colorScheme.primary)

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
                Text(
                    markupAnnotated(data.extraInformation),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Action items, if any. Each routes to selectMenuItem(label = …)
        data.items.filterNotNull().forEach { item ->
            Button(
                onClick = {
                    scope.launch { runCatching { viewModel.client.selectMenuItem(label = item.label) } }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(markupAnnotated(item.label))
            }
        }

        OutlinedButton(
            onClick = { scope.launch { runCatching { viewModel.client.goBack() } } },
            colors = ButtonDefaults.outlinedButtonColors(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Dismiss")
        }
    }
}
