package com.telegramdrive.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.styropyr0.prismal.components.LocalPrismalParentGlassLayer
import com.styropyr0.prismal.depth.PrismalDepthInset
import com.styropyr0.prismal.depth.PrismalDepthShadow
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.shapes.PrismalCapsule
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalMergedSource
import com.styropyr0.prismal.sources.rememberPrismalWrappedSource
import com.styropyr0.prismal.specular.PrismalSpecular
import com.telegramdrive.app.ui.theme.TDriveColors
import com.telegramdrive.app.ui.theme.TDriveMotion
import com.telegramdrive.app.ui.utils.rememberAppHaptics

/**
 * TDriveSwitch — High-performance glassmorphic switch button.
 *
 * The background track is a sleek rounded capsule with smooth color and border transitions.
 *
 * The thumb is a horizontally stretched liquid glass pill (PrismalCapsule) sized slightly
 * larger than the track (38.dp wide x 30.dp high over a 26.dp track). Powered by Prismal
 * AGSL shaders, the glass pill refracts, magnifies, and morphs the track and its boundaries
 * beneath it as it glides with spring physics.
 *
 * @param checked Whether the switch is currently on.
 * @param onCheckedChange Callback invoked when the user toggles the switch.
 * @param modifier Modifier applied to the outer switch container.
 * @param enabled Whether the switch is interactive.
 * @param activeColor Accent color for the track when checked (defaults to [TDriveColors.Primary]).
 * @param inactiveColor Color of the track when unchecked (defaults to [TDriveColors.SurfaceBright]).
 */
@Composable
fun TDriveSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = TDriveColors.Primary,
    inactiveColor: Color = TDriveColors.SurfaceBright
) {
    val density = LocalDensity.current
    val haptic = rememberAppHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // ── Dimensions ─────────────────────────────────────────────────────────────
    // Container provides breathing room for the horizontally stretched thumb
    val containerWidth = 60.dp
    val containerHeight = 34.dp

    val trackWidth = 54.dp
    val trackHeight = 26.dp

    // Horizontally stretched pill thumb: 38.dp wide x 30.dp high (wider than it is tall, slightly taller than track)
    val thumbWidth = 38.dp
    val thumbHeight = 30.dp

    // Travel range for the sliding thumb
    val minOffsetX = 2.dp
    val maxOffsetX = containerWidth - thumbWidth - 2.dp

    // ── Animations ────────────────────────────────────────────────────────────
    val thumbOffsetX by animateDpAsState(
        targetValue = if (checked) maxOffsetX else minOffsetX,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "switchThumbOffset"
    )

    // Subtle horizontal stretch on press/drag
    val thumbScaleX by animateFloatAsState(
        targetValue = if (isPressed) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 420f),
        label = "switchThumbStretch"
    )

    val trackColor by animateColorAsState(
        targetValue = if (checked) activeColor else inactiveColor,
        animationSpec = tween(TDriveMotion.chipSwitchMs),
        label = "switchTrackColor"
    )

    val trackBorderColor by animateColorAsState(
        targetValue = if (checked) activeColor.copy(alpha = 0.85f) else TDriveColors.Outline,
        animationSpec = tween(TDriveMotion.chipSwitchMs),
        label = "switchTrackBorder"
    )

    // ── Prismal AGSL Layers & Backdrop Merging ────────────────────────────────
    val parentGlassLayer = LocalPrismalParentGlassLayer.current
    val localUnderlayBackdrop = rememberPrismalGlassLayer()
    val trackBackdrop = rememberPrismalGlassLayer()

    localUnderlayBackdrop.readSamplingState()
    trackBackdrop.readSamplingState()
    parentGlassLayer?.readSamplingState()

    val underlayBackdrop = parentGlassLayer ?: localUnderlayBackdrop

    // Merge underlay with track scaled inside the glass lens
    val thumbMergedBackdrop = rememberPrismalMergedSource(
        underlayBackdrop,
        rememberPrismalWrappedSource(trackBackdrop) { drawPrismalGlass ->
            scale(0.85f, 0.85f) {
                drawPrismalGlass()
            }
        }
    )

    Box(
        modifier = modifier
            .size(width = containerWidth, height = containerHeight)
            .semantics { role = Role.Switch }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                haptic.click()
                onCheckedChange?.invoke(!checked)
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // ── 1. Underlay Layer (captures surrounding surface) ───────────────────
        Box(
            modifier = Modifier
                .matchParentSize()
                .prismalGlassLayer(localUnderlayBackdrop)
        )

        // ── 2. The Track (Captured into trackBackdrop for AGSL sampling) ────────
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = trackWidth, height = trackHeight)
                .prismalGlassLayer(trackBackdrop)
                .clip(PrismalCapsule())
                .background(trackColor)
                .border(BorderStroke(1.dp, trackBorderColor), PrismalCapsule())
        )

        // ── 3. The Liquid Glass Horizontally Stretched Pill Thumb ─────────────
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbOffsetX)
                .size(width = thumbWidth, height = thumbHeight)
                .graphicsLayer {
                    scaleX = thumbScaleX
                }
                // Genuine AGSL Liquid Glass Shader with Ambient Specular & Depth Shadow
                .drawPrismalGlass(
                    backdrop = thumbMergedBackdrop,
                    shape = { PrismalCapsule() },
                    effects = {
                        applyPrismalGlassEffects(
                            density = density,
                            adaptiveLuminance = true,
                            luminance = 0.5f,
                            blurRadiusPx = with(density) { 6.dp.toPx() },
                            refractionHeightPx = with(density) { 6.dp.toPx() },
                            refractionAmountPx = with(density) { 12.dp.toPx() },
                            chromaticAberration = 1.0f
                        )
                    },
                    specular = {
                        PrismalSpecular.Ambient.copy(
                            width = PrismalSpecular.Ambient.width / 1.5f,
                            blurRadius = PrismalSpecular.Ambient.blurRadius / 1.5f,
                            alpha = 0.85f
                        )
                    },
                    depthShadow = {
                        PrismalDepthShadow(
                            radius = 4.dp,
                            color = Color.Black.copy(alpha = 0.25f)
                        )
                    },
                    depthInset = {
                        PrismalDepthInset(
                            radius = 3.dp,
                            alpha = 0.45f
                        )
                    },
                    onDrawSurface = {
                        // Subtle frosted glass translucency
                        drawRect(Color.White.copy(alpha = 0.15f))
                    }
                )
        )
    }
}
