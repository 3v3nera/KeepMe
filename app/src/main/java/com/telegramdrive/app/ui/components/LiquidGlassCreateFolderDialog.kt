package com.telegramdrive.app.ui.components

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.prismalGlassEffects
import com.styropyr0.prismal.shapes.PrismalRoundedRectangle
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import com.telegramdrive.app.ui.theme.TDriveColors
import com.telegramdrive.app.ui.utils.ExactContourEmojiSticker
import com.telegramdrive.app.ui.utils.FolderEmojiUtils
import com.telegramdrive.app.ui.utils.RecentEmojiManager
import com.telegramdrive.app.ui.utils.rememberAppHaptics
import kotlinx.coroutines.launch

@Composable
fun LiquidGlassCreateFolderDialog(
    backdrop: com.styropyr0.prismal.PrismalBackdrop? = null,
    initialName: String = "",
    initialEmoji: String? = null,
    title: String = "Create Folder",
    subtitle: String = "Make a new private spot in your cloud for your files",
    confirmText: String = "Create",
    onConfirm: (name: String, emoji: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptics = rememberAppHaptics()
    val fallbackBackdrop = rememberPrismalGlassLayer()
    val effectiveBackdrop = backdrop ?: fallbackBackdrop
    effectiveBackdrop.readSamplingState()

    var folderName by remember { mutableStateOf(initialName) }
    // Preselected option: initialEmoji if provided, otherwise null represents the Dice (Random emoji)
    var selectedEmoji by remember { mutableStateOf<String?>(initialEmoji) }
    var showCustomEmojiDialog by remember { mutableStateOf(false) }
    val recentEmojis = remember { RecentEmojiManager.getRecentEmojis(context) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val scrimAlpha = remember { Animatable(0f) }
    val cardScale = remember { Animatable(0.92f) }
    val cardAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            scrimAlpha.animateTo(1f, tween(200))
        }
        launch {
            cardScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            cardAlpha.animateTo(1f, tween(160))
        }
        kotlinx.coroutines.delay(120)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
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

        // ── Outer Obsidian Frosted Glass Bento Shell ──
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
                    backdrop = effectiveBackdrop,
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
                    onClick = {} // block dismissal when clicking inside
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
                // Top Row: Tactile 3D Squircle Emblem + Cloud Pill Tag
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
                            imageVector = if (confirmText == "Rename" || title == "Edit") Icons.Rounded.DriveFileRenameOutline else Icons.Rounded.CreateNewFolder,
                            contentDescription = null,
                            tint = TDriveColors.Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Micro-Badge Pill
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
                        Icon(
                            imageVector = Icons.Rounded.CloudDone,
                            contentDescription = null,
                            tint = TDriveColors.Secondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Cloud Drive",
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
                    text = title,
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
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    ),
                    color = Color(0xFFCBD5E1)
                )
            }

            // ── Inset Input Module Card ──
            val inputBorderColor by animateColorAsState(
                targetValue = when {
                    isFocused -> TDriveColors.Primary.copy(alpha = 0.70f)
                    folderName.isNotBlank() -> Color.White.copy(alpha = 0.35f)
                    else -> Color.White.copy(alpha = 0.09f)
                },
                animationSpec = tween(150),
                label = "inputBorder"
            )

            BasicTextField(
                value = folderName,
                onValueChange = { folderName = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                ),
                cursorBrush = SolidColor(TDriveColors.Primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused },
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.50f))
                            .border(
                                width = 1.dp,
                                color = inputBorderColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = if (isFocused || folderName.isNotBlank()) TDriveColors.Primary else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            if (folderName.isEmpty()) {
                                Text(
                                    text = "Give your folder a name...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 15.sp
                                    ),
                                    color = Color(0xFF64748B)
                                )
                            }
                            innerTextField()
                        }

                        if (folderName.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .clickable {
                                        haptics.tick()
                                        folderName = ""
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            )

            // ── Sticker Emoji Selector Module ──
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
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pick a sticker",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            letterSpacing = 0.2.sp
                        ),
                        color = Color(0xFFCBD5E1)
                    )

                    // Active badge
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (selectedEmoji != null) {
                            ExactContourEmojiSticker(
                                emoji = selectedEmoji!!,
                                size = 18.dp,
                                rotation = 0f
                            )
                            Text(
                                text = "Sticker",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "🎲 Surprise me",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color.White
                            )
                        }
                    }
                }

                // Horizontal Row of Sticker Emojis (Custom +, Dice/Random, Recent & Presets)
                val stickerOptions = remember(initialEmoji, recentEmojis, selectedEmoji) {
                    val list = mutableListOf<String?>()
                    list.add(null) // dice option
                    val seen = mutableSetOf<String>()
                    if (selectedEmoji != null) {
                        list.add(selectedEmoji)
                        seen.add(selectedEmoji!!)
                    }
                    recentEmojis.forEach { em ->
                        if (seen.add(em)) {
                            list.add(em)
                        }
                    }
                    FolderEmojiUtils.STICKER_EMOJIS.forEach { em ->
                        if (seen.add(em)) {
                            list.add(em)
                        }
                    }
                    list
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    // Item 1: Custom Emoji Picker / Keyboard button (+)
                    item {
                        val customShape = RoundedCornerShape(12.dp)
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(customShape)
                                .background(
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF6C8FFF).copy(alpha = 0.22f),
                                            Color(0xFF6C8FFF).copy(alpha = 0.08f)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF6C8FFF).copy(alpha = 0.45f),
                                    shape = customShape
                                )
                                .clickable {
                                    haptics.tick()
                                    showCustomEmojiDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Pick any emoji",
                                tint = Color(0xFF6C8FFF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Items: Dice, Recent & Presets with Exact Die-Cut Vinyl Contour
                    items(stickerOptions) { emoji ->
                        val isSelected = (emoji == selectedEmoji)
                        val chipScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.10f else 1f,
                            animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                            label = "chipScale"
                        )

                        val chipShape = RoundedCornerShape(12.dp)
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .graphicsLayer {
                                    scaleX = chipScale
                                    scaleY = chipScale
                                }
                                .shadow(
                                    elevation = if (isSelected) 6.dp else 0.dp,
                                    shape = chipShape,
                                    spotColor = Color(0xFF6C8FFF).copy(alpha = 0.40f)
                                )
                                .clip(chipShape)
                                .background(
                                    if (isSelected) Color(0xFF1E293B).copy(alpha = 0.90f) else Color.White.copy(alpha = 0.07f)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF6C8FFF) else Color.White.copy(alpha = 0.12f),
                                    shape = chipShape
                                )
                                .clickable {
                                    haptics.tick()
                                    selectedEmoji = emoji
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (emoji == null) {
                                Text(
                                    text = "🎲",
                                    fontSize = 19.sp
                                )
                            } else {
                                ExactContourEmojiSticker(
                                    emoji = emoji,
                                    size = 32.dp,
                                    rotation = if (isSelected) -4f else 0f
                                )
                            }
                        }
                    }
                }
            }

            // ── Pill Action Buttons ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Cancel Button
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
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFCBD5E1)
                    )
                }

                // Confirm Button
                val isEnabled = folderName.trim().isNotBlank()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .shadow(
                            elevation = if (isEnabled) 10.dp else 0.dp,
                            shape = CircleShape,
                            spotColor = Color.Black.copy(alpha = 0.40f)
                        )
                        .clip(CircleShape)
                        .background(
                            if (isEnabled) Color.White else Color.White.copy(alpha = 0.06f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isEnabled) Color.White else Color.White.copy(alpha = 0.10f),
                            shape = CircleShape
                        )
                        .clickable(enabled = isEnabled) {
                            val trimmed = folderName.trim()
                            if (trimmed.isNotBlank()) {
                                haptics.success()
                                val finalEmoji = selectedEmoji ?: (initialEmoji ?: FolderEmojiUtils.getRandomEmoji())
                                RecentEmojiManager.addRecentEmoji(context, finalEmoji)
                                onConfirm(trimmed, finalEmoji)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = confirmText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) Color(0xFF0F172A) else Color.White.copy(alpha = 0.30f)
                    )
                }
            }
        }

        // Custom Emoji Picker Dialog Popup
        if (showCustomEmojiDialog) {
            CustomEmojiDialog(
                backdrop = backdrop,
                onEmojiSelected = { customEmoji ->
                    selectedEmoji = customEmoji
                    RecentEmojiManager.addRecentEmoji(context, customEmoji)
                },
                onDismiss = { showCustomEmojiDialog = false }
            )
        }
    }
}

/**
 * Overload for backward compatibility
 */
@Composable
fun LiquidGlassCreateFolderDialog(
    backdrop: com.styropyr0.prismal.PrismalBackdrop? = null,
    initialName: String = "",
    title: String = "Create Folder",
    subtitle: String = "Make a new private spot in your cloud for your files",
    confirmText: String = "Create",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    LiquidGlassCreateFolderDialog(
        backdrop = backdrop,
        initialName = initialName,
        title = title,
        subtitle = subtitle,
        confirmText = confirmText,
        onConfirm = { name, _ -> onConfirm(name) },
        onDismiss = onDismiss
    )
}
