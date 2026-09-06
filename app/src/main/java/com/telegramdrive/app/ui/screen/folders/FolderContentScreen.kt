package com.telegramdrive.app.ui.screen.folders

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.telegramdrive.app.domain.model.TelegramFile
import com.telegramdrive.app.ui.components.WaterfallGrid
import com.telegramdrive.app.ui.components.PrismalSortBar
import com.telegramdrive.app.ui.components.LiquidGlassCreateFolderDialog
import com.telegramdrive.app.ui.screen.home.FilterChip
import com.telegramdrive.app.ui.theme.TDriveColors
import com.telegramdrive.app.ui.theme.TDriveMotion
import com.telegramdrive.app.ui.utils.rememberAppHaptics

import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.telegramdrive.app.ui.components.SelectionBarActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderContentScreen(
    folderName: String,
    onBack: () -> Unit,
    onFileClick: (TelegramFile) -> Unit,
    onSelectionActionsChange: (SelectionBarActions?) -> Unit = {},
    viewModel: FolderContentViewModel = hiltViewModel()
) {
    val haptics = rememberAppHaptics()
    val context = androidx.compose.ui.platform.LocalContext.current
    val files by viewModel.files.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val sortDirection by viewModel.sortDirection.collectAsState()
    val selectedFileIds by viewModel.selectedFileIds.collectAsState()
    val inSelectionMode = selectedFileIds.isNotEmpty()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val gridState = androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState()

    // When sort option or sort direction changes, reset scroll to top so user sees the sorted list from item 0
    LaunchedEffect(sortOption, sortDirection) {
        gridState.scrollToItem(0)
    }

    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    // Intercept back gesture/button in selection mode to deselect all files without exiting the folder
    BackHandler(enabled = inSelectionMode) {
        haptics.click()
        viewModel.clearSelection()
    }

    // Sync selection actions with bottom morphing bar
    LaunchedEffect(inSelectionMode, selectedFileIds.size) {
        if (inSelectionMode) {
            onSelectionActionsChange(
                SelectionBarActions(
                    selectedCount = selectedFileIds.size,
                    onDelete = { viewModel.deleteSelected() },
                    onShare = {
                        val selectedFiles = viewModel.files.value.filter { it.messageId in viewModel.selectedFileIds.value }
                        selectedFiles.forEach { file ->
                            com.telegramdrive.app.ui.screen.preview.FileOpenUtils.shareFileInExternalApp(context, file)
                        }
                        viewModel.clearSelection()
                    },
                    onDownload = {
                        viewModel.downloadSelected()
                    },
                    onClear = { viewModel.clearSelection() }
                )
            )
        } else {
            onSelectionActionsChange(null)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onSelectionActionsChange(null)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.uploadFiles(uris)
        }
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Custom Header
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
                        // ── Icon morphs smoothly between Back and Close ──
                        AnimatedContent(
                            targetState = inSelectionMode,
                            transitionSpec = { TDriveMotion.iconMorphIn },
                            label = "headerIcon"
                        ) { inSelection ->
                            IconButton(onClick = {
                                haptics.click()
                                if (inSelection) viewModel.clearSelection() else onBack()
                            }) {
                                Icon(
                                    imageVector = if (inSelection) Icons.Rounded.Close else Icons.Rounded.ArrowBack,
                                    contentDescription = if (inSelection) "Clear Selection" else "Back",
                                    tint = Color(0xFF2C2C2E)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))

                    // ── Title slides up/down when toggling selection mode ──
                    AnimatedContent(
                        targetState = if (inSelectionMode) "${selectedFileIds.size} Selected" else folderName,
                        transitionSpec = { TDriveMotion.titleMorphIn },
                        label = "headerTitle"
                    ) { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TDriveColors.OnSurface
                        )
                    }
                }

                // Sort Chips with animated sliding Prismal glass slab & sort order arrow
                PrismalSortBar(
                    sortOption = sortOption,
                    onSortOptionChange = { option -> viewModel.setSortOption(option) },
                    sortDirection = sortDirection,
                    onToggleSortDirection = { viewModel.toggleSortDirection() }
                )

                val isLoading by viewModel.isLoading.collectAsState()

                Box(modifier = Modifier.weight(1f)) {
                    WaterfallGrid(
                        files = files,
                        gridState = gridState,
                        isLoading = isLoading,
                        selectedFileIds = selectedFileIds,
                        inSelectionMode = inSelectionMode,
                        groupByMonth = (sortOption == SortOption.DATE),
                        onFileClick = { file -> 
                            if (inSelectionMode) {
                                viewModel.toggleSelection(file.messageId)
                            } else {
                                onFileClick(file)
                            }
                        },
                        onFileLongPress = { file -> 
                            viewModel.toggleSelection(file.messageId)
                        },
                        onFileDragSelect = { file -> viewModel.selectFileById(file.messageId) },
                        onFileVisible = { file -> viewModel.requestThumbnail(file.thumbnailFileId) },
                        onEmptyAreaLongPress = { offset ->
                            if (!inSelectionMode) {
                                haptics.heavyClick()
                                contextMenuOffset = offset
                                showContextMenu = true
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    DropdownMenu(
                        expanded = showContextMenu,
                        onDismissRequest = { showContextMenu = false },
                        offset = DpOffset(
                            x = with(LocalDensity.current) { contextMenuOffset.x.toDp() },
                            y = with(LocalDensity.current) { contextMenuOffset.y.toDp() - 100.dp }
                        )
                    ) {
                        DropdownMenuItem(
                            text = { Text("📁 New Folder") },
                            onClick = {
                                haptics.click()
                                showContextMenu = false
                                showCreateFolderDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateFolderDialog) {
        LiquidGlassCreateFolderDialog(
            title = "New Subfolder",
            subtitle = "Create a new space inside $folderName",
            confirmText = "Create",
            onConfirm = { name ->
                viewModel.createSubFolder(name)
                showCreateFolderDialog = false
            },
            onDismiss = { showCreateFolderDialog = false }
        )
    }
}
