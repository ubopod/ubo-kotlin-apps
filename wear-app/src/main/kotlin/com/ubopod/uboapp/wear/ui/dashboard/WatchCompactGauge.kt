package com.ubopod.uboapp.wear.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/**
 * Small circular gauge shared by the Wear Dashboard pages (System page's
 * CPU/RAM/Storage, and per-entity gauges on sensor pages). Wear-local
 * counterpart to the watchOS app's `WatchCompactGauge.swift`.
 */
@Composable
public fun WatchCompactGauge(
    fraction: Float,
    valueText: String,
    label: String,
    color: Color,
    size: Dp = 56.dp,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.caption2)
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = fraction.coerceIn(0f, 1f),
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 5.dp,
                indicatorColor = color,
            )
            Text(valueText, style = MaterialTheme.typography.caption1)
        }
    }
}
