package com.telegramdrive.app.ui.screen.trash

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
        AlertDialog(
            onDismissRequest = { selectedTrashFile = null },
            title = {
                Text(
                    text = file.fileName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = { Text("What would you like to do with this file?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.click()
                        viewModel.restoreFile(file)
                        selectedTrashFile = null
                    }
                ) {
                    Text("Restore File", color = Color(0xFF00E5A0))
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            haptics.heavyClick()
                            viewModel.deletePermanently(file)
                            selectedTrashFile = null
                        }
                    ) {
                        Text("Delete Permanently", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { selectedTrashFile = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Dialog for emptying all trash
    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text("Empty Trash") },
            text = { Text("Permanently delete all ${trashFiles.size} items? They will be removed from your Telegram account completely and cannot be recovered.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.heavyClick()
                        viewModel.emptyTrash()
                        showEmptyTrashDialog = false
                    }
                ) {
                    Text("Empty Trash", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (trashFiles.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        haptics.error()
                        showEmptyTrashDialog = true
                    },
                    icon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = "Empty Trash") },
                    text = { Text("Empty Trash") },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
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
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(48.dp)
                    ) {
                        IconButton(onClick = {
                            haptics.click()
                            onBack()
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF2C2C2E)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Trash",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White
                    )
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
                                tint = Color.White.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Your trash is completely empty! 🧹",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.7f)
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
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
