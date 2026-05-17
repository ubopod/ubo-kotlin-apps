package com.ubopod.uboapp.phone.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Thin wrapper over Compose's [LocalHapticFeedback] tuned for the
 * controls that mirror the device's physical keypad. Phone-side button
 * taps get a [HapticFeedbackType.LongPress] (heavier than `TextHandleMove`,
 * easier to notice through a case) and incidental taps (menu rows, top
 * bar) get the lighter [HapticFeedbackType.TextHandleMove].
 *
 * Mirrors the Swift `triggerHaptic()` in
 * `Views/Controls/RemoteControlView.swift` /
 * `Views/Device/DeviceView.swift`.
 */
public enum class HapticStrength { LIGHT, MEDIUM }

@Composable
public fun rememberHaptic(): (HapticStrength) -> Unit {
    val haptic: HapticFeedback = LocalHapticFeedback.current
    return remember(haptic) {
        { strength: HapticStrength ->
            haptic.performHapticFeedback(
                when (strength) {
                    HapticStrength.LIGHT -> HapticFeedbackType.TextHandleMove
                    HapticStrength.MEDIUM -> HapticFeedbackType.LongPress
                },
            )
        }
    }
}
