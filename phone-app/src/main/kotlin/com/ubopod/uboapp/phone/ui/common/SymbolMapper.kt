package com.ubopod.uboapp.phone.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps semantic icon keys (`"wifi"`, `"settings"`, …) to Material Icons.
 *
 * Mirrors `SymbolMapper.systemName(for:)` in
 * `ubo-swift-app/ubo-swift-app/Views/Device/RenderDeviceView.swift`. Used
 * as the fallback path when an icon string is *not* a Nerd-Font glyph
 * (cf. [isUboNerdGlyph]).
 *
 * Unrecognised keys collapse to a neutral [RadioButtonUnchecked] dot so
 * a missing mapping is visible rather than blank.
 */
public object SymbolMapper {
    public fun imageVector(icon: String): ImageVector = when (icon.lowercase()) {
        "info" -> Icons.Filled.Info
        "warning", "alert" -> Icons.Filled.Warning
        "error", "fail", "failure" -> Icons.Filled.Block
        "success", "ok", "check", "checkmark" -> Icons.Filled.CheckCircle
        "wifi" -> Icons.Filled.Wifi
        "ssh" -> Icons.Filled.Terminal
        "vpn" -> Icons.Filled.Lock
        "docker" -> Icons.Filled.Inventory2
        "settings", "gear" -> Icons.Filled.Settings
        "power" -> Icons.Filled.PowerSettingsNew
        else -> Icons.Filled.RadioButtonUnchecked
    }
}
