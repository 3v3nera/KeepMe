package com.telegramdrive.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.depth.PrismalDepthShadow
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.effects.applyPrismalGlassEffects
import com.styropyr0.prismal.shapes.PrismalRoundedRectangle
import com.styropyr0.prismal.specular.PrismalSpecular
import com.styropyr0.prismal.specular.PrismalSpecularStyle
import kotlinx.coroutines.launch

/**
 * KeepMe popup material: one large, heavily blurred, smoky glass card with a soft
 * light-to-dark vertical gradient, a barely-there rim and inset dark pill controls.
 * The nav bar / FAB keep their own [KeepMeGlass] recipe.
 */
object GlassPanelTokens {
    val PanelRadius: Dp = 40.dp
    val InnerRadius: Dp = 28.dp
    val PanelPadding: Dp = 24.dp

    val TextPrimary = Color.White.copy(alpha = 0.94f)
    val TextSecondary = Color.White.copy(alpha = 0.74f)
    val Hairline = Color.White.copy(alpha = 0.22f)
    val PillFill = Color.Black.copy(alpha = 0.26f)

    internal val specular: () -> PrismalSpecular? = {
        PrismalSpecular(
            width = 0.8f.dp,
            blurRadius = 0.6f.dp,
            alpha = 0.55f,
            style = PrismalSpecularStyle.Default(
                color = Color.White.copy(alpha = 0.45f),
                angle = 45f,
                falloff = 1.6f
            )
        )
    }

    internal val depthShadow: () -> PrismalDepthShadow? = {
        PrismalDepthShadow(radius = 32.dp, color = Color.Black.copy(alpha = 0.28f))
    }

    internal val surface: DrawScope.() -> Unit = {
        // Smoky base plus lighter top / darker bottom, like the reference card
        drawRect(Color.White.copy(alpha = 0.05f))
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.10f),
                    Color.White.copy(alpha = 0.02f)
                )
            )
        )
    }
}

@Composable
fun GlassPanel(
    backdrop: PrismalBackdrop?,
    modifier: Modifier = Modifier,
    showHandle: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val density = LocalDensity.current
    val effects: com.styropyr0.prismal.PrismalGlassEffectProvider.() -> Unit = remember(density) {
        {
            applyPrismalGlassEffects(
                density = density,
                adaptiveLuminance = false,
                luminance = 0.5f,
                blurRadiusPx = with(density) { 28.dp.toPx() },
                refractionHeightPx = with(density) { 12.dp.toPx() },
                refractionAmountPx = with(density) { 16.dp.toPx() },
                saturation = 1.1f,
                useVibrancy = false
            )
        }
    }

    // Popups hosted inside the glass-source layer (or in their own window) cannot sample it
    // without recursing, so they pass a null backdrop and get an opaque smoky look instead.
    val surfaceModifier = if (backdrop != null) {
        Modifier.drawPrismalGlass(
            backdrop = backdrop,
            shape = { PrismalRoundedRectangle(GlassPanelTokens.PanelRadius) },
            effects = effects,
            specular = GlassPanelTokens.specular,
            depthShadow = GlassPanelTokens.depthShadow,
            onDrawSurface = GlassPanelTokens.surface
        )
    } else {
        Modifier
            .clip(RoundedCornerShape(GlassPanelTokens.PanelRadius))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF23262C).copy(alpha = 0.98f), Color(0xFF16181C).copy(alpha = 0.99f))
                )
            )
            .border(0.75.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(GlassPanelTokens.PanelRadius))
    }

    Column(
        modifier = modifier
            .then(surfaceModifier)
            .clip(RoundedCornerShape(GlassPanelTokens.PanelRadius))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {} // swallow taps so they don't dismiss the dialog
            )
            .padding(horizontal = GlassPanelTokens.PanelPadding)
            .padding(top = if (showHandle) 10.dp else GlassPanelTokens.PanelPadding, bottom = GlassPanelTokens.PanelPadding),
        horizontalAlignment = Alignment.Start
    ) {
        if (showHandle) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 14.dp)
                    .width(36.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f))
            )
        }
        content()
    }
}

/**
 * Shared popup shell: dimmed tap-to-dismiss scrim, centered content, spring entrance,
 * IME awareness and back handling.
 */
@Composable
fun GlassDialogHost(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    scrimStrength: Float = 0.42f,
    /** False when swapping between popups so the dim layer doesn't fade out and back in. */
    animateScrim: Boolean = true,
    content: @Composable () -> Unit
) {
    val scrimAlpha = remember { Animatable(if (animateScrim) 0f else 1f) }
    val cardScale = remember { Animatable(0.94f) }
    val cardAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (animateScrim) launch { scrimAlpha.animateTo(1f, tween(200)) }
        launch {
            cardScale.animateTo(
                1f,
                spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
            )
        }
        launch { cardAlpha.animateTo(1f, tween(160)) }
    }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = scrimAlpha.value }
                .background(Color(0xFF01040A).copy(alpha = scrimStrength))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = cardScale.value
                scaleY = cardScale.value
                alpha = cardAlpha.value
            }
        ) {
            content()
        }
    }
}

@Composable
fun GlassSectionHeader(text: String, modifier: Modifier = Modifier) {
    // Small caption label (not a tappable-looking heading, no divider)
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            letterSpacing = 1.2.sp
        ),
        color = GlassPanelTokens.TextSecondary,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun GlassChip(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(GlassPanelTokens.PillFill)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}

