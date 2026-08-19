package com.saurabh.focusapp.screens

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saurabh.focusapp.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import com.saurabh.focusapp.R

/**
 * SplashScreen — visual layer only.
 * Animation sequencing, timings, spring/tween specs, and the exact moment
 * onSplashFinished() fires are all unchanged. Only ring/icon/dot styling
 * and color sourcing were reworked.
 */

private object SplashPalette {
    val Accent = Color(0xFFE8A33D)
    val InkSurface = Color(0xFF14161A)
    val InkSurfaceAlt = Color(0xFF1D2026)
}

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {

    val iconScale = remember { Animatable(0.6f) }
    val contentAlpha = remember { Animatable(0f) }
    val ring1Alpha = remember { Animatable(0f) }
    val ring2Alpha = remember { Animatable(0f) }
    val ring3Alpha = remember { Animatable(0f) }
    val ring1Scale = remember { Animatable(0.4f) }
    val ring2Scale = remember { Animatable(0.4f) }
    val ring3Scale = remember { Animatable(0.4f) }

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        if (BuildConfig.DEBUG) Log.d("SplashScreen", "Animation started")

        launch {
            iconScale.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        launch {
            contentAlpha.animateTo(1f, animationSpec = tween(400))
        }

        launch {
            ring1Alpha.animateTo(0.7f, animationSpec = tween(400))
            ring1Scale.animateTo(1f, animationSpec = tween(500, easing = EaseOut))
        }

        delay(100.milliseconds)
        launch {
            ring2Alpha.animateTo(0.6f, animationSpec = tween(400))
            ring2Scale.animateTo(1f, animationSpec = tween(500, easing = EaseOut))
        }

        delay(100.milliseconds)
        launch {
            ring3Alpha.animateTo(0.5f, animationSpec = tween(400))
            ring3Scale.animateTo(1f, animationSpec = tween(500, easing = EaseOut))
        }

        delay(1000.milliseconds)

        contentAlpha.animateTo(0f, animationSpec = tween(300))

        if (BuildConfig.DEBUG) {
            Log.d(
                "SplashScreen",
                "Animation finished in ${System.currentTimeMillis() - startTime}ms"
            )
        }
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {

        // ── Concentric rings — amber-tinted instead of flat gray ──
        Ring(
            size = 360.dp,
            alphaProvider = { ring3Alpha.value },
            scaleProvider = { ring3Scale.value }
        )
        Ring(
            size = 260.dp,
            alphaProvider = { ring2Alpha.value },
            scaleProvider = { ring2Scale.value }
        )
        Ring(
            size = 180.dp,
            alphaProvider = { ring1Alpha.value },
            scaleProvider = { ring1Scale.value }
        )

        // ── Center content ──
        Column(
            modifier = Modifier.graphicsLayer { alpha = contentAlpha.value },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            // ── Icon box — dark gradient tile instead of flat surfaceVariant ──
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                    }
                    .size(88.dp)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(SplashPalette.InkSurface, SplashPalette.InkSurfaceAlt)
                        ),
                        shape = RoundedCornerShape(26.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(Modifier.height(22.dp))

            // ── App name ──
            Text(
                text = "Focus",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            // ── Tagline ──
            Text(
                text = "Stay in the zone",
                style = MaterialTheme.typography.bodySmall,
                color = SplashPalette.Accent,
                letterSpacing = 0.3.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(52.dp))

            // ── Loading dots ──
            LoadingDots()
        }

        // ── Version ──
        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .graphicsLayer { alpha = contentAlpha.value },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            letterSpacing = 0.5.sp
        )
    }
}

// ── Concentric ring — subtle amber outline instead of neutral outlineVariant ──
@Composable
private fun Ring(
    size: Dp,
    alphaProvider: () -> Float,
    scaleProvider: () -> Float
) {
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                alpha = alphaProvider()
                scaleX = scaleProvider()
                scaleY = scaleProvider()
            }
            .border(
                width = 1.dp,
                color = SplashPalette.Accent.copy(alpha = 0.25f),
                shape = CircleShape
            )
    )
}

// ── Animated 3-dot loader — amber dots instead of neutral gray ──
@Composable
private fun LoadingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    val dot1Alpha = infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 0),
            repeatMode = RepeatMode.Reverse
        ), label = "d1"
    )
    val dot2Alpha = infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 160),
            repeatMode = RepeatMode.Reverse
        ), label = "d2"
    )
    val dot3Alpha = infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 320),
            repeatMode = RepeatMode.Reverse
        ), label = "d3"
    )

    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf(dot1Alpha, dot2Alpha, dot3Alpha).forEach { alphaState ->
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .graphicsLayer { alpha = alphaState.value }
                    .background(
                        SplashPalette.Accent,
                        CircleShape
                    )
            )
        }
    }
}