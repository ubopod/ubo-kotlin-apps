package com.ubopod.uboapp.phone.ui.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.ubopod.uboapp.phone.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.models.PromptViewData
import kotlinx.coroutines.launch

/**
 * Renders a confirmation prompt with icon, title, body, and one button
 * per item. Empty `items` shows a single "Dismiss" fallback that calls
 * `goBack()`.
 *
 * Mirrors the prompt branch of
 * `ubo-swift-app/ubo-swift-app/Views/Device/DeviceView.swift`.
 */
@Composable
public fun PromptViewRenderer(data: PromptViewData, viewModel: DeviceViewModel) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconView(icon = data.icon, size = 52.dp, tint = MaterialTheme.colorScheme.primary)

        Text(
            markupAnnotated(data.title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            markupAnnotated(data.prompt),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (data.items.isEmpty()) {
            OutlinedButton(
                onClick = { scope.launch { runCatching { viewModel.client.goBack() } } },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Dismiss")
            }
        } else {
            data.items.forEach { item ->
                Button(
                    onClick = {
                        scope.launch { runCatching { viewModel.client.selectMenuItem(item) } }
                    },
                    colors = ButtonDefaults.buttonColors(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(markupAnnotated(item.label))
                }
            }
        }
    }
}