@Composable
fun GlassChipText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp
        ),
        color = GlassPanelTokens.TextPrimary
    )
}

/** Solid white pill with dark text — the single primary action, placed below the card. */
@Composable
fun GlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    loading: Boolean = false,
    /** true when the button sits directly on the light canvas (login screens): dark ink pill. */
    onLightCanvas: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "primaryScale"
    )
    val fill by animateColorAsState(
        targetValue = when {
            !enabled && !loading -> Color.White.copy(alpha = 0.16f)
            destructive -> Color(0xFFFF453A)
            else -> Color.White
        },
        animationSpec = tween(150),
        label = "primaryFill"
    )
    Box(
        modifier = modifier
            .height(52.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(fill)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !loading,
                onClick = onClick
            )
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp,
                color = if (destructive) Color.White else Color(0xFF0B0F1A)
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = when {
                    !enabled -> Color.White.copy(alpha = 0.4f)
                    destructive -> Color.White
                    else -> Color(0xFF0B0F1A)
                }
            )
        }
    }
}

/** Dark translucent pill for secondary actions such as Cancel. */
@Composable
fun GlassSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "secondaryScale"
    )
    Box(
        modifier = modifier
            .height(52.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(Color(0xFF12151C).copy(alpha = 0.82f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            ),
            color = GlassPanelTokens.TextPrimary
        )
    }
}

/** Inset rounded slot at the top of a panel (the "media" area in the reference), with chips overlaid. */
@Composable
fun GlassHeroSlot(
    modifier: Modifier = Modifier,
    height: Dp = 132.dp,
    tint: Color = Color.White,
    chips: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(GlassPanelTokens.InnerRadius))
            .background(
                Brush.verticalGradient(
                    listOf(tint.copy(alpha = 0.18f), Color.Black.copy(alpha = 0.22f))
                )
            )
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = chips
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

/** Dark pill row used for menu actions: leading icon, title, optional chevron. */
@Composable
fun GlassActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    tint: Color = Color.White,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "actionRowScale"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(GlassPanelTokens.InnerRadius))
            .background(if (pressed) Color.Black.copy(alpha = 0.40f) else GlassPanelTokens.PillFill)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal, fontSize = 16.sp),
                color = tint
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = GlassPanelTokens.TextSecondary
                )
            }
        }
    }
}
/**
 * Glass alert in its own window, safe to call from any screen. [body] adds content inside the
 * card (e.g. action rows); [confirmText] = null leaves only the secondary pill below the card.
 */
@Composable
fun GlassAlertDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    confirmText: String? = null,
    onConfirm: () -> Unit = {},
    destructive: Boolean = false,
    dismissText: String = "Cancel",
    heroIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    heroTint: Color = Color.White,
    body: (@Composable ColumnScope.() -> Unit)? = null
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        GlassDialogHost(onDismiss = onDismiss, scrimStrength = 0.74f) {
            Column(
                modifier = Modifier.fillMaxWidth(0.88f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassPanel(backdrop = null, modifier = Modifier.fillMaxWidth()) {
                    if (heroIcon != null) {
                        GlassHeroSlot(height = 108.dp, tint = heroTint) {
                            androidx.compose.material3.Icon(
                                imageVector = heroIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 22.sp,
                            letterSpacing = (-0.3).sp
                        ),
                        color = GlassPanelTokens.TextPrimary,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
                        color = GlassPanelTokens.TextSecondary
                    )
                    if (body != null) {
                        Spacer(Modifier.height(20.dp))
                        body()
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassSecondaryButton(text = dismissText, onClick = onDismiss)
                    if (confirmText != null) {
                        GlassPrimaryButton(text = confirmText, destructive = destructive, onClick = onConfirm)
                    }
                }
            }
        }
    }
}
/** Dark inset block for grouped info inside a panel. */
@Composable
fun GlassInfoBlock(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassPanelTokens.InnerRadius))
            .background(GlassPanelTokens.PillFill)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}

/**
 * Glass sheet in its own window (replaces ModalBottomSheet): titled card with scrollable [content]
 * and a Done pill below. [actions] adds extra pills next to it.
 */
@Composable
fun GlassSheetDialog(
    title: String,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    doneText: String = "Done",
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        GlassDialogHost(onDismiss = onDismiss, scrimStrength = 0.74f) {
            Column(
                modifier = Modifier.fillMaxWidth(0.88f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassPanel(backdrop = null, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 22.sp,
                            letterSpacing = (-0.3).sp
                        ),
                        color = GlassPanelTokens.TextPrimary
                    )
                    if (subtitle != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                            color = GlassPanelTokens.TextSecondary
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(GlassPanelTokens.Hairline))
                    Spacer(Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .androidx_heightIn(max = 460.dp)
                            .androidx_verticalScroll(androidx.compose.foundation.rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        content = content
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassSecondaryButton(text = doneText, onClick = onDismiss)
                    actions()
                }
            }
        }
    }
}

private fun Modifier.androidx_heightIn(max: Dp): Modifier =
    this.then(Modifier.heightIn(max = max))

private fun Modifier.androidx_verticalScroll(state: androidx.compose.foundation.ScrollState): Modifier =
    this.then(Modifier.verticalScroll(state))