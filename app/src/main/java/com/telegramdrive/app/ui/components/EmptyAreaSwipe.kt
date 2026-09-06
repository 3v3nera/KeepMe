package com.telegramdrive.app.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.composed
import com.telegramdrive.app.ui.utils.AppHaptics
import com.telegramdrive.app.ui.utils.rememberAppHaptics
import kotlin.math.abs

/**
 * Screen-level horizontal swipe gesture modifier for switching between top-level tabs
 * (Home <-> Search <-> Folders).
 *
 * Runs in [PointerEventPass.Main] to permit horizontally scrollable child elements (like [LazyRow])
 * to consume their own drag gestures without interference. When a drag occurs on an empty area,
 * background, or non-horizontally-scrolling component, this modifier detects the unconsumed
 * horizontal swipe and smoothly switches tabs.
 */
fun Modifier.screenTabSwipe(
    onSwipeNext: () -> Unit = {},
    onSwipePrev: () -> Unit = {},
    haptics: AppHaptics? = null
): Modifier = composed {
    val effectiveHaptics = haptics ?: rememberAppHaptics()
    this.pointerInput(onSwipeNext, onSwipePrev) {
        val touchSlop = viewConfiguration.touchSlop
        val swipeThreshold = 50.dp.toPx()

        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Main, requireUnconsumed = false)
            var totalDragX = 0f
            var totalDragY = 0f
            var isHorizontalSwipe: Boolean? = null
            var hasTriggered = false

            do {
                val event = awaitPointerEvent(pass = PointerEventPass.Main)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                // If a child (like LazyRow) consumed this horizontal touch event, abort screen swipe
                if (change.isConsumed) {
                    break
                }

                if (change.pressed) {
                    val dragX = change.position.x - change.previousPosition.x
                    val dragY = change.position.y - change.previousPosition.y
                    totalDragX += dragX
                    totalDragY += dragY

                    val absX = abs(totalDragX)
                    val absY = abs(totalDragY)

                    // Determine gesture intent once movement exceeds touch slop
                    if (isHorizontalSwipe == null) {
                        if (absX > touchSlop || absY > touchSlop) {
                            isHorizontalSwipe = absX > absY * 1.25f
                        }
                    }

                    if (isHorizontalSwipe == true) {
                        if (!hasTriggered && absX > swipeThreshold) {
                            hasTriggered = true
                            change.consume()
                            effectiveHaptics.tick()
                            if (totalDragX < 0) {
                                onSwipeNext()
                            } else {
                                onSwipePrev()
                            }
                        }
                    } else if (isHorizontalSwipe == false) {
                        // Dominantly vertical drag — let vertical LazyColumn / screen scroll freely
                        break
                    }
                }
            } while (change.pressed)
        }
    }
}

// Alias for backwards compatibility
fun Modifier.emptyAreaTabSwipe(
    onSwipeNext: () -> Unit = {},
    onSwipePrev: () -> Unit = {}
): Modifier = screenTabSwipe(onSwipeNext, onSwipePrev)
