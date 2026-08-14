package com.ubopod.uboapp.wear.ui.dashboard.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ubopod.ubokotlin.models.SystemStats
import com.ubopod.uboapp.wear.ui.dashboard.WatchWeatherIcon
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

/** Page 2 of the Wear Dashboard: weather, date, clock. */
@Composable
public fun WatchWeatherDateTimePage(stats: SystemStats) {
    val place = listOfNotNull(stats.locationCity?.ifEmpty { null }, stats.locationCountry?.ifEmpty { null })
        .joinToString(", ")
        .ifEmpty { null }
    val formattedDate = runCatching { LocalDate.parse(stats.date) }.getOrNull()?.format(DATE_FORMATTER)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val weather = stats.weather
        if (weather != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(WatchWeatherIcon.icon(weather.symbolCode), contentDescription = null, modifier = Modifier)
                Text(
                    "${weather.temperatureDisplayValue.toInt()}${weather.temperatureDisplayUnit}",
                    style = MaterialTheme.typography.title3,
                )
            }
            Text(WatchWeatherIcon.phrase(weather.symbolCode), style = MaterialTheme.typography.caption2)
            if (place != null) {
                Text(place, style = MaterialTheme.typography.caption2, maxLines = 1)
            }
        } else {
            Text("Fetching forecast…", style = MaterialTheme.typography.caption2)
        }

        if (formattedDate != null) {
            Text(formattedDate, style = MaterialTheme.typography.caption1)
        }

        if (stats.clock.isNotEmpty()) {
            Text(stats.clock, style = MaterialTheme.typography.title2)
        }
    }
}
