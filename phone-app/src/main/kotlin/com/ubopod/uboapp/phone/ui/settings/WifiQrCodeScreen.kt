package com.ubopod.uboapp.phone.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ubopod.uboapp.phone.ui.common.generateQrCodeBitmap

/**
 * Builds a standard `WIFI:` QR payload from user-entered credentials so a
 * Ubo Pod can join a network by scanning the phone's screen — no typing
 * required on the device itself.
 *
 * Mirrors `WiFiQRCodePayload` in
 * `ubo-swift-app/ubo-swift-app/Views/Settings/WiFiQRCodeGeneratorView.swift`.
 */
public object WifiQrCodePayload {
    public fun build(ssid: String, password: String, security: WifiSecurityType): String {
        val builder = StringBuilder("WIFI:T:${security.qrCodeValue};S:${escape(ssid)};")
        if (security != WifiSecurityType.NONE) {
            builder.append("P:${escape(password)};")
        }
        builder.append(";")
        return builder.toString()
    }

    /** Escapes the characters the spec reserves as field/payload delimiters. */
    private fun escape(value: String): String {
        val builder = StringBuilder()
        for (character in value) {
            if (character in RESERVED_CHARACTERS) builder.append('\\')
            builder.append(character)
        }
        return builder.toString()
    }

    private val RESERVED_CHARACTERS = charArrayOf('\\', ';', ',', '"', ':')
}

public enum class WifiSecurityType(public val label: String, public val qrCodeValue: String) {
    WPA("WPA/WPA2", "WPA"),
    WEP("WEP", "WEP"),
    NONE("None (Open)", "nopass"),
}

/**
 * Screen for entering Wi-Fi credentials and generating a scannable QR code.
 * Prefills the SSID from the phone's current network (requires
 * `ACCESS_FINE_LOCATION` — without it, `WifiManager.connectionInfo` returns
 * a placeholder instead of the real SSID).
 *
 * Mirrors `WiFiQRCodeGeneratorView` in the Swift port.
 */
@Composable
public fun WifiQrCodeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var security by remember { mutableStateOf(WifiSecurityType.WPA) }
    var securityMenuOpen by remember { mutableStateOf(false) }
    var qrImage by remember { mutableStateOf<ImageBitmap?>(null) }

    val ssidPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && ssid.isEmpty()) {
            fetchCurrentSsid(context)?.let { ssid = it }
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            fetchCurrentSsid(context)?.let { ssid = it }
        } else {
            ssidPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Wi-Fi QR Code",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        OutlinedTextField(
            value = ssid,
            onValueChange = { ssid = it; qrImage = null },
            label = { Text("SSID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Security", modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { securityMenuOpen = true }) {
                Text(security.label)
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = securityMenuOpen, onDismissRequest = { securityMenuOpen = false }) {
                WifiSecurityType.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            security = option
                            securityMenuOpen = false
                            qrImage = null
                        },
                    )
                }
            }
        }

        if (security != WifiSecurityType.NONE) {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; qrImage = null },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Text(
            "The SSID is prefilled from the network you're currently connected to. Clear it to enter a different one.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = {
                qrImage = generateQrCodeBitmap(
                    WifiQrCodePayload.build(ssid, password, security),
                    sizePx = 512,
                )
            },
            enabled = ssid.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Generate QR Code") }

        qrImage?.let { image ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = image,
                        contentDescription = "Wi-Fi QR code",
                        filterQuality = FilterQuality.None,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    )
                }
                Text(
                    "Hold this up to the Ubo Pod's camera",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Reads the SSID of the currently active Wi-Fi network via
 * [NetworkCapabilities.getTransportInfo] rather than the deprecated
 * `WifiManager.connectionInfo`. Still requires `ACCESS_FINE_LOCATION` —
 * without it, [WifiInfo.getSSID] returns [WifiManager.UNKNOWN_SSID] instead
 * of the real network name.
 */
private fun fetchCurrentSsid(context: Context): String? {
    val connectivityManager = context.applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return null
    val network = connectivityManager.activeNetwork ?: return null
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return null
    if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null
    val wifiInfo = capabilities.transportInfo as? WifiInfo ?: return null
    val cleaned = wifiInfo.ssid.removeSurrounding("\"")
    return cleaned.takeUnless { it.isEmpty() || it == WifiManager.UNKNOWN_SSID }
}
