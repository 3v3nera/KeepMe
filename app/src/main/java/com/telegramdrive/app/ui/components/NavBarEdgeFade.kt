package com.telegramdrive.app.ui.components

import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.draw.drawWithContent

/**
 * Geometric bounds for both the liquid glass nav capsule and the circular FAB.
 */
data class NavBarBounds(
    val capsule: Rect = Rect.Zero,
    val fab: Rect = Rect.Zero
) {
    val isEmpty: Boolean get() = capsule == Rect.Zero && fab == Rect.Zero
}

/**
 * Provides the actual visible bounding rectangles of the navbar capsule and FAB.
 */
val LocalNavBarBounds = staticCompositionLocalOf<State<NavBarBounds>> {
    androidx.compose.runtime.mutableStateOf(NavBarBounds())
}

/** Layout constants of the floating nav bar, shared so screens pad their content consistently. */
object NavBarMetrics {
    /** Capsule (72) + vertical padding (2 x 24) + bottom margin (16) = space the bar occupies. */
    val contentBottomPadding = 136.dp
}

// Keep backward-compat alias so nothing else breaks at compile time
@Deprecated("Use LocalNavBarBounds instead", ReplaceWith("LocalNavBarBounds"))
val LocalNavBarTopY = compositionLocalOf { 0f }

/**
 * Pixel-perfect gradient fade that hides content strictly where it slides
 * underneath the floating navbar capsule or FAB.
 *
 * Rules:
 * 1. Content above the navbar is 100% visible and untouched.
 * 2. If content is outside the horizontal bounds of the capsule/FAB (e.g. gap or margins),
 *    it is 100% visible and untouched.
 * 3. As content slides under the navbar top edge, only the portion that enters
 *    underneath the glass fades out smoothly via [BlendMode.DstOut].
 *
 * @param fadeZonePx Height of the gradient transition zone in pixels (runs downwards
 *   from the top edge into the navbar).
 * @param horizontalPaddingPx Extra horizontal padding for tolerance.
 */
fun Modifier.fadeAtNavBarEdge(
    fadeZonePx: Float = 14f,
    horizontalPaddingPx: Float = 4f
): Modifier = composed {
    val boundsState = LocalNavBarBounds.current

    var posX by remember { mutableFloatStateOf(0f) }
    var posY by remember { mutableFloatStateOf(0f) }
    var widthPx by remember { mutableIntStateOf(0) }
    var heightPx by remember { mutableIntStateOf(0) }

    this
        .onGloballyPositioned { coords ->
            val windowPos = coords.positionInWindow()
            posX = windowPos.x
            posY = windowPos.y
            widthPx = coords.size.width
            heightPx = coords.size.height
        }
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .drawWithContent {
            drawContent()

            // Read inside the draw scope so bounds changes only invalidate drawing, not composition
            val bounds = boundsState.value
            if (bounds.isEmpty) return@drawWithContent

            val elementRight = posX + widthPx
            val elementBottom = posY + heightPx

            val cap = bounds.capsule
            val overlapsCapsule = cap != Rect.Zero &&
                elementRight > (cap.left - horizontalPaddingPx) &&
                posX < (cap.right + horizontalPaddingPx) &&
                elementBottom > cap.top &&
                posY < cap.bottom

            val fab = bounds.fab
            val overlapsFab = fab != Rect.Zero &&
                elementRight > (fab.left - horizontalPaddingPx) &&
                posX < (fab.right + horizontalPaddingPx) &&
                elementBottom > fab.top &&
                posY < fab.bottom

            if (overlapsCapsule || overlapsFab) {
                val activeTop = when {
                    overlapsCapsule && overlapsFab -> minOf(cap.top, fab.top)
                    overlapsCapsule -> cap.top
                    else -> fab.top
                }

                // Where the navbar top edge falls within this composable's local coordinates
                val navTopLocal = (activeTop - posY).coerceIn(0f, size.height)

                // Only erase the portion below the navbar top edge (navTopLocal to size.height)
                if (navTopLocal < size.height) {
                    val fadeEndLocal = (navTopLocal + fadeZonePx).coerceAtMost(size.height)

                    // 1. Soft gradient transition starting at navTopLocal (transparent = keep visible)
                    // down to fadeEndLocal (black = fully erased by DstOut)
                    if (fadeEndLocal > navTopLocal) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black),
                                startY = navTopLocal,
                                endY = fadeEndLocal
                            ),
                            blendMode = BlendMode.DstOut
                        )
                    }

                    // 2. Solid erase for anything further below fadeEndLocal
                    if (fadeEndLocal < size.height) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(0f, fadeEndLocal),
                            size = Size(size.width, size.height - fadeEndLocal),
                            blendMode = BlendMode.DstOut
                        )
                    }
                }
            }
        }
}
