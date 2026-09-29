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
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.telegramdrive.app.domain.model.TelegramFile
import com.telegramdrive.app.ui.components.WaterfallGrid
import com.telegramdrive.app.ui.components.folderStickerTransition
import com.telegramdrive.app.ui.components.PrismalSortBar
import com.telegramdrive.app.ui.components.LiquidGlassCreateFolderDialog
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

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.blur
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.telegramdrive.app.ui.components.SelectionBarActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderContentScreen(
    folderName: String,
    chatId: Long = 0L,
    onBack: () -> Unit,
    onFileClick: (TelegramFile) -> Unit,
    onNewFolderClick: () -> Unit = {},
    onSelectionActionsChange: (SelectionBarActions?) -> Unit = {},
    backdrop: com.styropyr0.prismal.PrismalBackdrop? = null,
    viewModel: FolderContentViewModel = hiltViewModel()
) {
    val haptics = rememberAppHaptics()
    val context = androidx.compose.ui.platform.LocalContext.current
    val files by viewModel.files.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val sortDirection by viewModel.sortDirection.collectAsState()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsState()
    val availableMonths by viewModel.availableMonths.collectAsState()
    val selectedFileIds by viewModel.selectedFileIds.collectAsState()
    val inSelectionMode = selectedFileIds.isNotEmpty()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val gridState = androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState()

    // When sort option, direction, or date filter changes, reset scroll to top
    LaunchedEffect(sortOption, sortDirection, selectedDateFilter) {
        gridState.scrollToItem(0)
    }

    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }
    var showDateFilterSheet by remember { mutableStateOf(false) }

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
                    com.telegramdrive.app.ui.components.ScreenBackButton(
                        onBack = onBack,
                        inSelectionMode = inSelectionMode,
                        onClearSelection = { viewModel.clearSelection() }
                    )
                    Spacer(modifier = Modifier.width(16.dp))

                    // ── Title slides up/down when toggling selection mode ──
                    Column(modifier = Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = if (inSelectionMode) "${selectedFileIds.size} Selected" else folderName,
                            transitionSpec = { TDriveMotion.titleMorphIn },
                            label = "headerTitle"
                        ) { title ->
                            Text(
                                text = title,
                                style = com.telegramdrive.app.ui.components.screenTitleStyle(),
                                color = com.telegramdrive.app.ui.theme.TDriveColors.OnCanvas,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        if (!inSelectionMode && files.isNotEmpty()) {
                            Text(
                                text = "${files.size} items · ${com.telegramdrive.app.ui.components.formatFileSize(files.sumOf { it.fileSize })}",
                                style = MaterialTheme.typography.bodySmall,
                                color = com.telegramdrive.app.ui.theme.TDriveColors.OnCanvasMuted
                            )
                        }
                    }

                    val folderEmoji by viewModel.folderEmoji.collectAsState()
                    folderEmoji?.let { emoji ->
                        Spacer(modifier = Modifier.width(12.dp))
                        com.telegramdrive.app.ui.utils.ExactContourEmojiSticker(
                            emoji = emoji,
                            size = 44.dp,
                            rotation = -6f,
                            modifier = Modifier.folderStickerTransition(chatId)
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

                // ── Date & Month Filter Pill ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptics.click()
                                showDateFilterSheet = true
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedDateFilter != null) com.telegramdrive.app.ui.theme.TDriveColors.OnCanvas else com.telegramdrive.app.ui.theme.TDriveColors.FillSoft,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedDateFilter != null) com.telegramdrive.app.ui.theme.TDriveColors.OnCanvas else com.telegramdrive.app.ui.theme.TDriveColors.LineSoft
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.CalendarMonth,
                                contentDescription = "Date Filter",
                                tint = if (selectedDateFilter != null) Color(0xFF0B0C0F) else com.telegramdrive.app.ui.theme.TDriveColors.OnCanvas,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = selectedDateFilter?.displayName ?: "Filter by Date / Month",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (selectedDateFilter != null) Color(0xFF0B0C0F) else com.telegramdrive.app.ui.theme.TDriveColors.OnCanvas
                            )
                            if (selectedDateFilter != null) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.14f))
                                        .clickable {
                                            haptics.click()
                                            viewModel.setDateFilter(null)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Clear Filter",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    Icons.Rounded.ArrowDropDown,
                                    contentDescription = null,
                                    tint = com.telegramdrive.app.ui.theme.TDriveColors.OnCanvasMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (selectedDateFilter != null) {
                        Text(
                            text = "${files.size} ${if (files.size == 1) "item" else "items"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = com.telegramdrive.app.ui.theme.TDriveColors.OnCanvasMuted
                        )
                    }
                }

                val isLoading by viewModel.isLoading.collectAsState()

                Box(modifier = Modifier.weight(1f)) {
                    if (files.isEmpty() && selectedDateFilter != null && !isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No files found for \"${selectedDateFilter?.displayName}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = com.telegramdrive.app.ui.theme.TDriveColors.OnCanvasMuted
                                )
                                TextButton(
                                    onClick = {
                                        haptics.click()
                                        viewModel.setDateFilter(null)
                                    }
                                ) {
                                    Text("Show All Dates", color = com.telegramdrive.app.ui.theme.TDriveColors.OnCanvas, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        WaterfallGrid(
                            files = files,
                            gridState = gridState,
                            isLoading = isLoading,
                            emptyText = if (selectedDateFilter != null) "No files match this filter" else "This folder is empty",
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
                    }

                    DropdownMenu(
                        expanded = showContextMenu,
                        onDismissRequest = { showContextMenu = false },
                        shape = RoundedCornerShape(24.dp),
                        containerColor = TDriveColors.SurfaceElevated,
                        tonalElevation = 0.dp,
                        shadowElevation = 12.dp,
                        offset = DpOffset(
                            x = with(LocalDensity.current) { contextMenuOffset.x.toDp() },
                            y = with(LocalDensity.current) { contextMenuOffset.y.toDp() - 100.dp }
                        )
                    ) {
                        DropdownMenuItem(
                            text = { Text("📁 New Folder", color = TDriveColors.OnCanvas) },
                            onClick = {
                                haptics.click()
                                showContextMenu = false
                                onNewFolderClick()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDateFilterSheet) {
        com.telegramdrive.app.ui.components.DateMonthFilterSheet(
            activeFilter = selectedDateFilter,
            availableMonths = availableMonths,
            onSelectFilter = { filter ->
                viewModel.setDateFilter(filter)
            },
            onDismiss = { showDateFilterSheet = false }
        )
    }


}
