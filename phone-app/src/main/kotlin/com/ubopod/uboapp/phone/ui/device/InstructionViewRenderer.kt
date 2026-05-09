package com.ubopod.uboapp.phone.ui.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ubopod.uboapp.phone.ui.common.IconView
import com.ubopod.uboapp.phone.ui.common.markupAnnotated
import com.ubopod.uboapp.phone.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.models.InstructionViewData
import kotlinx.coroutines.delay

/**
 * Renders an instruction / "wait" view: large icon, title, instruction
 * body, optional spinner, optional countdown timer, optional progress
 * text, optional footer.
 *
 * Mirrors the instruction branch of
 * `ubo-swift-app/ubo-swift-app/Views/Device/DeviceView.swift`.
 */
@Composable
public fun InstructionViewRenderer(data: InstructionViewData, @Suppress("unused") viewModel: DeviceViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconView(icon = data.icon, size = 56.dp, tint = MaterialTheme.colorScheme.primary)

        Text(
            markupAnnotated(data.title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Text(
            markupAnnotated(data.instruction),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (data.spinner) {
            CircularProgressIndicator()
        }

        if (data.progressText.isNotEmpty()) {
            Text(
                markupAnnotated(data.progressText),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }

        if (data.timeoutSeconds > 0) {
            CountdownTimer(initialSeconds = data.timeoutSeconds)
        }

        if (data.footerText.isNotEmpty()) {
            Spacer(Modifier.size(8.dp))
            Text(
                markupAnnotated(data.footerText),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Counts down [initialSeconds] in 1 s ticks. The countdown is purely
 * presentational — the device-side reducer manages the actual timeout
 * deadline. Matches the Swift `InstructionDeviceView` countdown.
 */
@Composable
private fun CountdownTimer(initialSeconds: Int) {
    var remaining by remember(initialSeconds) { mutableIntStateOf(initialSeconds) }
    LaunchedEffect(initialSeconds) {
        while (remaining > 0) {
            delay(1_000)
            remaining -= 1
        }
    }
    Text(
        "${remaining}s remaining",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
