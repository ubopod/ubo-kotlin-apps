package com.ubopod.uboapp.wear.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Watch counterpart of the phone-app's haptic helper. Wear hardware
 * doesn't differentiate the way iOS UIImpactFeedbackGenerator does, so
 * we expose two strengths that map to Compose's two haptic primitives.
 */
public enum class WatchHapticStrength { LIGHT, MEDIUM }

@Composable
public fun rememberWatchHaptic(): (WatchHapticStrength) -> Unit {
    val haptic: HapticFeedback = LocalHapticFeedback.current
    return remember(haptic) {
        { strength: WatchHapticStrength ->
            haptic.performHapticFeedback(
                when (strength) {
                    WatchHapticStrength.LIGHT -> HapticFeedbackType.TextHandleMove
                    WatchHapticStrength.MEDIUM -> HapticFeedbackType.LongPress
                },
            )
        }
    }
}
