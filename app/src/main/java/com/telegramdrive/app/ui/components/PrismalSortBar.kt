package com.telegramdrive.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.styropyr0.prismal.depth.PrismalDepthInset
import com.styropyr0.prismal.depth.PrismalDepthShadow
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.shapes.PrismalCapsule
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import com.styropyr0.prismal.specular.PrismalSpecular
import com.telegramdrive.app.ui.screen.folders.SortDirection
import com.telegramdrive.app.ui.screen.folders.SortOption
import com.telegramdrive.app.ui.utils.rememberAppHaptics
import kotlinx.coroutines.launch

private val ArrowSortIcon = ImageVector.Builder(
    name = "ArrowSort",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.4f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 4f)
        lineTo(12f, 20f)
        moveTo(19f, 13f)
        lineTo(12f, 20f)
        lineTo(5f, 13f)
    }
}.build()

@Composable
fun PrismalSortBar(
    sortOption: SortOption,
    onSortOptionChange: (SortOption) -> Unit,
    sortDirection: SortDirection,
    onToggleSortDirection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberAppHaptics()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // ── Prismal AGSL Glass Layers for optical refraction ──
    val trackBackdrop = rememberPrismalGlassLayer()
    val arrowBackdrop = rememberPrismalGlassLayer()
    trackBackdrop.readSamplingState()
    arrowBackdrop.readSamplingState()

    val trackWidth = 276.dp
    val trackHeight = 44.dp
    val trackPadding = 3.dp
    val tabCount = 4
    val tabWidth = (trackWidth - (trackPadding * 2)) / tabCount
    val slabHeight = trackHeight - (trackPadding * 2)

    val targetSlabX = trackPadding + (tabWidth * sortOption.ordinal)

    // Smooth, calibrated spring animation (less jelly, more refined glide)
    val slabOffsetX by animateDpAsState(
        targetValue = targetSlabX,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 360f),
        label = "slabOffsetX"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Unified Pill Track with Crystal Clear Refracting Glass Slab ──
        Box(
            modifier = Modifier
                .width(trackWidth)
                .height(trackHeight)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val effectiveX = (offset.x - trackPadding.toPx()).coerceAtLeast(0f)
                        val tabWidthPx = tabWidth.toPx()
                        val tappedIndex = (effectiveX / tabWidthPx).toInt().coerceIn(0, 3)
                        haptics.click()
                        onSortOptionChange(SortOption.values()[tappedIndex])
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // ── 1. The Track Base with 4 Tabs (Sampled by Prismal AGSL for refraction) ──
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .prismalGlassLayer(trackBackdrop)
                    .clip(CircleShape)
                    .background(Color(0xFF131B2C))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .padding(trackPadding)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SortOption.values().forEach { option ->
                        val isSelected = option == sortOption

                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) Color.White else Color(0xFF8E9BAE),
                            animationSpec = tween(180),
                            label = "textColor"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = textColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // ── 2. Crystal Clear Refracting Glass Slab (Zero Frost, Clean Optics) ──
            Box(
                modifier = Modifier
                    .offset(x = slabOffsetX)
                    .size(width = tabWidth, height = slabHeight)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        spotColor = Color.Black.copy(alpha = 0.35f)
                    )
                    // Pure crystal clear AGSL optical refraction — zero blur/frost
                    .drawPrismalGlass(
                        backdrop = trackBackdrop,
                        shape = { PrismalCapsule() },
                        effects = {
                            applyPrismalGlassEffects(
                                density = density,
                                adaptiveLuminance = false,
                                luminance = 1.0f,
                                blurRadiusPx = 0f, // No blur = crystal clear, zero frost
                                refractionHeightPx = with(density) { 5.dp.toPx() },
                                refractionAmountPx = with(density) { 8.dp.toPx() },
                                chromaticAberration = 0.5f
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
                                radius = 3.dp,
                                color = Color.Black.copy(alpha = 0.20f)
                            )
                        },
                        depthInset = {
                            PrismalDepthInset(
                                radius = 2.dp,
                                alpha = 0.35f
                            )
                        }
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.80f),
                                Color.White.copy(alpha = 0.20f)
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        // Fills the remaining space to push the arrow to the extreme right
        Spacer(modifier = Modifier.weight(1f))

        // ── Sort Order Toggle Arrow (Extreme Right, only for Date & Size) ──
        val showArrow = sortOption == SortOption.DATE || sortOption == SortOption.SIZE

        AnimatedVisibility(
            visible = showArrow,
            enter = fadeIn(tween(200)) + scaleIn(spring(dampingRatio = 0.70f, stiffness = 360f)),
            exit = fadeOut(tween(150)) + scaleOut(spring(dampingRatio = 0.70f, stiffness = 360f))
        ) {
            val arrowScale = remember { Animatable(1f) }

            val arrowRotation by animateFloatAsState(
                targetValue = if (sortDirection == SortDirection.DESCENDING) 0f else 180f,
                animationSpec = spring(dampingRatio = 0.70f, stiffness = 360f),
                label = "arrowRotation"
            )

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .graphicsLayer {
                        scaleX = arrowScale.value
                        scaleY = arrowScale.value
                    }
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        spotColor = Color.Black.copy(alpha = 0.35f)
                    )
                    .drawPrismalGlass(
                        backdrop = arrowBackdrop,
                        shape = { PrismalCapsule() },
                        effects = {
                            applyPrismalGlassEffects(
                                density = density,
                                adaptiveLuminance = false,
                                luminance = 1.0f,
                                blurRadiusPx = 0f,
                                refractionHeightPx = with(density) { 5.dp.toPx() },
                                refractionAmountPx = with(density) { 7.dp.toPx() },
                                chromaticAberration = 0.5f
                            )
                        },
                        specular = {
                            PrismalSpecular.Ambient.copy(
                                width = PrismalSpecular.Ambient.width / 1.5f,
                                blurRadius = PrismalSpecular.Ambient.blurRadius / 1.5f,
                                alpha = 0.80f
                            )
                        },
                        onDrawSurface = {
                            drawRect(Color(0xFF131B2C).copy(alpha = 0.85f))
                        }
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.70f),
                                Color.White.copy(alpha = 0.15f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            coroutineScope.launch {
                                arrowScale.animateTo(0.92f, tween(60))
                                arrowScale.animateTo(1.0f, spring(dampingRatio = 0.70f, stiffness = 360f))
                            }
                            haptics.click()
                            onToggleSortDirection()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ArrowSortIcon,
                    contentDescription = if (sortDirection == SortDirection.DESCENDING) "Descending" else "Ascending",
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            rotationZ = arrowRotation
                        }
                )
            }
        }
    }
}
