package com.telegramdrive.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.telegramdrive.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// "slow -> fast -> slow" S-curve easing
private val SlowFastSlowEasing = CubicBezierEasing(0.50f, 0.0f, 0.20f, 1.0f)

@Composable
fun LaunchSplashOverlay(
    isReady: Boolean,
    onSplashComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var minHoldPassed by remember { mutableStateOf(false) }

    // Ensure splash is visible for at least 350ms so it doesn't flash if engine connects instantly
    LaunchedEffect(Unit) {
        delay(350)
        minHoldPassed = true
    }

    val iconAlpha = remember { Animatable(1f) }
    val iconScale = remember { Animatable(1f) }
    val wordmarkOffsetY = remember { Animatable(0f) }
    val wordmarkHeightProgress = remember { Animatable(0f) }
    val bgAlpha = remember { Animatable(1f) }

    var animationStarted by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = bgAlpha.value }
            .background(Color(0xFF080C18))
    ) {
        val screenHeight = maxHeight
        val topPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
        val headerLogoY = topPadding + 16.dp + 4.dp
        val centerY = screenHeight / 2
        val splashIconY = centerY - 54.dp
        val splashTextY = centerY + 36.dp

        val totalAscentDistance = (splashTextY + 16.dp) - headerLogoY

        LaunchedEffect(isReady, minHoldPassed) {
            if (isReady && minHoldPassed && !animationStarted) {
                animationStarted = true

                // Phase 1: Anticipation - "first go a lil down"
                launch {
                    iconAlpha.animateTo(0f, animationSpec = tween(300, easing = FastOutSlowInEasing))
                }
                launch {
                    iconScale.animateTo(0.85f, animationSpec = tween(300, easing = FastOutSlowInEasing))
                }

                // Wordmark dips down 16.dp smoothly
                wordmarkOffsetY.animateTo(
                    targetValue = 16f,
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                )

                // Phase 2: Launch & Glide up - "then up with speed as slow->fast->slow" (lil slow duration: 850ms)
                launch {
                    wordmarkHeightProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 850, easing = SlowFastSlowEasing)
                    )
                }

                val targetAscentOffset = 16f - totalAscentDistance.value
                launch {
                    delay(700)
                    bgAlpha.animateTo(0f, animationSpec = tween(150))
                }

                wordmarkOffsetY.animateTo(
                    targetValue = targetAscentOffset,
                    animationSpec = tween(durationMillis = 850, easing = SlowFastSlowEasing)
                )

                onSplashComplete()
            }
        }

        val currentWordmarkY = splashTextY + wordmarkOffsetY.value.dp
        val currentWordmarkHeight = lerp(36.dp, 28.dp, wordmarkHeightProgress.value)

        // Centered App Icon
        if (iconAlpha.value > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = splashIconY)
                    .graphicsLayer {
                        alpha = iconAlpha.value
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.75f),
                    shadowElevation = 14.dp,
                    modifier = Modifier.size(84.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_keepme_logo),
                        contentDescription = "KeepMe App Icon",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(22.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        // Wordmark Logo with custom Anticipation Dip and Slow->Fast->Slow Glide
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = currentWordmarkY),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_keepme_wordmark),
                contentDescription = "KeepMe Logo",
                modifier = Modifier.height(currentWordmarkHeight),
                contentScale = ContentScale.Fit
            )
        }
    }
}
