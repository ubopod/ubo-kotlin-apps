package com.ubopod.uboapp.phone.ui.dashboard.tiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ubopod.ubokotlin.models.DockerAppStatus
import com.ubopod.ubokotlin.models.DockerItemHealth
import com.ubopod.ubokotlin.models.DockerItemStatus
import com.ubopod.uboapp.phone.ui.dashboard.DashboardCard
import com.ubopod.uboapp.phone.ui.dashboard.DashboardColors

/**
 * One row per installed Docker app, mirroring the Web UI's `AppsCard`.
 * Health outranks lifecycle status — the same precedence
 * `_update_app_badge` applies on the device
 * (`ubo_app/services/080-docker/menus.py`) — so a crash-looping app under
 * `restart_policy: always` still shows red even while it cycles back to
 * RUNNING every few seconds.
 */
private data class AppPresentation(val icon: ImageVector, val color: Color, val text: String)

private fun present(app: DockerAppStatus): AppPresentation = when (app.health) {
    DockerItemHealth.CRASH_LOOPING -> AppPresentation(Icons.Filled.Warning, DashboardColors.critical, "Crash looping")
    DockerItemHealth.RECOVERED -> AppPresentation(Icons.Filled.Warning, DashboardColors.warning, "Restarted")
    DockerItemHealth.OK, DockerItemHealth.UNSPECIFIED -> when (app.status) {
        DockerItemStatus.RUNNING -> AppPresentation(Icons.Filled.Circle, DashboardColors.good, "Running")
        DockerItemStatus.STARTING -> AppPresentation(Icons.Filled.Circle, DashboardColors.warning, "Starting")
        DockerItemStatus.FETCHING -> AppPresentation(Icons.Filled.Circle, DashboardColors.warning, "Fetching")
        DockerItemStatus.PROCESSING -> AppPresentation(Icons.Filled.Circle, DashboardColors.warning, "Working")
        DockerItemStatus.ERROR -> AppPresentation(Icons.Filled.Warning, DashboardColors.critical, "Errored")
        DockerItemStatus.AVAILABLE, DockerItemStatus.CREATED ->
            AppPresentation(Icons.Filled.Circle, DashboardColors.idle, "Stopped")
        DockerItemStatus.NOT_AVAILABLE, DockerItemStatus.UNSPECIFIED ->
            AppPresentation(Icons.Filled.HelpOutline, DashboardColors.idle, "Unknown")
    }
}

@Composable
public fun AppsTile(apps: List<DockerAppStatus>, modifier: Modifier = Modifier) {
    val sorted = apps.sortedBy { it.label.ifEmpty { it.id } }
    DashboardCard(title = "Apps", icon = Icons.Filled.Apps, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (app in sorted) {
                val presentation = present(app)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = presentation.icon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = presentation.color,
                    )
                    Text(
                        app.label.ifEmpty { app.id },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        presentation.text,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = presentation.color,
                    )
                }
            }
        }
    }
}
