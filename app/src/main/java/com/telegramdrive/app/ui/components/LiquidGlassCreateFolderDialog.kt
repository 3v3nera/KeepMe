package com.telegramdrive.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import com.telegramdrive.app.ui.theme.TDriveColors
import com.telegramdrive.app.ui.utils.ExactContourEmojiSticker
import com.telegramdrive.app.ui.utils.FolderEmojiUtils
import com.telegramdrive.app.ui.utils.RecentEmojiManager
import com.telegramdrive.app.ui.utils.rememberAppHaptics

@Composable
fun LiquidGlassCreateFolderDialog(
    backdrop: com.styropyr0.prismal.PrismalBackdrop? = null,
    initialName: String = "",
    initialEmoji: String? = null,
    title: String = "Create Folder",
    subtitle: String = "Make a new private spot in your cloud for your files",
    confirmText: String = "Create",
    animateScrim: Boolean = true,
    onConfirm: (name: String, emoji: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptics = rememberAppHaptics()
    // A null backdrop (dialog shown inside the glass-source layer) renders the opaque panel
    backdrop?.readSamplingState()

    var folderName by remember { mutableStateOf(initialName) }
    // null represents the dice (random emoji)
    var selectedEmoji by remember { mutableStateOf<String?>(initialEmoji) }
    var showCustomEmojiDialog by remember { mutableStateOf(false) }
    val recentEmojis = remember { RecentEmojiManager.getRecentEmojis(context) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val isEnabled = folderName.trim().isNotBlank()
    val imeVisible = androidx.compose.foundation.layout.WindowInsets.Companion.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0

    val submit = {
        val trimmed = folderName.trim()
        if (trimmed.isNotBlank()) {
            haptics.success()
            val finalEmoji = selectedEmoji ?: (initialEmoji ?: FolderEmojiUtils.getRandomEmoji())
            RecentEmojiManager.addRecentEmoji(context, finalEmoji)
            onConfirm(trimmed, finalEmoji)
        }
    }

    LaunchedEffect(Unit) {
        // Let the entrance settle, then bring up the keyboard on the name field
        kotlinx.coroutines.delay(240)
        runCatching { focusRequester.requestFocus() }
    }

    GlassDialogHost(onDismiss = onDismiss, animateScrim = animateScrim) {
        Column(
            modifier = Modifier.fillMaxWidth(0.88f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassPanel(
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Collapses while the keyboard is open so the card always fits above it
                androidx.compose.animation.AnimatedVisibility(visible = !imeVisible) {
                Column {
                // ── Inset preview (mirrors the media slot in the reference card) ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .clip(RoundedCornerShape(GlassPanelTokens.InnerRadius))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.16f),
                                    Color.Black.copy(alpha = 0.22f)
                                )
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassChip {
                            Icon(
                                imageVector = Icons.Rounded.CloudDone,
                                contentDescription = null,
                                tint = TDriveColors.Secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            GlassChipText("Cloud Drive")
                        }
                        GlassChip { GlassChipText("Private") }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedEmoji != null) {
                            ExactContourEmojiSticker(
                                emoji = selectedEmoji!!,
                                size = 64.dp,
                                rotation = -4f
                            )
                        } else {
                            Text(text = "🎲", fontSize = 48.sp)
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))
                }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = GlassPanelTokens.TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = GlassPanelTokens.TextSecondary
                )

                Spacer(Modifier.height(22.dp))
                GlassSectionHeader("Folder name")
                Spacer(Modifier.height(14.dp))

                val inputBorder by animateColorAsState(
                    targetValue = if (isFocused) TDriveColors.Accent else Color.Transparent,
                    animationSpec = tween(150),
                    label = "inputBorder"
                )
                BasicTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = GlassPanelTokens.TextPrimary,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp
                    ),
                    cursorBrush = SolidColor(TDriveColors.Accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused },
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(CircleShape)
                                .background(GlassPanelTokens.PillFill)
                                .border(1.dp, inputBorder, CircleShape)
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                tint = GlassPanelTokens.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                if (folderName.isEmpty()) {
                                    Text(
                                        text = "Give your folder a name...",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                                        color = Color.White.copy(alpha = 0.55f)
                                    )
                                }
                                innerTextField()
                            }
                            if (folderName.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.14f))
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

                Spacer(Modifier.height(24.dp))
                GlassSectionHeader("Pick a sticker")
                Spacer(Modifier.height(14.dp))

                val stickerOptions = remember(initialEmoji, recentEmojis, selectedEmoji) {
                    val list = mutableListOf<String?>()
                    list.add(null) // dice option
                    val seen = mutableSetOf<String>()
                    if (selectedEmoji != null) {
                        list.add(selectedEmoji)
                        seen.add(selectedEmoji!!)
                    }
                    recentEmojis.forEach { em -> if (seen.add(em)) list.add(em) }
                    FolderEmojiUtils.STICKER_EMOJIS.forEach { em -> if (seen.add(em)) list.add(em) }
                    list
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(GlassPanelTokens.PillFill)
                                .clickable {
                                    haptics.tick()
                                    showCustomEmojiDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Pick any emoji",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    items(stickerOptions) { emoji ->
                        val isSelected = (emoji == selectedEmoji)
                        val chipScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.08f else 1f,
                            animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                            label = "chipScale"
                        )
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .graphicsLayer {
                                    scaleX = chipScale
                                    scaleY = chipScale
                                }
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) TDriveColors.Accent.copy(alpha = 0.18f) else GlassPanelTokens.PillFill
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) TDriveColors.Accent else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    haptics.tick()
                                    selectedEmoji = emoji
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (emoji == null) {
                                Text(text = "🎲", fontSize = 20.sp)
                            } else {
                                ExactContourEmojiSticker(
                                    emoji = emoji,
                                    size = 30.dp,
                                    rotation = if (isSelected) -4f else 0f
                                )
                            }
                        }
                    }
                }
            }

            // ── Actions float below the card, like the reference's "Book" pill ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassSecondaryButton(
                    text = "Cancel",
                    onClick = {
                        haptics.click()
                        onDismiss()
                    }
                )
                GlassPrimaryButton(
                    text = confirmText,
                    enabled = isEnabled,
                    onClick = submit
                )
            }
        }

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
