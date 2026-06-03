package com.saurabh.skipad.screens

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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saurabh.skipad.BuildConfig
import com.saurabh.skipad.R
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {

    // ── Animation states ──
    val iconScale = remember { Animatable(0.6f) }
    val contentAlpha = remember { Animatable(0f) }
    val ring1Alpha = remember { Animatable(0f) }
    val ring2Alpha = remember { Animatable(0f) }
    val ring3Alpha = remember { Animatable(0f) }
    val ring1Scale = remember { Animatable(0.4f) }
    val ring2Scale = remember { Animatable(0.4f) }
    val ring3Scale = remember { Animatable(0.4f) }

    LaunchedEffect(Unit) {
        // Icon pops in
        iconScale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        // Text + rings fade in
        contentAlpha.animateTo(1f, animationSpec = tween(400))

        // Rings expand outward in sequence
        ring1Alpha.animateTo(0.7f, animationSpec = tween(500))
        ring1Scale.animateTo(1f, animationSpec = tween(600, easing = EaseOut))

        delay(100.milliseconds)
        ring2Alpha.animateTo(0.6f, animationSpec = tween(500))
        ring2Scale.animateTo(1f, animationSpec = tween(600, easing = EaseOut))

        delay(100.milliseconds)
        ring3Alpha.animateTo(0.5f, animationSpec = tween(500))
        ring3Scale.animateTo(1f, animationSpec = tween(600, easing = EaseOut))

        delay(800.milliseconds)

        // Fade everything out
        contentAlpha.animateTo(0f, animationSpec = tween(350))
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {

        // ── Concentric rings ──
        Ring(
            size = 360.dp,
            alpha = ring3Alpha.value,
            scale = ring3Scale.value
        )
        Ring(
            size = 260.dp,
            alpha = ring2Alpha.value,
            scale = ring2Scale.value
        )
        Ring(
            size = 180.dp,
            alpha = ring1Alpha.value,
            scale = ring1Scale.value
        )

        // ── Center content ──
        Column(
            modifier = Modifier.graphicsLayer { alpha = contentAlpha.value },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            // ── Icon box ──
            Box(
                modifier = Modifier
                    .scale(iconScale.value)
                    .size(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(22.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = null
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── App name ──
            Text(
                text = "Focus",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Medium,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.3.sp,
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

// ── Concentric ring ──
@Composable
private fun Ring(
    size: Dp,
    alpha: Float,
    scale: Float
) {
    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .graphicsLayer { this.alpha = alpha }
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
    )
}

// ── Animated 3-dot loader ──
@Composable
private fun LoadingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 0),
            repeatMode = RepeatMode.Reverse
        ), label = "d1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 160),
            repeatMode = RepeatMode.Reverse
        ), label = "d2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 320),
            repeatMode = RepeatMode.Reverse
        ), label = "d3"
    )

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(dot1Alpha, dot2Alpha, dot3Alpha).forEach { a ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer { alpha = a }
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        CircleShape
                    )
            )
        }
    }
}