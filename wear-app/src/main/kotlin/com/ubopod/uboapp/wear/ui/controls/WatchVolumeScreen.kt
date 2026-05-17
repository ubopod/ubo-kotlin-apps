package com.ubopod.uboapp.wear.ui.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Stepper
import androidx.wear.compose.material.Text
import com.ubopod.uboapp.wear.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.models.AudioDevice
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Volume control. [Stepper] is the wear-native widget for this kind of
 * incrementing value — it picks up rotary crown input automatically and
 * pairs the value with +/- buttons for touch users. Releasing the
 * crown / pressing a step button dispatches `setVolume`. The slider
 * tracks `systemStats.playbackVolume` live whenever the user isn't
 * editing (commit `8610b0a`).
 */
@Composable
public fun WatchVolumeScreen(viewModel: DeviceViewModel) {
    val stats by viewModel.systemStats.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var slider by remember { mutableFloatStateOf(stats?.playbackVolume ?: 0f) }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(stats?.playbackVolume, isEditing) {
        if (!isEditing) stats?.playbackVolume?.let { slider = it }
    }

    val muted = stats?.isPlaybackMute == true
    val icon = when {
        muted || slider <= 0f -> Icons.Filled.VolumeOff
        slider < 0.5f -> Icons.Filled.VolumeDown
        else -> Icons.Filled.VolumeUp
    }

    val commit: (Float) -> Unit = { target ->
        isEditing = false
        scope.launch { runCatching { viewModel.client.setVolume(target, AudioDevice.OUTPUT) } }
    }

    Stepper(
        value = slider,
        onValueChange = {
            isEditing = true
            slider = it
            commit(it)
        },
        valueRange = 0f..1f,
        steps = 19,
        decreaseIcon = { Icon(Icons.Filled.VolumeDown, contentDescription = "Quieter") },
        increaseIcon = { Icon(Icons.Filled.VolumeUp, contentDescription = "Louder") },
        modifier = Modifier.fillMaxSize().padding(8.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null)
            Text(
                "${(slider * 100).roundToInt()}%",
                style = MaterialTheme.typography.title2,
            )
        }
    }
}
