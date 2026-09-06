package com.telegramdrive.app.ui.components

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.prismalGlassEffects
import com.styropyr0.prismal.shapes.PrismalRoundedRectangle
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import com.telegramdrive.app.domain.model.TelegramFolder
import com.telegramdrive.app.ui.theme.TDriveColors
import com.telegramdrive.app.ui.utils.ExactContourEmojiSticker
import com.telegramdrive.app.ui.utils.FolderEmojiUtils
import com.telegramdrive.app.ui.utils.rememberAppHaptics
import kotlinx.coroutines.launch

private enum class FolderOptionsStep {
    MENU,
    DELETE_CONFIRM,
    RENAME
}

/**
 * Liquid Glass Folder Options System:
 * Designed to 100% match the exact visual language of LiquidGlassCreateFolderDialog:
 * - Outer 28.dp dual-frosted obsidian glass shell (fillMaxWidth 0.90f)
 * - Elevated top Bento hero section with 3D squircle badge + micro pill tag
 * - Inset middle modules with dark translucent fill (40-50% alpha)
 * - 48.dp tactile pill action buttons with specular edge highlights
 */
@Composable
fun LiquidGlassFolderOptionsDialog(
    folder: TelegramFolder,
    backdrop: com.styropyr0.prismal.PrismalBackdrop? = null,
    onRename: (newName: String, newEmoji: String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val haptics = rememberAppHaptics()
    val fallbackBackdrop = rememberPrismalGlassLayer()
    val effectiveBackdrop = backdrop ?: fallbackBackdrop
    effectiveBackdrop.readSamplingState()

    var currentStep by remember { mutableStateOf(FolderOptionsStep.MENU) }

    // Clean folder display name (strip [TG] and [TD] tags)
    val rawName = remember(folder.name) {
        folder.displayName
    }
    val currentEmoji = remember(folder) {
        folder.emoji ?: FolderEmojiUtils.getDeterministicEmoji(folder.id)
    }

    when (currentStep) {
        FolderOptionsStep.MENU -> {
            FolderOptionsMenuPopup(
                folderName = rawName,
                emoji = currentEmoji,
                isSavedMessages = folder.isSavedMessages,
                backdrop = effectiveBackdrop,
                onRenameClick = {
                    haptics.click()
                    currentStep = FolderOptionsStep.RENAME
                },
                onDeleteClick = {
                    haptics.click()
                    currentStep = FolderOptionsStep.DELETE_CONFIRM
                },
                onDismiss = onDismiss
            )
        }

        FolderOptionsStep.DELETE_CONFIRM -> {
            FolderDeleteConfirmationPopup(
                folderName = rawName,
                emoji = currentEmoji,
                backdrop = effectiveBackdrop,
                onConfirmSure = {
                    haptics.error()
                    onDelete()
                    onDismiss()
                },
                onCancelNo = {
                    haptics.click()
                    onDismiss()
                },
                onDismiss = onDismiss
            )
        }

        FolderOptionsStep.RENAME -> {
            LiquidGlassCreateFolderDialog(
                backdrop = effectiveBackdrop,
                initialName = rawName,
                initialEmoji = currentEmoji,
                title = "Edit",
                subtitle = "Change the name or sticker",
                confirmText = "Rename",
                onConfirm = { newName, newEmoji ->
                    onRename(newName, newEmoji)
                    onDismiss()
                },
                onDismiss = onDismiss
            )
        }
    }
}

/**
 * Folder Options Menu Dialog:
 * Styled identically to LiquidGlassCreateFolderDialog (Image 1)
 */
@Composable
private fun FolderOptionsMenuPopup(
    folderName: String,
    emoji: String,
    isSavedMessages: Boolean,
    backdrop: com.styropyr0.prismal.PrismalBackdrop,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val haptics = rememberAppHaptics()
    val scrimAlpha = remember { Animatable(0f) }
    val cardScale = remember { Animatable(0.92f) }
    val cardAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { scrimAlpha.animateTo(1f, tween(200)) }
        launch {
            cardScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch { cardAlpha.animateTo(1f, tween(160)) }
    }

    androidx.activity.compose.BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        // High-contrast dimmed scrim (tapping dismisses)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { this.alpha = scrimAlpha.value }
                .background(Color(0xFF01040A).copy(alpha = 0.60f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        // ── Outer Obsidian Frosted Glass Bento Shell (Matching Image 1) ──
        Column(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .graphicsLayer {
                    scaleX = cardScale.value
                    scaleY = cardScale.value
                    alpha = cardAlpha.value
                }
                .shadow(
                    elevation = 32.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = Color.Black.copy(alpha = 0.70f),
                    ambientColor = Color.Black.copy(alpha = 0.35f)
                )
                .drawPrismalGlass(
                    backdrop = backdrop,
                    shape = { PrismalRoundedRectangle(28.dp) },
                    effects = prismalGlassEffects(density)
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F141E).copy(alpha = 0.75f),
                            Color(0xFF05080E).copy(alpha = 0.85f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .clip(RoundedCornerShape(28.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Top Bento Hero Card ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.16f),
                                Color.White.copy(alpha = 0.06f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.36f),
                                Color.White.copy(alpha = 0.12f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                // Top Row: Tactile 3D Squircle Emblem + Micro-Badge Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tactile 3D Squircle Icon Badge
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(14.dp),
                                spotColor = Color(0xFF6C8FFF).copy(alpha = 0.30f),
                                ambientColor = Color.Black.copy(alpha = 0.40f)
                            )
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF1E293B).copy(alpha = 0.90f),
                                        Color(0xFF0F172A).copy(alpha = 0.95f)
                                    )
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.40f),
                                        Color.White.copy(alpha = 0.12f)
                                    )
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = TDriveColors.Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Micro-Badge Pill (Sticker / Folder indicator)
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.25f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.16f),
                                shape = CircleShape
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        ExactContourEmojiSticker(
                            emoji = emoji,
                            size = 18.dp,
                            rotation = 0f
                        )
                        Text(
                            text = if (isSavedMessages) "Root Cloud" else "Cloud Folder",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.2.sp
                            ),
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title
                Text(
                    text = folderName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Subtitle
                Text(
                    text = if (isSavedMessages) "Saved Messages root cloud storage"
                           else "Rename, tweak the sticker, or manage this folder",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    ),
                    color = Color(0xFFCBD5E1)
                )
            }

            // ── Inset Options Module (Matching Image 1 middle modules) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.40f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Option 1: Rename Folder
                BentoOptionItem(
                    icon = Icons.Rounded.DriveFileRenameOutline,
                    iconTint = Color(0xFF6C8FFF),
                    iconBg = Brush.linearGradient(
                        listOf(
                            Color(0xFF3863E5).copy(alpha = 0.35f),
                            Color(0xFF1E3A8A).copy(alpha = 0.45f)
                        )
                    ),
                    iconBorder = Color(0xFF6C8FFF).copy(alpha = 0.45f),
                    title = "Rename",
                    subtitle = "Change the name or sticker",
                    textColor = Color.White,
                    onClick = onRenameClick
                )

                // Option 2: Delete Folder (if !isSavedMessages)
                if (!isSavedMessages) {
                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.05f))
                    )

                    BentoOptionItem(
                        icon = Icons.Rounded.DeleteOutline,
                        iconTint = Color(0xFFF87171),
                        iconBg = Brush.linearGradient(
                            listOf(
                                Color(0xFFEF4444).copy(alpha = 0.25f),
                                Color(0xFF991B1B).copy(alpha = 0.35f)
                            )
                        ),
                        iconBorder = Color(0xFFEF4444).copy(alpha = 0.45f),
                        title = "Delete Folder",
                        subtitle = "Remove this channel folder from KeepMe",
                        textColor = Color(0xFFFCA5A5),
                        onClick = onDeleteClick
                    )
                }
            }

            // ── Bottom Pill Action Button (Matching Image 1) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = CircleShape
                    )
                    .clickable {
                        haptics.click()
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Close",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFCBD5E1)
                )
            }
        }
    }
}

