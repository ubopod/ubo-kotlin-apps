package com.ubopod.uboapp.wear.ui.connection

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.ubopod.uboapp.wear.ui.common.WatchTextField
import com.ubopod.uboapp.wear.ui.common.generateQrCodeBitmap
import com.ubopod.uboapp.wear.ui.common.rotaryScroll

/**
 * Builds a standard `WIFI:` QR payload from user-entered credentials so a
 * brand-new Ubo Pod (not on any network yet) can join Wi-Fi by scanning the
 * watch's screen with its camera. Mirrors the phone app's
 * `WifiQrCodePayload`/`WifiSecurityType` — no SSID autofill here, since that
 * path (`ConnectivityManager`/`WifiInfo`) isn't meaningful on a watch that
 * isn't itself on the target Wi-Fi network; SSID/password are typed
 * manually, same input method the connect form's Host field already uses.
 */
public object WatchWifiQrCodePayload {
    public fun build(ssid: String, password: String, security: WatchWifiSecurityType): String {
        val builder = StringBuilder("WIFI:T:${security.qrCodeValue};S:${escape(ssid)};")
        if (security != WatchWifiSecurityType.NONE) {
            builder.append("P:${escape(password)};")
        }
        builder.append(";")
        return builder.toString()
    }

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

public enum class WatchWifiSecurityType(public val label: String, public val qrCodeValue: String) {
    WPA("WPA/WPA2", "WPA"),
    WEP("WEP", "WEP"),
    NONE("None (Open)", "nopass"),
}

@Composable
public fun WatchWifiQrCodeScreen() {
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var security by remember { mutableStateOf(WatchWifiSecurityType.WPA) }
    var qrPayload by remember { mutableStateOf<String?>(null) }
    val qrImage = qrPayload?.let { payload -> remember(payload) { generateQrCodeBitmap(payload) } }
    val listState = rememberScalingLazyListState()

    Scaffold(positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).rotaryScroll(listState),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text("Wi-Fi QR Code", style = MaterialTheme.typography.title3, textAlign = TextAlign.Center)
            }
            item {
                WifiField(label = "SSID", value = ssid, onChange = { ssid = it; qrPayload = null })
            }
            item {
                Chip(
                    onClick = {
                        security = when (security) {
                            WatchWifiSecurityType.WPA -> WatchWifiSecurityType.WEP
                            WatchWifiSecurityType.WEP -> WatchWifiSecurityType.NONE
                            WatchWifiSecurityType.NONE -> WatchWifiSecurityType.WPA
                        }
                        qrPayload = null
                    },
                    label = { Text("Security: ${security.label}") },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (security != WatchWifiSecurityType.NONE) {
                item {
                    WifiField(label = "Password", value = password, onChange = { password = it; qrPayload = null }, isPassword = true)
                }
            }
            item {
                Chip(
                    onClick = { qrPayload = WatchWifiQrCodePayload.build(ssid, password, security) },
                    label = { Text("Generate QR Code") },
                    colors = ChipDefaults.primaryChipColors(),
                    enabled = ssid.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (qrImage != null) {
                item {
                    Image(
                        bitmap = qrImage,
                        contentDescription = "Wi-Fi QR code",
                        filterQuality = FilterQuality.None,
                        modifier = Modifier.fillMaxWidth(0.6f).aspectRatio(1f).background(Color.White).padding(2.dp),
                    )
                }
                item {
                    Text(
                        "Hold this up to the Ubo Pod's camera",
                        style = MaterialTheme.typography.caption2,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun WifiField(label: String, value: String, onChange: (String) -> Unit, isPassword: Boolean = false) {
    WatchTextField(
        label = label,
        value = value,
        onValueChange = onChange,
        placeholder = if (isPassword) "Password" else "Network name",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        visualTransformation = if (isPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
    )
}
