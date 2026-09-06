package com.telegramdrive.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import com.telegramdrive.app.ui.theme.TDriveMotion

@Composable
fun ZoomableImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onScaleChanged: (Float) -> Unit = {},
    onTap: () -> Unit = {}
) {
    // ── State ──
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val animatedScale = remember { Animatable(1f) }
    val animatedOffsetX = remember { Animatable(0f) }
    val animatedOffsetY = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val minScale = 1f
    val maxScale = 5f
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(scale) { onScaleChanged(scale) }

    // ── Boundary clamping helper ──
    fun clampOffset(currentScale: Float, currentOffset: Offset): Offset {
        if (currentScale <= 1f) return Offset.Zero
        val maxX = ((viewportSize.width * currentScale - viewportSize.width) / 2f).coerceAtLeast(0f)
        val maxY = ((viewportSize.height * currentScale - viewportSize.height) / 2f).coerceAtLeast(0f)
        return Offset(
            x = currentOffset.x.coerceIn(-maxX, maxX),
            y = currentOffset.y.coerceIn(-maxY, maxY)
        )
    }

    // ── Animate zoom to target using the tuned doubleTapZoom spring ──
    fun animateZoomTo(targetScale: Float, targetOffset: Offset) {
        val clampedOffset = clampOffset(targetScale, targetOffset)
        coroutineScope.launch {
            coroutineScope {
                launch { animatedScale.animateTo(targetScale, TDriveMotion.doubleTapZoom) }
                launch { animatedOffsetX.animateTo(clampedOffset.x, TDriveMotion.doubleTapZoom) }
                launch { animatedOffsetY.animateTo(clampedOffset.y, TDriveMotion.doubleTapZoom) }
            }
        }
        scale = targetScale
        offset = clampedOffset
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { viewportSize = it }
            .pointerInput(Unit) {
                // ── Unified gesture handler ──
                val touchSlop = viewConfiguration.touchSlop
                var lastTapTimeMs = 0L
                var lastTapPosition = Offset.Zero
                val doubleTapTimeoutMs = 300L
                val doubleTapSlopPx = 100f

                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    val downTimeMs = System.currentTimeMillis()
                    val downPosition = firstDown.position
                    var lastPointerPosition = downPosition

                    var wasPinchOrDrag = false
                    var pointerCount = 1

                    do {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val changes = event.changes
                        pointerCount = maxOf(pointerCount, changes.size)

                        val primaryChange = changes.firstOrNull()
                        if (primaryChange != null) {
                            lastPointerPosition = primaryChange.position
                            if ((primaryChange.position - downPosition).getDistance() > touchSlop) {
                                wasPinchOrDrag = true
                            }
                        }

                        if (changes.size >= 2) {
                            // ── PINCH GESTURE ──
                            wasPinchOrDrag = true
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()

                            val newScale = (scale * zoom).coerceIn(minScale, maxScale)
                            scale = newScale
                            coroutineScope.launch { animatedScale.snapTo(newScale) }

                            if (newScale > 1f) {
                                val newOffset = clampOffset(newScale, offset + pan)
                                offset = newOffset
                                coroutineScope.launch {
                                    animatedOffsetX.snapTo(newOffset.x)
                                    animatedOffsetY.snapTo(newOffset.y)
                                }
                            } else {
                                offset = Offset.Zero
                                coroutineScope.launch {
                                    animatedOffsetX.snapTo(0f)
                                    animatedOffsetY.snapTo(0f)
                                }
                            }

                            changes.forEach { it.consume() }
                        } else if (changes.size == 1 && scale > 1.01f) {
                            // ── PAN WHILE ZOOMED ──
                            val change = changes.first()
                            if (change.positionChanged()) {
                                val pan = change.position - (change.previousPosition)
                                if (abs(pan.x) > 1f || abs(pan.y) > 1f) {
                                    wasPinchOrDrag = true
                                    val newOffset = clampOffset(scale, offset + pan)
                                    offset = newOffset
                                    coroutineScope.launch {
                                        animatedOffsetX.snapTo(newOffset.x)
                                        animatedOffsetY.snapTo(newOffset.y)
                                    }
                                    change.consume()
                                }
                            }
                        }
                    } while (changes.any { it.pressed })

                    // ── Pointer up: check for tap / double-tap ──
                    val totalDistanceMoved = (lastPointerPosition - downPosition).getDistance()
                    if (!wasPinchOrDrag && pointerCount == 1 && totalDistanceMoved < touchSlop) {
                        val upTimeMs = System.currentTimeMillis()
                        val tapDuration = upTimeMs - downTimeMs

                        if (tapDuration < 400L) {
                            val timeSinceLastTap = downTimeMs - lastTapTimeMs
                            val distFromLastTap = (downPosition - lastTapPosition).getDistance()

                            if (timeSinceLastTap < doubleTapTimeoutMs && distFromLastTap < doubleTapSlopPx) {
                                // ── DOUBLE-TAP ── with haptic confirmation
                                lastTapTimeMs = 0L
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (scale < 1.5f) {
                                    // Zoom IN to 2.5× centred on tap point
                                    val targetScale = 2.5f
                                    val centerX = viewportSize.width / 2f
                                    val centerY = viewportSize.height / 2f
                                    val targetOffset = Offset(
                                        x = (centerX - downPosition.x) * (targetScale - 1),
                                        y = (centerY - downPosition.y) * (targetScale - 1)
                                    )
                                    animateZoomTo(targetScale, targetOffset)
                                } else {
                                    // Zoom OUT to 1×
                                    animateZoomTo(1f, Offset.Zero)
                                }
                            } else {
                                // ── SINGLE TAP ──
                                lastTapTimeMs = downTimeMs
                                lastTapPosition = downPosition
                                onTap()
                            }
                        }
                    }
                }
            }
    ) {
        AsyncImage(
            model = model?.let { if (it is String) java.io.File(it) else it },
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = animatedScale.value
                    scaleY = animatedScale.value
                    translationX = animatedOffsetX.value
                    translationY = animatedOffsetY.value
                }
        )
    }
}
