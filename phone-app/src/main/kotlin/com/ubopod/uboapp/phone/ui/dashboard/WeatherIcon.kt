package com.ubopod.uboapp.phone.ui.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps MET Norway weather symbol codes (as streamed in
 * `LocalizationState.weather.symbol_code`, e.g. "partlycloudy_day") to
 * Material icons and a human-readable phrase. The Web UI hand-draws these
 * as SVG (`WeatherIcon.tsx`); Material Symbols already cover this
 * vocabulary, so we map instead of porting the SVG path data.
 */
public object WeatherIcon {
    /**
     * Display phrases for the MET Norway symbol codes, keyed by the code
     * with its day/night suffix stripped. Mirrors `SYMBOL_PHRASES` in
     * `ubo_app/services/010-localization/weather.py` and the Web UI's
     * `WeatherCard.tsx` `PHRASES` table.
     */
    private val phrases: Map<String, String> = mapOf(
        "clearsky" to "Clear",
        "fair" to "Fair",
        "partlycloudy" to "Partly cloudy",
        "cloudy" to "Cloudy",
        "fog" to "Fog",
        "rain" to "Rain",
        "lightrain" to "Light rain",
        "heavyrain" to "Heavy rain",
        "rainshowers" to "Showers",
        "lightrainshowers" to "Light showers",
        "heavyrainshowers" to "Heavy showers",
        "drizzle" to "Drizzle",
        "sleet" to "Sleet",
        "lightsleet" to "Light sleet",
        "heavysleet" to "Heavy sleet",
        "sleetshowers" to "Sleet showers",
        "snow" to "Snow",
        "lightsnow" to "Light snow",
        "heavysnow" to "Heavy snow",
        "snowshowers" to "Snow showers",
        "rainandthunder" to "Rain and thunder",
        "rainshowersandthunder" to "Showers and thunder",
        "thunderstorm" to "Thunderstorm",
        "heavyrainandthunder" to "Heavy rain and thunder",
        "snowandthunder" to "Snow and thunder",
        "sleetandthunder" to "Sleet and thunder",
    )

    private fun base(symbolCode: String): Pair<String, Boolean> {
        for (suffix in listOf("_day", "_night", "_polartwilight")) {
            if (symbolCode.endsWith(suffix)) {
                return symbolCode.removeSuffix(suffix) to (suffix == "_night")
            }
        }
        return symbolCode to false
    }

    /** Human-readable description, e.g. "Partly cloudy". */
    public fun phrase(symbolCode: String): String {
        val (key, _) = base(symbolCode)
        return phrases[key] ?: key.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    public fun icon(symbolCode: String): ImageVector {
        val (key, isNight) = base(symbolCode)
        return when (key) {
            "clearsky" -> if (isNight) Icons.Outlined.NightsStay else Icons.Filled.WbSunny
            "fair" -> if (isNight) Icons.Outlined.NightsStay else Icons.Filled.WbSunny
            "partlycloudy" -> Icons.Filled.WbCloudy
            "cloudy" -> Icons.Filled.Cloud
            "fog" -> Icons.Filled.Cloud
            "rain", "lightrain", "heavyrain", "drizzle",
            "rainshowers", "lightrainshowers", "heavyrainshowers",
            -> Icons.Filled.WaterDrop
            "sleet", "lightsleet", "heavysleet", "sleetshowers" -> Icons.Filled.Grain
            "snow", "lightsnow", "heavysnow", "snowshowers" -> Icons.Filled.AcUnit
            "rainandthunder", "rainshowersandthunder", "thunderstorm",
            "heavyrainandthunder", "snowandthunder", "sleetandthunder",
            -> Icons.Filled.Thunderstorm
            else -> Icons.Filled.HelpOutline
        }
    }
}
