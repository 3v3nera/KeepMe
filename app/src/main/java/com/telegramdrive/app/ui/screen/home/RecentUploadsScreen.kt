package com.telegramdrive.app.ui.screen.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.telegramdrive.app.domain.model.FileType
import com.telegramdrive.app.domain.model.TelegramFile
import com.telegramdrive.app.domain.model.selectionKey
import com.telegramdrive.app.ui.components.PrismalSortBar
import com.telegramdrive.app.ui.components.SelectionBarActions
import com.telegramdrive.app.ui.components.WaterfallGrid
import com.telegramdrive.app.ui.screen.preview.FileOpenUtils
import com.telegramdrive.app.ui.theme.TDriveColors
import com.telegramdrive.app.ui.theme.TDriveMotion
import com.telegramdrive.app.ui.utils.rememberAppHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentUploadsScreen(
    onBack: () -> Unit,
    onFileClick: (TelegramFile) -> Unit,
    onSelectionActionsChange: (SelectionBarActions?) -> Unit = {},
    viewModel: RecentUploadsViewModel = hiltViewModel()
) {
    val haptics = rememberAppHaptics()
    val context = LocalContext.current
    val files by viewModel.files.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val sortDirection by viewModel.sortDirection.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsState()
    val availableMonths by viewModel.availableMonths.collectAsState()
    val selectedFileIds by viewModel.selectedFileIds.collectAsState()
    val inSelectionMode = selectedFileIds.isNotEmpty()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val gridState = androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState()

    var showDateFilterSheet by remember { mutableStateOf(false) }

    LaunchedEffect(sortOption, sortDirection, selectedFilter, selectedDateFilter) {
        gridState.scrollToItem(0)
    }

    BackHandler(enabled = inSelectionMode) {
        haptics.click()
        viewModel.clearSelection()
    }

    LaunchedEffect(inSelectionMode, selectedFileIds.size) {
        if (inSelectionMode) {
            onSelectionActionsChange(
                SelectionBarActions(
                    selectedCount = selectedFileIds.size,
                    onDelete = { viewModel.deleteSelected() },
                    onShare = {
                        val selectedFiles = viewModel.files.value.filter { it.selectionKey in viewModel.selectedFileIds.value }
                        selectedFiles.forEach { file ->
                            FileOpenUtils.shareFileInExternalApp(context, file)
                        }
                        viewModel.clearSelection()
                    },
                    onDownload = { viewModel.downloadSelected() },
                    onClear = { viewModel.clearSelection() }
                )
            )
        } else {
            onSelectionActionsChange(null)
        }
    }

    DisposableEffect(Unit) {
        onDispose { onSelectionActionsChange(null) }
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
                // Top Header
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

                    AnimatedContent(
                        targetState = if (inSelectionMode) "${selectedFileIds.size} Selected" else "Recent Uploads",
                        transitionSpec = { TDriveMotion.titleMorphIn },
                        label = "headerTitle"
                    ) { title ->
                        Column {
                            Text(
                                text = title,
                                style = com.telegramdrive.app.ui.components.screenTitleStyle(),
                                color = com.telegramdrive.app.ui.theme.TDriveColors.OnCanvas
                            )
                            if (!inSelectionMode) {
                                Text(
                                    text = "${files.size} items in your cloud vault",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = com.telegramdrive.app.ui.theme.TDriveColors.OnCanvasMuted
                                )
                            }
                        }
                    }
                }

                // Filter Chips (All, Images, Videos, Documents, Audio)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterPill(
                            label = "All",
                            isSelected = selectedFilter == null,
                            onClick = {
                                haptics.click()
                                viewModel.setFilter(null)
                            }
                        )
                    }
                    item {
                        FilterPill(
                            label = "Photos",
                            isSelected = selectedFilter == FileType.IMAGE,
                            onClick = {
                                haptics.click()
                                viewModel.setFilter(FileType.IMAGE)
                            }
                        )
                    }
                    item {
                        FilterPill(
                            label = "Videos",
                            isSelected = selectedFilter == FileType.VIDEO,
                            onClick = {
                                haptics.click()
                                viewModel.setFilter(FileType.VIDEO)
                            }
                        )
                    }
                    item {
                        FilterPill(
                            label = "Docs",
                            isSelected = selectedFilter == FileType.DOCUMENT,
                            onClick = {
                                haptics.click()
                                viewModel.setFilter(FileType.DOCUMENT)
                            }
                        )
                    }
                    item {
                        FilterPill(
                            label = "Audio",
                            isSelected = selectedFilter == FileType.AUDIO,
                            onClick = {
                                haptics.click()
                                viewModel.setFilter(FileType.AUDIO)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Prismal Sort Bar
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

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (files.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (selectedDateFilter != null) "No files found for \"${selectedDateFilter?.displayName}\"" else "No recent files found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = com.telegramdrive.app.ui.theme.TDriveColors.OnCanvasMuted
                                )
                                if (selectedDateFilter != null) {
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
                        }
                    } else {
                        WaterfallGrid(
                            files = files,
                            gridState = gridState,
                            groupByMonth = (sortOption == com.telegramdrive.app.ui.screen.folders.SortOption.DATE),
                            selectedFileIds = selectedFileIds,
                            selectionKey = { it.selectionKey },
                            inSelectionMode = inSelectionMode,
                            onFileClick = { file ->
                                if (inSelectionMode) {
                                    viewModel.toggleSelection(file.selectionKey)
                                } else {
                                    onFileClick(file)
                                }
                            },
                            onFileLongPress = { file ->
                                viewModel.toggleSelection(file.selectionKey)
                            },
                            onFileDragSelect = { file -> viewModel.selectFileById(file.selectionKey) },
                            onFileVisible = { file -> viewModel.requestThumbnail(file.thumbnailFileId) },
                            modifier = Modifier.fillMaxSize()
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

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) TDriveColors.OnCanvas else TDriveColors.FillSoft,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) TDriveColors.OnCanvas else TDriveColors.LineSoft
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (isSelected) Color(0xFF0B0C0F) else TDriveColors.OnCanvas,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}
