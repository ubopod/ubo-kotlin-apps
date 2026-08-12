package com.ubopod.uboapp.wear.ui.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps MET Norway weather symbol codes to Material icons and a short
 * phrase, condensed for a watch face. Wear-local counterpart to the phone
 * app's `WeatherIcon.kt`.
 */
public object WatchWeatherIcon {
    private val phrases: Map<String, String> = mapOf(
        "clearsky" to "Clear", "fair" to "Fair", "partlycloudy" to "Partly cloudy",
        "cloudy" to "Cloudy", "fog" to "Fog", "rain" to "Rain", "lightrain" to "Light rain",
        "heavyrain" to "Heavy rain", "rainshowers" to "Showers", "drizzle" to "Drizzle",
        "sleet" to "Sleet", "snow" to "Snow", "lightsnow" to "Light snow",
        "heavysnow" to "Heavy snow", "snowshowers" to "Snow showers",
        "rainandthunder" to "Thunder", "thunderstorm" to "Thunderstorm",
    )

    private fun base(symbolCode: String): Pair<String, Boolean> {
        for (suffix in listOf("_day", "_night", "_polartwilight")) {
            if (symbolCode.endsWith(suffix)) {
                return symbolCode.removeSuffix(suffix) to (suffix == "_night")
            }
        }
        return symbolCode to false
    }

    public fun phrase(symbolCode: String): String {
        val (key, _) = base(symbolCode)
        return phrases[key] ?: key.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    public fun icon(symbolCode: String): ImageVector {
        val (key, isNight) = base(symbolCode)
        return when (key) {
            "clearsky", "fair" -> if (isNight) Icons.Outlined.NightsStay else Icons.Filled.WbSunny
            "partlycloudy" -> Icons.Filled.WbCloudy
            "cloudy", "fog" -> Icons.Filled.Cloud
            "rain", "lightrain", "heavyrain", "drizzle", "rainshowers" -> Icons.Filled.WaterDrop
            "sleet" -> Icons.Filled.Grain
            "snow", "lightsnow", "heavysnow", "snowshowers" -> Icons.Filled.AcUnit
            "rainandthunder", "thunderstorm" -> Icons.Filled.Thunderstorm
            else -> Icons.Filled.HelpOutline
        }
    }
}
