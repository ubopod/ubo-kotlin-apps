package com.ubopod.uboapp.wear.ui.common

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable

/**
 * Whether the top-level tab `VerticalPager` (see `WatchContentScreen.kt`)
 * is currently mid-swipe. `rotaryScroll` reads this to stay inert while
 * true — see the note on [rotaryScroll] for why. Defaults to `false`
 * (rotary active) so anything composed outside `ConnectedShell` behaves as
 * before.
 */
public val LocalOuterPagerScrolling: androidx.compose.runtime.ProvidableCompositionLocal<Boolean> =
    compositionLocalOf { false }

/**
 * Wires a rotating-crown/bezel input to a [ScalingLazyListState]-backed
 * list — the watch equivalent of `.digitalCrownRotation` on watchOS
 * (see `WatchVolumeView.swift`). Requests focus once on entry, since
 * rotary events only route to a focused element.
 *
 * A no-op while [LocalOuterPagerScrolling] is true: `rotaryScrollable`'s
 * focus/gesture wiring, attached to a page's own list, was found to
 * interfere with the *outer* tab `VerticalPager`'s swipe-to-change-tab
 * gesture even for plain touch input (confirmed by reproducing a 3-tab
 * swipe reliably skipping the middle tab, then reliably not skipping it
 * once this modifier was removed from the destination page). Only
 * attaching it once the tab switch has actually settled keeps the crown
 * usable for scrolling a settled page's content without it fighting the
 * gesture that gets you there.
 */
@Composable
public fun Modifier.rotaryScroll(state: ScalingLazyListState): Modifier {
    if (LocalOuterPagerScrolling.current) return this
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
    if (LocalOuterPagerScrolling.current) return this
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
