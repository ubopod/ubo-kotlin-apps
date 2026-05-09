package com.ubopod.uboapp.phone.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ubopod.uboapp.phone.viewmodel.DeviceViewModel
import kotlinx.coroutines.launch

/**
 * Four-page welcome carousel mirroring
 * `ubo-swift-app/ubo-swift-app/Views/Onboarding/OnboardingView.swift`.
 * On the last page the "Get started" button persists the
 * `has_completed_onboarding` flag so subsequent launches skip the carousel.
 */
@Composable
public fun OnboardingScreen(viewModel: DeviceViewModel) {
    val pagerState = rememberPagerState(pageCount = { OnboardingPages.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(0.dp),
        ) { index ->
            OnboardingPageView(OnboardingPages[index])
        }

        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(OnboardingPages.size) { index ->
                val active = pagerState.currentPage == index
                Surface(
                    modifier = Modifier.size(if (active) 10.dp else 8.dp),
                    shape = CircleShape,
                    color = if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                    content = {},
                )
            }
        }

        val isLast = pagerState.currentPage == OnboardingPages.lastIndex
        Button(
            onClick = {
                if (isLast) {
                    coroutineScope.launch { viewModel.markOnboardingComplete() }
                } else {
                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isLast) "Get started" else "Continue")
        }
    }
}

@Composable
private fun OnboardingPageView(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            page.icon,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(32.dp))
        Text(
            page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            page.body,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val body: String,
)

private val OnboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Filled.Notifications,
        title = "Welcome to Ubo",
        body = "Control your Ubo device from your Android phone. Send notifications, dispatch actions, and watch the screen mirror in real time.",
    ),
    OnboardingPage(
        icon = Icons.Filled.Wifi,
        title = "Stay on the same network",
        body = "Make sure your phone and Ubo device are on the same Wi-Fi network. The device's gRPC server listens on port 50051 by default.",
    ),
    OnboardingPage(
        icon = Icons.Filled.SettingsRemote,
        title = "A remote in your pocket",
        body = "Drive the menu with on-screen D-Pad / L1-L3 buttons, or trigger actions directly from quick-action shortcuts.",
    ),
    OnboardingPage(
        icon = Icons.Filled.LinkOff,
        title = "Reconnects automatically",
        body = "If the connection drops we'll back off and retry. You can adjust the policy in code (ReconnectPolicy.Default).",
    ),
)

