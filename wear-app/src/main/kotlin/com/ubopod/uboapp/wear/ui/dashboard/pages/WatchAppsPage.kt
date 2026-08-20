package com.ubopod.uboapp.wear.ui.dashboard.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.ubopod.ubokotlin.models.DockerAppStatus
import com.ubopod.ubokotlin.models.DockerItemHealth
import com.ubopod.ubokotlin.models.DockerItemStatus
import com.ubopod.uboapp.wear.ui.common.rotaryScroll

/**
 * Page 3 of the Wear Dashboard: one compact row per installed Docker app,
 * same status/health precedence as the phone's AppsTile.
 */
private data class WatchAppPresentation(val icon: ImageVector, val color: Color, val text: String)

private fun present(app: DockerAppStatus): WatchAppPresentation = when (app.health) {
    DockerItemHealth.CRASH_LOOPING -> WatchAppPresentation(Icons.Filled.Warning, Color(0xFFD0_3B_3B), "Crash looping")
    DockerItemHealth.RECOVERED -> WatchAppPresentation(Icons.Filled.Warning, Color(0xFFFA_B2_19), "Restarted")
    DockerItemHealth.OK, DockerItemHealth.UNSPECIFIED -> when (app.status) {
        DockerItemStatus.RUNNING -> WatchAppPresentation(Icons.Filled.Circle, Color(0xFF0C_A3_0C), "Running")
        DockerItemStatus.STARTING -> WatchAppPresentation(Icons.Filled.Circle, Color(0xFFFA_B2_19), "Starting")
        DockerItemStatus.FETCHING -> WatchAppPresentation(Icons.Filled.Circle, Color(0xFFFA_B2_19), "Fetching")
        DockerItemStatus.PROCESSING -> WatchAppPresentation(Icons.Filled.Circle, Color(0xFFFA_B2_19), "Working")
        DockerItemStatus.ERROR -> WatchAppPresentation(Icons.Filled.Warning, Color(0xFFD0_3B_3B), "Errored")
        DockerItemStatus.AVAILABLE, DockerItemStatus.CREATED ->
            WatchAppPresentation(Icons.Filled.Circle, Color(0xFF8A_8F_98), "Stopped")
        DockerItemStatus.NOT_AVAILABLE, DockerItemStatus.UNSPECIFIED ->
            WatchAppPresentation(Icons.Filled.HelpOutline, Color(0xFF8A_8F_98), "Unknown")
    }
}

@Composable
public fun WatchAppsPage(apps: List<DockerAppStatus>) {
    val sorted = apps.sortedBy { it.label.ifEmpty { it.id } }
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).rotaryScroll(scrollState).padding(horizontal = 16.dp).padding(top = 30.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // A few extra dp on the title only — list rows fit fine at the
        // Column's base padding, but a full-width title runs closer to
        // the bezel at this row's height and needs a bit more clearance.
        Text("Apps", style = MaterialTheme.typography.title3, modifier = Modifier.padding(start = 6.dp))
        for (app in sorted) {
            val presentation = present(app)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(presentation.icon, contentDescription = null, tint = presentation.color, modifier = Modifier.size(12.dp))
                Text(
                    app.label.ifEmpty { app.id },
                    style = MaterialTheme.typography.caption2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    presentation.text,
                    style = MaterialTheme.typography.caption2,
                    color = presentation.color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
