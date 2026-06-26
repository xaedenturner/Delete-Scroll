package com.xaedenturner.deletescroll

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Palette derived from the app icon.
// Red is the active/blocking state colour — it's a success signal, not a warning.
private val Red    = Color(0xFFFA0500)
private val DarkBg = Color(0xFF181818)
private val CardBg = Color(0xFF242424)
private val SurfBg = Color(0xFF2E2E2E)
private val Gray   = Color(0xFF848484)
private val White  = Color(0xFFFFFFFF)
private val Border = Color(0xFF303030)
private val RedDim = Color(0x1FFA0500)
private val Green  = Color(0xFF1D9E75)

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force light (white) status bar icons so they're visible on the dark background.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            ReelsBlockerScreen(
                isServiceEnabled    = uiState.isServiceEnabled, 
                hasOverlayPermission = uiState.hasOverlayPermission,
                onEnableService     = { openAccessibilitySettings() },
                onGrantOverlay      = { openOverlaySettings() } // Overlay settings currently not used but might be used later on
            )
        }
    }

    // Re-check both flags every time the user comes back from a system settings screen.
    override fun onResume() {
        super.onResume()
        viewModel.refreshStatus()
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openOverlaySettings() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
        )
    }
}

// ─── Composables ─────────────────────────────────────────────────────────────

@Composable
private fun ReelsBlockerScreen(
    isServiceEnabled: Boolean,
    hasOverlayPermission: Boolean,
    onEnableService: () -> Unit,
    onGrantOverlay: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(28.dp))

        Text(
            text = "REELS BLOCKER",
            color = Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )

        Spacer(Modifier.height(16.dp))

        StatusCard(isServiceEnabled)

        Spacer(Modifier.height(10.dp))

        PrimaryButton(
            isServiceEnabled = isServiceEnabled,
            onClick = onEnableService
        )

        Spacer(Modifier.height(10.dp))

        OverlayPermissionRow(
            hasOverlayPermission = hasOverlayPermission,
            onClick = onGrantOverlay
        )
    }
}

@Composable
private fun StatusCard(isServiceEnabled: Boolean) {
    val cardBg by animateColorAsState(
        targetValue = if (isServiceEnabled) Red else CardBg,
        animationSpec = tween(durationMillis = 300),
        label = "card_bg"
    )
    val headlineColor by animateColorAsState(
        targetValue = if (isServiceEnabled) White else Gray,
        animationSpec = tween(300),
        label = "headline_color"
    )
    val labelColor by animateColorAsState(
        targetValue = if (isServiceEnabled) White.copy(alpha = 0.75f) else Gray,
        animationSpec = tween(300),
        label = "label_color"
    )
    val dotColor by animateColorAsState(
        targetValue = if (isServiceEnabled) White.copy(alpha = 0.85f) else Gray,
        animationSpec = tween(300),
        label = "dot_color"
    )

    // Blink animation always runs — alpha conditioned on service state so there's
    // no restart stutter when the state changes mid-animation.
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .padding(20.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .alpha(if (isServiceEnabled) blinkAlpha else 1f)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Text(
                    text = if (isServiceEnabled) "ACTIVE" else "INACTIVE",
                    color = labelColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.0.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = if (isServiceEnabled) "Reels are\nblocked." else "Reels are\ngetting through.",
                color = headlineColor,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 26.sp,
                letterSpacing = (-0.5).sp
            )
        }
    }
}

@Composable
private fun PrimaryButton(
    isServiceEnabled: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isServiceEnabled) SurfBg else Red,
        animationSpec = tween(300),
        label = "btn_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isServiceEnabled) Gray else White,
        animationSpec = tween(300),
        label = "btn_text"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp)
    ) {
        Text(
            text = if (isServiceEnabled) "Open Accessibility Settings" else "Enable in Settings",
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp
        )
    }
}

@Composable
private fun OverlayPermissionRow(
    hasOverlayPermission: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .border(0.5.dp, Border, RoundedCornerShape(12.dp))
            .clickable(enabled = !hasOverlayPermission, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Column {
            Text(
                text = "Overlay permission",
                color = White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Needed for on-screen blocking",
                color = Gray,
                fontSize = 11.sp
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (hasOverlayPermission) {
                Text(
                    text = "Granted",
                    color = Green,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(RedDim)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "GRANT",
                        color = Red,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                }
                Text(text = "›", color = Gray, fontSize = 18.sp)
            }
        }
    }
}