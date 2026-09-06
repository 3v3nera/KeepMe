package com.telegramdrive.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.drawPrismalGlass
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

@Composable
fun FloatingBottomBar(
    items: List<Triple<String, String, ImageVector>>, // Title, Route, Icon
    currentRoute: String?,
    backdrop: PrismalBackdrop,
    onNavigate: (String) -> Unit,
    onUploadClick: () -> Unit,
    selectionActions: SelectionBarActions? = null,
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

    // 2-Phase Choreography: Collapse into button -> Rotate button WHILE springing new capsule OUT together!
    LaunchedEffect(inSelectionMode) {
        if (inSelectionMode && displayedMode != BarDisplayMode.ACTIONS) {
            // ── Phase 1: Smoothly collapse the Nav capsule into the '+' button ──
            launch {
                capsuleAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                )
            }
            launch {
                capsuleScaleY.animateTo(
                    targetValue = 0.82f,
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                )
            }
            capsuleScaleX.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            )

            // ── Phase 2 & 3 TOGETHER: Rotate '+' to '✕' WHILE Action capsule springs OUT! ──
            displayedMode = BarDisplayMode.ACTIONS

            launch {
                fabPulse.animateTo(1.22f, tween(130, easing = FastOutSlowInEasing))
                fabPulse.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 160f))
            }
            launch {
                fabRotation.animateTo(
                    targetValue = 45f,
                    animationSpec = spring(dampingRatio = 0.58f, stiffness = 135f)
                )
            }
            launch {
                capsuleAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
                )
            }
            launch {
                capsuleScaleY.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.55f, stiffness = 135f)
                )
            }
            capsuleScaleX.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.52f, // True jelly bounce overshoot
                    stiffness = 125f     // Slower, relaxed pace so you can see the stretch
                )
            )
        } else if (!inSelectionMode && displayedMode != BarDisplayMode.NAV) {
            // ── Phase 1: Smoothly collapse the Action capsule into the '✕' button ──
            launch {
                capsuleAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                )
            }
            launch {
                capsuleScaleY.animateTo(
                    targetValue = 0.82f,
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                )
            }
            capsuleScaleX.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            )

            // ── Phase 2 & 3 TOGETHER: Rotate '✕' to '+' WHILE Nav capsule springs OUT! ──
            displayedMode = BarDisplayMode.NAV

            launch {
                fabPulse.animateTo(1.22f, tween(130, easing = FastOutSlowInEasing))
                fabPulse.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 160f))
            }
            launch {
                fabRotation.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.58f, stiffness = 135f)
                )
            }
            launch {
                capsuleAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
                )
            }
            launch {
                capsuleScaleY.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.55f, stiffness = 135f)
                )
            }
            capsuleScaleX.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.52f, // True jelly bounce overshoot
                    stiffness = 125f     // Slower, relaxed pace
                )
            )
        }
    }

    // Size configuration
    val itemSize = 64.dp
    val indicatorSize = 56.dp
    val horizontalPadding = 8.dp

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
                        horizontalPadding = horizontalPadding
                    )
                }
            } else {
                NavCapsule(
                    items = items,
                    currentRoute = currentRoute,
                    backdrop = backdrop,
                    itemSize = itemSize,
                    indicatorSize = indicatorSize,
                    horizontalPadding = horizontalPadding,
                    onNavigate = onNavigate
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

        val fabBackground by animateColorAsState(
            targetValue = if (inSelectionMode) Color(0xFFFF453A).copy(alpha = 0.18f) else Color.Transparent,
            animationSpec = tween(300),
            label = "fabBackground"
        )

        Box(
            modifier = Modifier
                .size(indicatorSize)
                .graphicsLayer {
                    scaleX = uploadScale * fabPulse.value
                    scaleY = uploadScale * fabPulse.value
                }
                .shadow(6.dp, CircleShape)
                .drawPrismalGlass(
                    backdrop = backdrop,
                    shape = { PrismalCapsule() },
                    effects = prismalGlassEffects(density)
                )
                .background(fabBackground, CircleShape)
                .clip(CircleShape)
                .clickable(
                    interactionSource = uploadInteractionSource,
                    indication = null,
                    onClick = {
                        haptics.click()
                        if (inSelectionMode && selectionActions != null) {
                            selectionActions.onClear()
                        } else {
                            onUploadClick()
                        }
                    }
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
}

@Composable
private fun SelectionActionCapsule(
    actions: SelectionBarActions,
    backdrop: PrismalBackdrop,
    itemSize: Dp,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptics = rememberAppHaptics()

    Box(
        modifier = modifier
            .padding(vertical = 24.dp)
            .height(72.dp)
            .shadow(8.dp, CircleShape)
            .drawPrismalGlass(
                backdrop = backdrop,
                shape = { PrismalCapsule() },
                effects = prismalGlassEffects(density)
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
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptics = rememberAppHaptics()

    val selectedIndex = remember(currentRoute, items) {
        items.indexOfFirst { it.second == currentRoute }.coerceAtLeast(0)
    }

    Box(
        modifier = modifier
            .padding(vertical = 24.dp)
            .height(72.dp)
            .shadow(8.dp, CircleShape)
            .drawPrismalGlass(
                backdrop = backdrop,
                shape = { PrismalCapsule() },
                effects = prismalGlassEffects(density)
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
                    .clip(CircleShape)
                    .background(Color(0xFF1C1C1E))
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
                    targetValue = if (isSelected) Color.White else Color(0xFF8E8E93),
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
