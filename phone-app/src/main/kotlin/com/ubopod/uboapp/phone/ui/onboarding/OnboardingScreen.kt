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
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ubopod.uboapp.phone.viewmodel.DeviceViewModel
import kotlinx.coroutines.launch

/**
 * Five-page welcome carousel mirroring
 * `ubo-swift-app/ubo-swift-app/Views/Onboarding/OnboardingView.swift`.
 * On the last page the "Get Started" button persists the
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
            Text(if (isLast) "Get Started" else "Next")
        }
    }
}

@Composable
private fun OnboardingPageView(page: OnboardingPage) {
    val uriHandler = LocalUriHandler.current
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
        if (page.links.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            page.links.forEach { link ->
                OutlinedButton(
                    onClick = { uriHandler.openUri(link.url) },
                    modifier = Modifier.fillMaxWidth(0.8f),
                ) {
                    Text(link.label)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private data class OnboardingLink(
    val label: String,
    val url: String,
)

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val links: List<OnboardingLink> = emptyList(),
)

private val OnboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Filled.Wifi,
        title = "Welcome to Ubo",
        body = "Your Ubo Pod, made mobile. Connect to monitor, control, and interact with your device from anywhere on the network or remotely.",
    ),
    OnboardingPage(
        icon = Icons.Filled.Speed,
        title = "See it at a glance",
        body = "Track full application and system stats (CPU, Memory, Storage, etc), and sensor readings in real time.",
    ),
    OnboardingPage(
        icon = Icons.Filled.PhoneAndroid,
        title = "Your Pod's screen, on your phone",
        body = "Browse menus, respond to prompts, and chat with the on-device assistant — all mirrored live from your Ubo.",
    ),
    OnboardingPage(
        icon = Icons.Filled.QrCode,
        title = "WiFi onboarding made easy",
        body = "Create a WiFi QR code to pass credentials to your Ubo Pod in a single step.",
    ),
    OnboardingPage(
        icon = Icons.Filled.ShoppingCart,
        title = "Don't have a Ubo yet?",
        body = "Get a ready-to-go UboPod, or deploy the software only version yourself on a Raspberry Pi.",
        links = listOf(
            OnboardingLink("Order a UboPod", "https://shop.getubo.com/products/ubo-pro-4-and-5"),
            OnboardingLink("Set up on Raspberry Pi", "https://github.com/ubopod/ubo_app/releases"),
        ),
    ),
)

