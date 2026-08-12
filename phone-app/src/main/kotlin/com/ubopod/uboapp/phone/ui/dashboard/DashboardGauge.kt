package com.ubopod.uboapp.phone.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Shared circular gauge for Dashboard tiles (CPU/RAM/Storage/sensor
 * readings). Uses Compose's native `CircularProgressIndicator` instead of
 * porting the Web UI's hand-drawn SVG arc (`Gauge.tsx`).
 */
@Composable
public fun DashboardGauge(
    fraction: Float,
    valueText: String,
    color: Color,
    unit: String? = null,
    size: androidx.compose.ui.unit.Dp = 72.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 8.dp,
) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.size(size),
            strokeWidth = strokeWidth,
            color = color,
            trackColor = color.copy(alpha = 0.15f),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(valueText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (unit != null) {
                Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