/**
 * Delete Confirmation Dialog:
 * Styled identically to LiquidGlassCreateFolderDialog (Image 1)
 */
@Composable
private fun FolderDeleteConfirmationPopup(
    folderName: String,
    emoji: String,
    backdrop: com.styropyr0.prismal.PrismalBackdrop,
    onConfirmSure: () -> Unit,
    onCancelNo: () -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val haptics = rememberAppHaptics()
    val scrimAlpha = remember { Animatable(0f) }
    val cardScale = remember { Animatable(0.92f) }
    val cardAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { scrimAlpha.animateTo(1f, tween(200)) }
        launch {
            cardScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch { cardAlpha.animateTo(1f, tween(160)) }
    }

    androidx.activity.compose.BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        // High-contrast dimmed scrim (tapping dismisses)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { this.alpha = scrimAlpha.value }
                .background(Color(0xFF01040A).copy(alpha = 0.60f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        // ── Outer Obsidian Frosted Glass Bento Shell (Matching Image 1) ──
        Column(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .graphicsLayer {
                    scaleX = cardScale.value
                    scaleY = cardScale.value
                    alpha = cardAlpha.value
                }
                .shadow(
                    elevation = 32.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = Color.Black.copy(alpha = 0.70f),
                    ambientColor = Color.Black.copy(alpha = 0.35f)
                )
                .drawPrismalGlass(
                    backdrop = backdrop,
                    shape = { PrismalRoundedRectangle(28.dp) },
                    effects = prismalGlassEffects(density)
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F141E).copy(alpha = 0.75f),
                            Color(0xFF05080E).copy(alpha = 0.85f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .clip(RoundedCornerShape(28.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Top Bento Hero Card (Matching Image 1) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.16f),
                                Color.White.copy(alpha = 0.06f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.36f),
                                Color.White.copy(alpha = 0.12f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                // Top Row: 3D Squircle Emblem + Micro-Badge Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tactile 3D Squircle Icon Badge with Crimson Accent
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(14.dp),
                                spotColor = Color(0xFFEF4444).copy(alpha = 0.40f),
                                ambientColor = Color.Black.copy(alpha = 0.40f)
                            )
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF7F1D1D).copy(alpha = 0.70f),
                                        Color(0xFF450A0A).copy(alpha = 0.80f)
                                    )
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFCA5A5).copy(alpha = 0.60f),
                                        Color(0xFFEF4444).copy(alpha = 0.20f)
                                    )
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = Color(0xFFFECACA),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Micro-Badge Pill with Sticker
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.25f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.16f),
                                shape = CircleShape
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ExactContourEmojiSticker(
                            emoji = emoji,
                            size = 18.dp,
                            rotation = 0f
                        )
                        Text(
                            text = "Folder",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.2.sp
                            ),
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title
                Text(
                    text = "Delete this folder?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Subtitle
                Text(
                    text = "Are you sure you want to delete '$folderName'?",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    ),
                    color = Color(0xFFCBD5E1)
                )
            }

            // ── Inset Info Notice Module (Matching Image 1 middle modules) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.40f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = "Files inside will stay safe in Telegram, but this folder channel will be permanently removed from KeepMe.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    color = Color(0xFF94A3B8)
                )
            }

            // ── Pill Action Buttons: "No" & "Sure" (Matching Image 1) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // "No" Button (Identical to "Cancel" in Image 1)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.12f),
                            shape = CircleShape
                        )
                        .clickable {
                            haptics.click()
                            onCancelNo()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No, Keep It",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFCBD5E1)
                    )
                }

                // "Sure" Button (Crimson Radiant Pill with Specular Top Border)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .shadow(
                            elevation = 10.dp,
                            shape = CircleShape,
                            spotColor = Color(0xFFEF4444).copy(alpha = 0.50f)
                        )
                        .clip(CircleShape)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFF4D4D),
                                    Color(0xFFE11D48)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.60f),
                                    Color.White.copy(alpha = 0.15f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .clickable {
                            haptics.error()
                            onConfirmSure()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Delete Folder",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Bento Option Item for Menu:
 * Generous, tactile row with 3D squircle badge, title, subtitle, and chevron.
 */
@Composable
private fun BentoOptionItem(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Brush,
    iconBorder: Color,
    title: String,
    subtitle: String,
    textColor: Color,
    onClick: () -> Unit
) {
    val haptics = rememberAppHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "bentoScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isPressed) Color.White.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.click()
                    onClick()
                }
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 3D Squircle Icon Badge
        Box(
            modifier = Modifier
                .size(38.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = iconTint.copy(alpha = 0.35f)
                )
                .background(iconBg, RoundedCornerShape(12.dp))
                .border(1.dp, iconBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        // Title & subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = textColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color(0xFF64748B)
            )
        }

        // Subtle Right Chevron
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.25f),
            modifier = Modifier.size(18.dp)
        )
    }
}
