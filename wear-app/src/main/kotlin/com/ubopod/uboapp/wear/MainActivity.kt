package com.ubopod.uboapp.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.MaterialTheme
import com.ubopod.uboapp.wear.ui.WatchContentScreen
import com.ubopod.uboapp.wear.viewmodel.DeviceViewModel

public class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UboWearTheme {
                val viewModel: DeviceViewModel = viewModel(factory = DeviceViewModel.Factory)
                WatchContentScreen(viewModel)
            }
        }
    }
}

/**
 * Wear Compose Material 2 theme wrapper. Wear OS doesn't have dynamic
 * color (yet) so the theme is a hand-picked dark palette — matches the
 * always-on-display friendly style the Swift Watch app uses.
 */
@Composable
public fun UboWearTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            content()
        }
    }
}
