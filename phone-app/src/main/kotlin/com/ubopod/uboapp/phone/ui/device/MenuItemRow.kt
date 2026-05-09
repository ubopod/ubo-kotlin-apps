package com.ubopod.uboapp.phone.ui.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ubopod.uboapp.phone.ui.common.IconView
import com.ubopod.uboapp.phone.ui.common.markupAnnotated
import com.ubopod.uboapp.phone.ui.common.uboIconColor
import com.ubopod.ubokotlin.models.MenuItemData

/**
 * Shared row layout used by both [HomeViewRenderer] and
 * [MenuViewRenderer]. Renders the item's Nerd-Font / Material icon on
 * the left, BBCode-aware label in the middle, and a chevron on the
 * right. Tapping fires [onTap].
 *
 * Mirrors the Swift port's `MenuItemRow`, which is similarly shared
 * across the home and menu views.
 */
@Composable
internal fun MenuItemRow(item: MenuItemData, onTap: () -> Unit) {
    Card(
        onClick = onTap,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val accent = uboIconColor(item.color, fallback = MaterialTheme.colorScheme.onSurface)
            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                IconView(icon = item.icon, size = 24.dp, tint = accent)
            }
            Text(
                markupAnnotated(item.label.ifEmpty { item.key }),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
