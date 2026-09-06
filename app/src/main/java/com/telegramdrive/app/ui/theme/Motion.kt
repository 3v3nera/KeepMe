package com.telegramdrive.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.ContentTransform

object TDriveMotion {

    // ── Screen-level nav transitions ──────────────────────────────────────────
    val screenEnter = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f)
    ) + fadeIn(animationSpec = tween(200))

    val screenExit = slideOutHorizontally(
        targetOffsetX = { -it / 3 },
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f)
    ) + fadeOut(animationSpec = tween(150))

    val screenPopEnter = slideInHorizontally(
        initialOffsetX = { -it / 3 },
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f)
    ) + fadeIn(animationSpec = tween(200))

    val screenPopExit = slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f)
    ) + fadeOut(animationSpec = tween(150))

    // ── Tab-switch transitions (cross-fade, no slide) ─────────────────────────
    val tabEnter = fadeIn(animationSpec = tween(160))
    val tabExit  = fadeOut(animationSpec = tween(120))

    // ── List / grid item stagger ───────────────────────────────────────────────
    fun itemEnter(index: Int) = fadeIn(
        animationSpec = tween(280, delayMillis = (index * 35).coerceAtMost(400))
    ) + slideInVertically(
        initialOffsetY = { it / 5 },
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f)
    )

    // ── Component-level springs ────────────────────────────────────────────────
    /** Snappy bounce on FAB / bottom-bar icon tap */
    val fabSpring: SpringSpec<Float> = spring(dampingRatio = 0.55f, stiffness = 600f)

    /** Card press-down feel */
    val cardPress: SpringSpec<Float> = spring(dampingRatio = 0.45f, stiffness = 900f)

    /** Tab icon pop (slight over-shoot) */
    val tabIconPop: SpringSpec<Float> = spring(dampingRatio = 0.4f, stiffness = 700f)

    /** Duration (ms) for chip/colour/size swap tweens — use tween(chipSwitchMs) at call sites */
    const val chipSwitchMs = 200

    // ── Overlay enter/exit (selection FAB, info strip, etc.) ──────────────────
    val overlayEnter = scaleIn(
        initialScale = 0.82f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 500f)
    ) + fadeIn(tween(160))

    val overlayExit = scaleOut(
        targetScale = 0.82f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 500f)
    ) + fadeOut(tween(120))

    // ── AnimatedContent transforms for header/title morphs ────────────────────
    val titleMorphIn: ContentTransform
        get() = (slideInVertically(tween(200)) { -it / 2 } + fadeIn(tween(180))) togetherWith
                (slideOutVertically(tween(160)) { it / 2 } + fadeOut(tween(140)))

    val iconMorphIn: ContentTransform
        get() = (scaleIn(spring(dampingRatio = 0.6f, stiffness = 600f)) + fadeIn(tween(150))) togetherWith
                (scaleOut(spring(dampingRatio = 0.6f, stiffness = 600f)) + fadeOut(tween(120)))

    // ── Content swap (generic cross-dissolve) ─────────────────────────────────
    val contentSwap = fadeIn(tween(200)) togetherWith fadeOut(tween(150))

    // ── Viewer-specific ───────────────────────────────────────────────────────
    val dismissDragSpring: SpringSpec<Float> = spring(dampingRatio = 0.72f, stiffness = 420f)
    val viewerFadeOut: TweenSpec<Float> = tween(durationMillis = 200)

    /** Double-tap zoom spring — snappy with slight over-shoot */
    val doubleTapZoom: SpringSpec<Float> = spring(dampingRatio = 0.65f, stiffness = 600f)
}
