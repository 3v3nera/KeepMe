package com.telegramdrive.app.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import io.github.om252345.composemeshgradient.MeshGradient
import io.github.om252345.composemeshgradient.rememberMeshGradientState
import io.github.om252345.composemeshgradient.utils.SimplexNoise
import kotlinx.coroutines.launch
import com.telegramdrive.app.ui.theme.TDriveColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.lerp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
@Composable
fun AnimatedMeshBackground(
    targetColors: List<Color> = listOf(
        TDriveColors.Primary.copy(alpha = 0.25f),
        TDriveColors.Background,
        TDriveColors.Secondary.copy(alpha = 0.15f),
        TDriveColors.Background,
        TDriveColors.Primary.copy(alpha = 0.15f),
        TDriveColors.Background,
        TDriveColors.Secondary.copy(alpha = 0.25f),
        TDriveColors.Primary.copy(alpha = 0.2f),
        TDriveColors.Background
    ),
    modifier: Modifier = Modifier
) {
    val width = 3
    val height = 3

    val animatedColors = targetColors.mapIndexed { index, color ->
        animateColorAsState(
            targetValue = color,
            animationSpec = tween(1200, easing = LinearOutSlowInEasing),
            label = "meshColor$index"
        ).value
    }

    val initialPoints = remember {
        Array(width * height) { i ->
            val col = i % width
            val row = i / width
            Offset(x = col / (width - 1f), y = row / (height - 1f))
        }
    }
    
    val meshState = rememberMeshGradientState(points = initialPoints, colors = animatedColors.toTypedArray())

    LaunchedEffect(Unit) {
        var time = 0f
        val basePoints = initialPoints.toList()
        val currentPoints = initialPoints.toMutableList()
        val targetPoints = initialPoints.toMutableList()
        var lastFrameTime = 0L

        while (true) {
            withFrameNanos { frameTime ->
                if (lastFrameTime == 0L) lastFrameTime = frameTime
                val deltaTime = (frameTime - lastFrameTime) / 1_000_000_000.0f
                lastFrameTime = frameTime
                time += deltaTime * 0.15f // Slow down the animation speed for a relaxed ambient feel

                for (i in targetPoints.indices) {
                    val col = i % width
                    val row = i / width
                    val isBorder = row == 0 || row == height - 1 || col == 0 || col == width - 1

                    // We only animate the inner points to keep the border fixed to the screen edges
                    if (!isBorder) {
                        val bp = basePoints[i]
                        val noiseX = SimplexNoise.noise(bp.x * 1.5f, time + i) * 0.3f
                        val noiseY = SimplexNoise.noise(bp.y * 1.5f, time + i + 100f) * 0.3f
                        targetPoints[i] = Offset(bp.x + noiseX, bp.y + noiseY)
                    }
                }

                for (i in currentPoints.indices) {
                    val lerped = lerp(currentPoints[i], targetPoints[i], (5f * deltaTime))
                    currentPoints[i] = lerped
                }

                launch {
                    meshState.snapAllPoints(currentPoints.toList())
                }
            }
        }
    }

    MeshGradient(
        modifier = modifier.fillMaxSize(),
        width = width,
        height = height,
        points = meshState.points.toTypedArray(),
        colors = animatedColors.toTypedArray()
    )
}
