package com.telegramdrive.app.ui.screen.trash

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.telegramdrive.app.domain.model.TelegramFile
import com.telegramdrive.app.ui.components.WaterfallGrid
import com.telegramdrive.app.ui.utils.rememberAppHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onBack: () -> Unit = {},
    viewModel: TrashViewModel = hiltViewModel()
) {
    val haptics = rememberAppHaptics()
    val trashFiles by viewModel.trashFiles.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var selectedTrashFile by remember { mutableStateOf<TelegramFile?>(null) }

    // Dialog when clicking on an individual trashed file
    if (selectedTrashFile != null) {
        val file = selectedTrashFile!!
        com.telegramdrive.app.ui.components.GlassAlertDialog(
            title = file.fileName,
            message = "What would you like to do with this file?",
            onDismiss = { selectedTrashFile = null },
            body = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    com.telegramdrive.app.ui.components.GlassActionRow(
                        icon = Icons.Rounded.Restore,
                        title = "Restore File",
                        subtitle = "Move it back to your drive",
                        tint = Color(0xFF00E5A0),
                        onClick = {
                            haptics.click()
                            viewModel.restoreFile(file)
                            selectedTrashFile = null
                        }
                    )
                    com.telegramdrive.app.ui.components.GlassActionRow(
                        icon = Icons.Rounded.DeleteForever,
                        title = "Delete Permanently",
                        subtitle = "This cannot be undone",
                        tint = Color(0xFFFF8A80),
                        onClick = {
                            haptics.heavyClick()
                            viewModel.deletePermanently(file)
                            selectedTrashFile = null
                        }
                    )
                }
            }
        )
    }

    // Dialog for emptying all trash
    if (showEmptyTrashDialog) {
        com.telegramdrive.app.ui.components.GlassAlertDialog(
            title = "Empty Trash",
            message = "Permanently delete all ${trashFiles.size} items? They will be removed from your Telegram account completely and cannot be recovered.",
            confirmText = "Empty Trash",
            destructive = true,
            heroIcon = Icons.Rounded.DeleteSweep,
            heroTint = Color(0xFFFF453A),
            onConfirm = {
                haptics.heavyClick()
                viewModel.emptyTrash()
                showEmptyTrashDialog = false
            },
            onDismiss = { showEmptyTrashDialog = false }
        )
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (trashFiles.isNotEmpty()) {
                com.telegramdrive.app.ui.components.GlassPrimaryButton(
                    text = "Empty Trash",
                    destructive = true,
                    onClick = {
                        haptics.error()
                        showEmptyTrashDialog = true
                    }
                )
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                haptics.tick()
                viewModel.refresh()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Custom Header with Back Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.telegramdrive.app.ui.components.ScreenBackButton(onBack = onBack)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Trash",
                            style = com.telegramdrive.app.ui.components.screenTitleStyle(),
                            color = com.telegramdrive.app.ui.theme.TDriveColors.OnCanvas
                        )
                        Text(
                            text = if (trashFiles.isEmpty()) "Deleted files wait here for 30 days"
                            else "${trashFiles.size} items · deleted for good after 30 days",
                            style = MaterialTheme.typography.bodySmall,
                            color = com.telegramdrive.app.ui.theme.TDriveColors.OnCanvasMuted
                        )
                    }
                    if (trashFiles.isNotEmpty()) {
                        Text(
                            text = "Restore all",
                            style = MaterialTheme.typography.labelLarge,
                            color = com.telegramdrive.app.ui.theme.TDriveColors.Accent,
                            modifier = Modifier
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .border(1.dp, com.telegramdrive.app.ui.theme.TDriveColors.LineSoft, androidx.compose.foundation.shape.CircleShape)
                                .clickable {
                                    haptics.click()
                                    viewModel.restoreAll()
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                if (trashFiles.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Empty",
                                modifier = Modifier.size(64.dp),
                                tint = com.telegramdrive.app.ui.theme.TDriveColors.OnCanvasFaint
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Your trash is completely empty! 🧹",
                                style = MaterialTheme.typography.titleMedium,
                                color = com.telegramdrive.app.ui.theme.TDriveColors.OnCanvasMuted
                            )
                        }
                    }
                } else {
                    WaterfallGrid(
                        files = trashFiles,
                        onFileClick = { file ->
                            selectedTrashFile = file
                        },
                        onFileLongPress = { file ->
                            selectedTrashFile = file
                        },
                        onFileVisible = { file ->
                            viewModel.requestThumbnail(file.thumbnailFileId)
                        },
                        itemBadge = { file ->
                            val trashedAt = file.trashedAt
                            if (trashedAt == null) null else {
                                val daysLeft = (30 - (System.currentTimeMillis() - trashedAt) / 86_400_000L).coerceAtLeast(0)
                                if (daysLeft == 0L) "today" else "${daysLeft}d left"
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
