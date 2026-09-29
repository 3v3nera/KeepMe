package com.telegramdrive.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import com.telegramdrive.app.ui.theme.TDriveColors

/**
 * App background: flat near-black canvas plus a single very faint accent glow at the top edge.
 * Static on purpose: no per-frame recomposition, so every screen scrolls smoothly on top of it.
 */
@Composable
fun CanvasBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TDriveColors.Canvas)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TDriveColors.Accent.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, -size.height * 0.02f),
                        radius = size.width * 0.9f
                    )
                )
            }
    )
}
