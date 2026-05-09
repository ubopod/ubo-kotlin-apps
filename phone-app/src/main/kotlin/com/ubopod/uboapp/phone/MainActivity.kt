package com.ubopod.uboapp.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ubopod.uboapp.phone.ui.ContentScreen
import com.ubopod.uboapp.phone.viewmodel.DeviceViewModel

public class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UboTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val viewModel: DeviceViewModel = viewModel(factory = DeviceViewModel.Factory)
                    // Hand CameraX a LifecycleOwner so its use-cases bind
                    // to a real lifecycle (this Activity).
                    LaunchedEffect(viewModel) {
                        viewModel.bindHardwareServices(this@MainActivity)
                    }
                    ContentScreen(viewModel)
                }
            }
        }
    }
}

/**
 * Material 3 theme wrapper. Uses dynamic color on Android 12+ (matches the
 * `Material You` look the rest of the system uses); falls back to a hand-
 * picked dark palette otherwise.
 */
@Composable
public fun UboTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) {
        runCatching { dynamicDarkColorScheme(context) }.getOrElse { darkColorScheme() }
    } else {
        runCatching { dynamicLightColorScheme(context) }.getOrElse { lightColorScheme() }
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
