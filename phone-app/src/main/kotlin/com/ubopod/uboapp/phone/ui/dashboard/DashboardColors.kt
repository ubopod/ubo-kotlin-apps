package com.ubopod.uboapp.phone.ui.dashboard

import androidx.compose.ui.graphics.Color

/**
 * Status palette — fixed, never themed, and deliberately distinct from any
 * series color so a status hue never impersonates a category. Mirrors the
 * Web UI's `colors.ts`.
 */
public object DashboardColors {
    public val good: Color = Color(0xFF0C_A3_0C)
    public val warning: Color = Color(0xFF_FA_B2_19)
    public val serious: Color = Color(0xFF_EC_83_5A)
    public val critical: Color = Color(0xFF_D0_3B_3B)

    /** The neutral accent for ratios that carry no severity meaning. */
    public val neutralAccent: Color = Color(0xFF2A_78_D6)

    /**
     * Sensor-gauge ring color — yellow reads with much higher contrast
     * than [neutralAccent]'s blue against the dashboard's dark cards.
     */
    public val gaugeAccent: Color = Color(0xFFFF_C1_07)

    /**
     * An app that's installed but not running — deliberately outside the
     * severity scale, a fixed hue so a stopped app reads the same in both
     * themes.
     */
    public val idle: Color = Color(0xFF8A_8F_98)

    /**
     * Pick a meter fill for a load-style percentage, where more is worse.
     * Applies to CPU, RAM and disk — sensor readings don't use this, since
     * e.g. high humidity is not a fault.
     */
    public fun loadSeverity(percent: Float): Color = when {
        percent >= 90f -> critical
        percent >= 80f -> serious
        percent >= 60f -> warning
        else -> good
    }
}
