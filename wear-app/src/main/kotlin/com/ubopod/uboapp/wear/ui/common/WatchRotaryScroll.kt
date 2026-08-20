package com.ubopod.uboapp.wear.ui.common

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable

/**
 * Wires a rotating-crown/bezel input to a [ScalingLazyListState]-backed
 * list — the watch equivalent of `.digitalCrownRotation` on watchOS
 * (see `WatchVolumeView.swift`). Requests focus once on entry, since
 * rotary events only route to a focused element.
 */
@Composable
public fun Modifier.rotaryScroll(state: ScalingLazyListState): Modifier {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    return this
        .focusRequester(focusRequester)
        .focusable()
        .rotaryScrollable(
            behavior = RotaryScrollableDefaults.snapBehavior(state),
            focusRequester = focusRequester,
        )
}

/**
 * Same as [rotaryScroll] but for a plain [ScrollableState] (e.g. the
 * `rememberScrollState()` used by the Dashboard's Column-based pages,
 * which aren't `ScalingLazyColumn`s).
 */
@Composable
public fun Modifier.rotaryScroll(state: ScrollableState): Modifier {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    return this
        .focusRequester(focusRequester)
        .focusable()
        .rotaryScrollable(
            behavior = RotaryScrollableDefaults.behavior(state),
            focusRequester = focusRequester,
        )
}
