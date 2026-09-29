package com.telegramdrive.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import com.telegramdrive.app.domain.model.TelegramFolder
import com.telegramdrive.app.ui.theme.TDriveColors
import com.telegramdrive.app.ui.utils.ExactContourEmojiSticker
import com.telegramdrive.app.ui.utils.FolderEmojiUtils
import com.telegramdrive.app.ui.utils.rememberAppHaptics

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
    val haptics = rememberAppHaptics()
    // Popups always use the dark smoked panel (no live glass sampling)
    val effectiveBackdrop: com.styropyr0.prismal.PrismalBackdrop? = null

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
                animateScrim = false,
                onConfirm = { newName, newEmoji ->
                    onRename(newName, newEmoji)
                    onDismiss()
                },
                onDismiss = onDismiss
            )
        }
    }
}


/** Folder options: a glass card with the folder preview and actions, Close floats below. */
@Composable
private fun FolderOptionsMenuPopup(
    folderName: String,
    emoji: String,
    isSavedMessages: Boolean,
    backdrop: com.styropyr0.prismal.PrismalBackdrop?,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = rememberAppHaptics()
    GlassDialogHost(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth(0.88f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassPanel(backdrop = backdrop, modifier = Modifier.fillMaxWidth()) {
                GlassHeroSlot(
                    chips = {
                        GlassChip {
                            Icon(
                                imageVector = Icons.Rounded.CloudDone,
                                contentDescription = null,
                                tint = TDriveColors.Secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            GlassChipText(if (isSavedMessages) "Root Cloud" else "Cloud Folder")
                        }
                    }
                ) {
                    ExactContourEmojiSticker(emoji = emoji, size = 64.dp, rotation = -4f)
                }

                Spacer(Modifier.height(22.dp))
                Text(
                    text = folderName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = GlassPanelTokens.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isSavedMessages) "Saved Messages root cloud storage"
                    else "Rename, tweak the sticker, or manage this folder",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                    color = GlassPanelTokens.TextSecondary
                )

                Spacer(Modifier.height(22.dp))
                GlassSectionHeader("Actions")
                Spacer(Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassActionRow(
                        icon = Icons.Rounded.DriveFileRenameOutline,
                        title = "Rename",
                        subtitle = "Change the name or sticker",
                        onClick = { onRenameClick() }
                    )
                    if (!isSavedMessages) {
                        GlassActionRow(
                            icon = Icons.Rounded.DeleteOutline,
                            title = "Delete Folder",
                            subtitle = "Remove this channel folder from KeepMe",
                            tint = Color(0xFFFF8A80),
                            onClick = { onDeleteClick() }
                        )
                    }
                }
            }

            GlassSecondaryButton(
                text = "Close",
                onClick = {
                    haptics.click()
                    onDismiss()
                }
            )
        }
    }
}

/** Delete confirmation: red-tinted preview, notice, and a destructive primary pill below the card. */
@Composable
private fun FolderDeleteConfirmationPopup(
    folderName: String,
    emoji: String,
    backdrop: com.styropyr0.prismal.PrismalBackdrop?,
    onConfirmSure: () -> Unit,
    onCancelNo: () -> Unit,
    onDismiss: () -> Unit
) {
    GlassDialogHost(onDismiss = onDismiss, animateScrim = false) {
        Column(
            modifier = Modifier.fillMaxWidth(0.88f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassPanel(backdrop = backdrop, modifier = Modifier.fillMaxWidth()) {
                GlassHeroSlot(
                    tint = Color(0xFFFF453A),
                    chips = { GlassChip { GlassChipText("Folder") } }
                ) {
                    ExactContourEmojiSticker(emoji = emoji, size = 64.dp, rotation = -4f)
                }

                Spacer(Modifier.height(22.dp))
                Text(
                    text = "Delete this folder?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = GlassPanelTokens.TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Are you sure you want to delete '$folderName'?",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                    color = GlassPanelTokens.TextSecondary
                )

                Spacer(Modifier.height(22.dp))
                GlassSectionHeader("Heads up")
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GlassPanelTokens.InnerRadius))
                        .background(GlassPanelTokens.PillFill)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = GlassPanelTokens.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Files inside will stay safe in Telegram, but this folder channel will be permanently removed from KeepMe.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, lineHeight = 17.sp),
                        color = GlassPanelTokens.TextSecondary
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassSecondaryButton(text = "No, Keep It", onClick = onCancelNo)
                GlassPrimaryButton(text = "Delete", destructive = true, onClick = onConfirmSure)
            }
        }
    }
}