package com.ubopod.uboapp.phone.ui.dashboard.tiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ubopod.ubokotlin.models.WeatherCondition
import com.ubopod.uboapp.phone.ui.dashboard.DashboardCard
import com.ubopod.uboapp.phone.ui.dashboard.WeatherIcon
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

/**
 * Weather + date + time merged into one card. Previously three separate
 * grid tiles — but `LazyVerticalGrid` locks every row's height to its
 * tallest cell, so a short tile next to a tall one left a large empty
 * gap beneath the short one. Merging into a single card removes the
 * row-height mismatch entirely.
 */
@Composable
public fun TodaySection(
    weather: WeatherCondition?,
    locationCity: String?,
    locationCountry: String?,
    date: String,
    clock: String,
) {
    DashboardCard(title = "Today", icon = Icons.Filled.CalendarMonth) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                WeatherBlock(weather, locationCity, locationCountry)
            }

            VerticalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val formattedDate = remember(date) {
                    runCatching { LocalDate.parse(date) }.getOrNull()?.format(DATE_FORMATTER)
                }
                if (formattedDate != null) {
                    Text(formattedDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (clock.isNotEmpty()) {
                    Text(
                        clock,
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherBlock(weather: WeatherCondition?, locationCity: String?, locationCountry: String?) {
    if (weather != null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(imageVector = WeatherIcon.icon(weather.symbolCode), contentDescription = null)
            Text(
                "${weather.temperatureDisplayValue.roundToInt()}${weather.temperatureDisplayUnit}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(WeatherIcon.phrase(weather.symbolCode), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val place = listOfNotNull(
            locationCity?.takeIf { it.isNotEmpty() },
            locationCountry?.takeIf { it.isNotEmpty() },
        ).joinToString(", ").takeIf { it.isNotEmpty() }
        if (place != null) {
            Text(
                place,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    } else {
        Text(
            if (locationCity == null) "Location not detected yet" else "Fetching forecast…",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
