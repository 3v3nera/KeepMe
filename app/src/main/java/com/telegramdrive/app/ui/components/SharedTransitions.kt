package com.telegramdrive.app.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/**
 * Shared-element plumbing for the folder open/close animation: the tapped folder card
 * expands into the folder screen and its sticker flies to the header (and back on exit).
 * Both locals are provided by NavGraph; without them (previews, dialogs) the modifiers are no-ops.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

fun folderBoundsKey(chatId: Long) = "folder_bounds_$chatId"
fun folderStickerKey(chatId: Long) = "folder_sticker_$chatId"

/** Card ⇄ screen container: the card's bounds grow to fill the screen. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.folderContainerTransition(chatId: Long): Modifier {
    val scope = LocalSharedTransitionScope.current ?: return this
    val visibility = LocalNavAnimatedScope.current ?: return this
    return with(scope) {
        this@folderContainerTransition.sharedBounds(
            sharedContentState = rememberSharedContentState(key = folderBoundsKey(chatId)),
            animatedVisibilityScope = visibility,
            enter = fadeIn(tween(240, delayMillis = 60)),
            exit = fadeOut(tween(200)),
            boundsTransform = { _, _ -> spring(dampingRatio = 0.84f, stiffness = 320f, visibilityThreshold = Rect.VisibilityThreshold) },
            // Scale (don't re-layout) the content while the bounds change: no per-frame reflow
            resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(ContentScale.FillWidth, Alignment.TopCenter),
            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(32.dp))
        )
    }
}

/** The emoji sticker keeps its identity while it travels between card and header. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.folderStickerTransition(chatId: Long): Modifier {
    val scope = LocalSharedTransitionScope.current ?: return this
    val visibility = LocalNavAnimatedScope.current ?: return this
    return with(scope) {
        this@folderStickerTransition.sharedElement(
            sharedContentState = rememberSharedContentState(key = folderStickerKey(chatId)),
            animatedVisibilityScope = visibility,
            boundsTransform = { _, _ -> spring(dampingRatio = 0.72f, stiffness = 380f, visibilityThreshold = Rect.VisibilityThreshold) }
        )
    }
}
