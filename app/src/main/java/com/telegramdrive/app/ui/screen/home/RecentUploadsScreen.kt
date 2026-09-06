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
    val selectedFileIds by viewModel.selectedFileIds.collectAsState()
    val inSelectionMode = selectedFileIds.isNotEmpty()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val gridState = androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState()

    LaunchedEffect(sortOption, sortDirection, selectedFilter) {
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
                        val selectedFiles = viewModel.files.value.filter { it.messageId in viewModel.selectedFileIds.value }
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
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                // Top Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(46.dp)
                    ) {
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
                                    imageVector = if (inSelection) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = if (inSelection) "Clear Selection" else "Back",
                                    tint = Color(0xFF2C2C2E)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))

                    AnimatedContent(
                        targetState = if (inSelectionMode) "${selectedFileIds.size} Selected" else "Recent Uploads",
                        transitionSpec = { TDriveMotion.titleMorphIn },
                        label = "headerTitle"
                    ) { title ->
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = TDriveColors.OnBackground
                            )
                            if (!inSelectionMode) {
                                Text(
                                    text = "${files.size} items in your cloud vault",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TDriveColors.OnSurfaceDim
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

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (files.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No recent files found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TDriveColors.OnSurfaceDim
                            )
                        }
                    } else {
                        WaterfallGrid(
                            files = files,
                            gridState = gridState,
                            groupByMonth = (sortOption == com.telegramdrive.app.ui.screen.folders.SortOption.DATE),
                            selectedFileIds = selectedFileIds,
                            inSelectionMode = inSelectionMode,
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
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
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
        color = if (isSelected) TDriveColors.Primary else TDriveColors.SurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) TDriveColors.Primary else TDriveColors.OutlineVariant
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (isSelected) Color.White else TDriveColors.OnBackground,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}
