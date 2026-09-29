package com.telegramdrive.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.prismalGlassEffects
import com.styropyr0.prismal.shapes.PrismalCapsule
import com.telegramdrive.app.ui.theme.TDriveMotion
import com.telegramdrive.app.ui.utils.rememberAppHaptics
import kotlinx.coroutines.launch

data class SelectionBarActions(
    val selectedCount: Int,
    val onDelete: () -> Unit,
    val onShare: () -> Unit,
    val onDownload: () -> Unit,
    val onClear: () -> Unit
)

private enum class BarDisplayMode {
    NAV, ACTIONS
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloatingBottomBar(
    items: List<Triple<String, String, ImageVector>>, // Title, Route, Icon
    currentRoute: String?,
    isFoldersTab: Boolean = false,
    backdrop: PrismalBackdrop,
    onNavigate: (String) -> Unit,
    onUploadClick: () -> Unit,
    selectionActions: SelectionBarActions? = null,
    onBoundsChanged: ((NavBarBounds) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptics = rememberAppHaptics()
    val inSelectionMode = selectionActions != null

    // Cache the latest non-null actions so content remains rendered during the collapse animation
    var cachedActions by remember { mutableStateOf(selectionActions) }
    if (selectionActions != null) {
        cachedActions = selectionActions
    }

    var displayedMode by remember {
        mutableStateOf(if (inSelectionMode) BarDisplayMode.ACTIONS else BarDisplayMode.NAV)
    }

    // Choreographed animation controllers
    val capsuleScaleX = remember { Animatable(1f) }
    val capsuleScaleY = remember { Animatable(1f) }
    val capsuleAlpha = remember { Animatable(1f) }
    val fabRotation = remember { Animatable(if (inSelectionMode) 45f else 0f) }
    val fabPulse = remember { Animatable(1f) }

    var capsuleBounds by remember { mutableStateOf(Rect.Zero) }
    var fabBounds by remember { mutableStateOf(Rect.Zero) }

    DisposableEffect(Unit) {
        onDispose {
            onBoundsChanged?.invoke(NavBarBounds())
        }
    }

    // Converging choreography: collapse the old capsule into the button, swap content, then spring the
    // new capsule out. Every run first checks where the animatables actually are, so a cancelled run
    // (rapid select / deselect) can never leave the capsule collapsed or the FAB mis-rotated.
    LaunchedEffect(inSelectionMode) {
        val targetMode = if (inSelectionMode) BarDisplayMode.ACTIONS else BarDisplayMode.NAV
        val targetRotation = if (inSelectionMode) 45f else 0f
        val modeChanged = displayedMode != targetMode

        if (modeChanged) {
            // ── Phase 1: collapse the current capsule into the circular button ──
            launch {
                capsuleAlpha.animateTo(0f, tween(durationMillis = 260, easing = FastOutSlowInEasing))
            }
            launch {
                capsuleScaleY.animateTo(0.82f, tween(durationMillis = 280, easing = FastOutSlowInEasing))
            }
            capsuleScaleX.animateTo(0f, tween(durationMillis = 280, easing = FastOutSlowInEasing))
            displayedMode = targetMode
        }

        // ── Phase 2: rotate the button while the capsule springs out (also restores a capsule
        //    left half-collapsed by a cancelled run) ──
        if (modeChanged) {
            launch {
                fabPulse.animateTo(1.22f, tween(130, easing = FastOutSlowInEasing))
                fabPulse.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 160f))
            }
        }
        launch {
            fabRotation.animateTo(targetRotation, spring(dampingRatio = 0.58f, stiffness = 135f))
        }
        launch {
            capsuleAlpha.animateTo(1f, tween(durationMillis = 200, easing = LinearOutSlowInEasing))
        }
        launch {
            capsuleScaleY.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 135f))
        }
        capsuleScaleX.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.52f, stiffness = 125f)
        )
    }

    val context = LocalContext.current
    val tunerStorage = remember { FabTunerStorage(context) }
    var tuningParams by remember(isFoldersTab) {
        mutableStateOf(tunerStorage.loadParams(isFoldersTab))
    }
    var showTuningSheet by remember { mutableStateOf(false) }

    // Synchronized Size configuration (scales capsule height, tab item width, active pill indicator, and circular button)
    val defaultIndicatorSize = 56.dp
    val effectiveIndicatorSize = if (FabTunerConfig.ENABLE_FAB_TUNER) tuningParams.fabSize.dp else defaultIndicatorSize
    val itemSize = effectiveIndicatorSize + 8.dp
    val horizontalPadding = 8.dp

    val defaultElevation = if (isFoldersTab) 22.dp else 12.dp
    val effectiveElevation = if (FabTunerConfig.ENABLE_FAB_TUNER) tuningParams.elevation.dp else defaultElevation

    // Optical glass effects applied uniformly to the whole navigation bar (capsule + button)
    val fabEffects: com.styropyr0.prismal.PrismalGlassEffectProvider.() -> Unit = remember(density, isFoldersTab, tuningParams) {
        if (FabTunerConfig.ENABLE_FAB_TUNER) {
            {
                applyPrismalGlassEffects(
                    density = density,
                    adaptiveLuminance = false,
                    luminance = 0.5f,
                    blurRadiusPx = with(density) { tuningParams.blurRadius.dp.toPx() },
                    refractionHeightPx = with(density) { tuningParams.refractionHeight.dp.toPx() },
                    refractionAmountPx = with(density) { tuningParams.refractionAmount.dp.toPx() },
                    depthEffect = tuningParams.depthEffect,
                    chromaticAberration = tuningParams.chromaticAberration,
                    saturation = tuningParams.saturation,
                    useVibrancy = true
                )
            }
        } else if (isFoldersTab) {
            // High Z-Axis Optical Lift: 28.dp full dome + 64.dp refraction Z-distance displacement (blur stays 8.dp)
            {
                applyPrismalGlassEffects(
                    density = density,
                    adaptiveLuminance = false,
                    luminance = 0.5f,
                    blurRadiusPx = with(density) { 8.dp.toPx() },
                    refractionHeightPx = with(density) { (defaultIndicatorSize / 2).toPx() },
                    refractionAmountPx = with(density) { 64.dp.toPx() },
                    depthEffect = true,
                    chromaticAberration = 0.10f,
                    useVibrancy = true
                )
            }
        } else {
            prismalGlassEffects(density)
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Morphing Capsule Container: single container, no competing layouts or overlaps
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = capsuleScaleX.value
                    scaleY = capsuleScaleY.value
                    alpha = capsuleAlpha.value
                    transformOrigin = TransformOrigin(1f, 0.5f) // Anchored directly on the right edge facing the FAB!
                }
        ) {
            if (displayedMode == BarDisplayMode.ACTIONS) {
                cachedActions?.let { actions ->
                    SelectionActionCapsule(
                        actions = actions,
                        backdrop = backdrop,
                        itemSize = itemSize,
                        horizontalPadding = horizontalPadding,
                        effects = fabEffects,
                        elevation = effectiveElevation,
                        backingTintAlpha = if (FabTunerConfig.ENABLE_FAB_TUNER) tuningParams.backingTintAlpha else 0f,
                        onCapsuleBoundsChanged = { rect ->
                            capsuleBounds = rect
                            onBoundsChanged?.invoke(NavBarBounds(capsule = rect, fab = fabBounds))
                        }
                    )
                }
            } else {
                NavCapsule(
                    items = items,
                    currentRoute = currentRoute,
                    backdrop = backdrop,
                    itemSize = itemSize,
                    indicatorSize = effectiveIndicatorSize,
                    horizontalPadding = horizontalPadding,
                    effects = fabEffects,
                    elevation = effectiveElevation,
                    backingTintAlpha = if (FabTunerConfig.ENABLE_FAB_TUNER) tuningParams.backingTintAlpha else 0f,
                    onNavigate = onNavigate,
                    onLongClick = if (FabTunerConfig.ENABLE_FAB_TUNER) {
                        {
                            haptics.heavyClick()
                            showTuningSheet = true
                        }
                    } else null,
                    onCapsuleBoundsChanged = { rect ->
                        capsuleBounds = rect
                        onBoundsChanged?.invoke(NavBarBounds(capsule = rect, fab = fabBounds))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right circular button (+ / ✕)
        val uploadInteractionSource = remember { MutableInteractionSource() }
        val isPressed by uploadInteractionSource.collectIsPressedAsState()
        val uploadScale by animateFloatAsState(
            targetValue = if (isPressed) 0.88f else 1f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
            label = "uploadScale"
        )

        Box(
            modifier = Modifier
                .size(effectiveIndicatorSize)
                .onGloballyPositioned { coords ->
                    fabBounds = coords.boundsInWindow()
                    onBoundsChanged?.invoke(NavBarBounds(capsule = capsuleBounds, fab = fabBounds))
                }
                .graphicsLayer {
                    scaleX = uploadScale * fabPulse.value
                    scaleY = uploadScale * fabPulse.value
                }
                .shadow(effectiveElevation, CircleShape)
                .drawPrismalGlass(
                    backdrop = backdrop,
                    shape = { PrismalCapsule() },
                    effects = fabEffects,
                    specular = KeepMeGlass.specular,
                    depthInset = KeepMeGlass.depthInset,
                    onDrawSurface = KeepMeGlass.surface
                )
                .then(
                    if (inSelectionMode) {
                        Modifier.background(Color(0xFFFF453A).copy(alpha = 0.35f), CircleShape)
                    } else if (FabTunerConfig.ENABLE_FAB_TUNER && tuningParams.backingTintAlpha > 0f) {
                        Modifier.background(Color(0xFF0C101A).copy(alpha = tuningParams.backingTintAlpha), CircleShape)
                    } else Modifier
                )
                .clip(CircleShape)
                .combinedClickable(
                    interactionSource = uploadInteractionSource,
                    indication = null,
                    onClick = {
                        haptics.click()
                        if (inSelectionMode && selectionActions != null) {
                            selectionActions.onClear()
                        } else {
                            onUploadClick()
                        }
                    },
                    onLongClick = if (FabTunerConfig.ENABLE_FAB_TUNER) {
                        {
                            haptics.heavyClick()
                            showTuningSheet = true
                        }
                    } else null
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = if (inSelectionMode) "Deselect All" else "Upload Files",
                tint = Color.White,
                modifier = Modifier
                    .size(26.dp)
                    .graphicsLayer {
                        rotationZ = fabRotation.value
                    }
            )
        }
    }

    if (FabTunerConfig.ENABLE_FAB_TUNER && showTuningSheet) {
        LiquidGlassFabTunerPopup(
            params = tuningParams,
            isFoldersTab = isFoldersTab,
            onParamsChange = { updated ->
                tuningParams = updated
                tunerStorage.saveParams(updated, isFoldersTab)
            },
            onReset = {
                val reset = tunerStorage.resetParams(isFoldersTab)
                tuningParams = reset
            },
            onDismiss = {
                showTuningSheet = false
            }
        )
    }
}

@Composable
private fun SelectionActionCapsule(
    actions: SelectionBarActions,
    backdrop: PrismalBackdrop,
    itemSize: Dp,
    horizontalPadding: Dp,
    effects: com.styropyr0.prismal.PrismalGlassEffectProvider.() -> Unit,
    elevation: Dp,
    backingTintAlpha: Float = 0f,
    onCapsuleBoundsChanged: ((Rect) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptics = rememberAppHaptics()

    Box(
        modifier = modifier.padding(vertical = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .height(72.dp)
                .onGloballyPositioned { coords ->
                    onCapsuleBoundsChanged?.invoke(coords.boundsInWindow())
                }
                .shadow(elevation, CircleShape)
                .drawPrismalGlass(
                    backdrop = backdrop,
                    shape = { PrismalCapsule() },
                    effects = effects,
                    specular = KeepMeGlass.specular,
                    depthInset = KeepMeGlass.depthInset,
                    onDrawSurface = KeepMeGlass.surface
                )
                .then(
                    if (backingTintAlpha > 0f) {
                        Modifier.background(Color(0xFF0C101A).copy(alpha = backingTintAlpha), CircleShape)
                    } else Modifier
                )
                .padding(horizontal = horizontalPadding)
        ) {
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Delete Action Button
            SelectionActionButton(
                icon = Icons.Rounded.Delete,
                contentDescription = "Delete Selected",
                tint = Color(0xFFFF453A),
                itemSize = itemSize,
                onClick = {
                    haptics.heavyClick()
                    actions.onDelete()
                }
            )

            // 2. Share Action Button
            SelectionActionButton(
                icon = Icons.Rounded.Share,
                contentDescription = "Share Selected",
                tint = Color(0xFF6C8FFF),
                itemSize = itemSize,
                onClick = {
                    haptics.click()
                    actions.onShare()
                }
            )

            // 3. Download Action Button
            SelectionActionButton(
                icon = Icons.Rounded.Download,
                contentDescription = "Download Selected",
                tint = Color(0xFF00E5A0),
                itemSize = itemSize,
                onClick = {
                    haptics.success()
                    actions.onDownload()
                }
            )
        }
    }
}
}

@Composable
private fun SelectionActionButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    itemSize: Dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "actionScale"
    )

    Box(
        modifier = Modifier
            .size(itemSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = if (isPressed) 0.32f else 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun NavCapsule(
    items: List<Triple<String, String, ImageVector>>,
    currentRoute: String?,
    backdrop: PrismalBackdrop,
    itemSize: Dp,
    indicatorSize: Dp,
    horizontalPadding: Dp,
    effects: com.styropyr0.prismal.PrismalGlassEffectProvider.() -> Unit,
    elevation: Dp,
    backingTintAlpha: Float = 0f,
    onNavigate: (String) -> Unit,
    onLongClick: (() -> Unit)? = null,
    onCapsuleBoundsChanged: ((Rect) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptics = rememberAppHaptics()

    val rawIndex = remember(currentRoute, items) {
        items.indexOfFirst { it.second == currentRoute }
    }
    // Keep the indicator parked on the last matching tab while no tab matches, but hide it
    var selectedIndex by remember { mutableStateOf(rawIndex.coerceAtLeast(0)) }
    if (rawIndex >= 0) selectedIndex = rawIndex
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (rawIndex >= 0) 1f else 0f,
        animationSpec = tween(200),
        label = "indicatorAlpha"
    )

    Box(
        modifier = modifier.padding(vertical = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .height(72.dp)
                .onGloballyPositioned { coords ->
                    onCapsuleBoundsChanged?.invoke(coords.boundsInWindow())
                }
                .shadow(elevation, CircleShape)
                .drawPrismalGlass(
                    backdrop = backdrop,
                    shape = { PrismalCapsule() },
                    effects = effects,
                    specular = KeepMeGlass.specular,
                    depthInset = KeepMeGlass.depthInset,
                    onDrawSurface = KeepMeGlass.surface
                )
                .then(
                    if (backingTintAlpha > 0f) {
                        Modifier.background(Color(0xFF0C101A).copy(alpha = backingTintAlpha), CircleShape)
                    } else Modifier
                )
                .padding(horizontal = horizontalPadding)
        ) {
        // Stretch and squeeze animated indicator
        val transition = androidx.compose.animation.core.updateTransition(
            targetState = selectedIndex,
            label = "indicatorTransition"
        )

        val indicatorOffsetPadding = (itemSize - indicatorSize) / 2

        val indicatorLeft by transition.animateDp(
            transitionSpec = {
                if (initialState < targetState) {
                    spring(dampingRatio = 0.8f, stiffness = 250f)
                } else {
                    spring(dampingRatio = 0.7f, stiffness = 1200f)
                }
            },
            label = "indicatorLeft"
        ) { index ->
            (itemSize * index) + indicatorOffsetPadding
        }

        val indicatorRight by transition.animateDp(
            transitionSpec = {
                if (initialState < targetState) {
                    spring(dampingRatio = 0.7f, stiffness = 1200f)
                } else {
                    spring(dampingRatio = 0.8f, stiffness = 250f)
                }
            },
            label = "indicatorRight"
        ) { index ->
            (itemSize * index) + indicatorOffsetPadding + indicatorSize
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .offset(x = indicatorLeft)
                .width(indicatorRight - indicatorLeft),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(indicatorSize)
                    .graphicsLayer { alpha = indicatorAlpha }
                    .clip(CircleShape)
                    .background(Color(0xFF1C1C1E).copy(alpha = 0.78f))
                    .border(0.75.dp, Color.White.copy(alpha = 0.16f), CircleShape)
            )
        }

        // The icons
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.second
                val interactionSource = remember { MutableInteractionSource() }

                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.2f else 1f,
                    animationSpec = TDriveMotion.tabIconPop,
                    label = "iconScale"
                )

                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.88f),
                    animationSpec = tween(TDriveMotion.chipSwitchMs),
                    label = "iconTint"
                )

                Box(
                    modifier = Modifier
                        .size(itemSize)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                if (currentRoute != item.second) {
                                    haptics.click()
                                }
                                onNavigate(item.second)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.third,
                        contentDescription = item.first,
                        tint = iconTint,
                        modifier = Modifier
                            .size(26.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                    )
                }
            }
        }
    }
}
}
