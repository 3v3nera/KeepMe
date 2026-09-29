package com.telegramdrive.app.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.telegramdrive.app.domain.model.FileType
import com.telegramdrive.app.domain.model.TelegramFile
import com.telegramdrive.app.domain.update.UpdateDownloadState
import com.telegramdrive.app.ui.components.BentoCard
import com.telegramdrive.app.ui.components.FileCard
import com.telegramdrive.app.ui.components.IconCircleButton
import com.telegramdrive.app.ui.components.MonthPhotoCard
import com.telegramdrive.app.ui.components.NavBarMetrics
import com.telegramdrive.app.ui.components.SearchPill
import com.telegramdrive.app.ui.components.SectionHeader
import com.telegramdrive.app.ui.components.TDriveSwitch
import com.telegramdrive.app.ui.components.TwoToneTitle
import com.telegramdrive.app.ui.components.formatFileSize
import com.telegramdrive.app.ui.components.screenTabSwipe
import com.telegramdrive.app.ui.screen.home.components.LiveUpdateDownloadDialog
import com.telegramdrive.app.ui.screen.home.components.UpdateAvailableBanner
import com.telegramdrive.app.ui.screen.home.components.UpdatePromptDialog
import com.telegramdrive.app.ui.theme.TDriveColors
import com.telegramdrive.app.ui.utils.rememberAppHaptics
import com.telegramdrive.app.utils.formatRelativeTime
import java.io.File

/** Session holder to track whether the cold-start splash animation has played */
object AppSessionState {
    var hasPlayedSplash: Boolean by mutableStateOf(false)
    var updatePromptShownForVersion: Int = -1
}

private val VideoColor = Color(0xFFFF6B9A)
private val ImageColor = Color(0xFF7C9BFF)
private val DocColor = TDriveColors.Accent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToFolder: (String, Long) -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSettings: () -> Unit,
    onNavigateToRecentUploads: () -> Unit = {},
    onNavigateToTrash: () -> Unit = {},
    onNewFolderClick: () -> Unit = {},
    onFileClick: (TelegramFile) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val haptics = rememberAppHaptics()
    val favoriteFiles by viewModel.favoriteFiles.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()
    val monthCovers by viewModel.monthCovers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val userProfilePhotoPath by viewModel.userProfilePhotoPath.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isBackupEnabled by viewModel.isBackupEnabled.collectAsState()
    val lastBackupTime by viewModel.lastBackupTime.collectAsState()

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        entered = true
        // Cheap refresh on every visit; the full re-crawl only happens on pull-to-refresh
        viewModel.refresh(userInitiated = false)
    }

    val availableUpdate by viewModel.availableUpdate.collectAsState()
    val downloadState by viewModel.updateDownloadState.collectAsState()
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showUpdatePromptDialog by remember { mutableStateOf(false) }

    LaunchedEffect(availableUpdate) {
        val update = availableUpdate
        // Prompt once per version per app session, not on every return to the Home tab
        if (update != null && downloadState is UpdateDownloadState.Idle &&
            AppSessionState.updatePromptShownForVersion != update.versionCode
        ) {
            AppSessionState.updatePromptShownForVersion = update.versionCode
            showUpdatePromptDialog = true
        }
    }

    LaunchedEffect(downloadState) {
        if (downloadState is UpdateDownloadState.Downloading || downloadState is UpdateDownloadState.ReadyToInstall) {
            showDownloadDialog = true
        }
    }

    // Ask TDLib for cover thumbnails that have not been fetched yet
    LaunchedEffect(monthCovers) {
        monthCovers.take(4).forEach { cover ->
            if (cover.coverPath == null) viewModel.requestThumbnail(cover.coverThumbnailFileId)
        }
    }

    val topPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val firstName = currentUser?.firstName?.takeIf { it.isNotBlank() } ?: "there"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .screenTabSwipe(onSwipeNext = onNavigateToSearch, onSwipePrev = {})
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                haptics.tick()
                viewModel.refresh()
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topPadding + 12.dp, bottom = NavBarMetrics.contentBottomPadding),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── Header: avatar + settings ──
                item {
                    AnimatedVisibility(
                        visible = entered,
                        enter = slideInVertically(tween(320)) { -it / 3 } + fadeIn(tween(280))
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(TDriveColors.SurfaceElevated)
                                        .border(1.dp, TDriveColors.LineSoft, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!userProfilePhotoPath.isNullOrBlank() && File(userProfilePhotoPath!!).exists()) {
                                        AsyncImage(
                                            model = File(userProfilePhotoPath!!),
                                            contentDescription = "Profile Photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = firstName.take(1).uppercase(),
                                            color = TDriveColors.OnCanvas,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(TDriveColors.SurfaceElevated)
                                        .border(1.dp, TDriveColors.LineSoft, CircleShape)
                                        .clickable {
                                            haptics.click()
                                            onNavigateToSettings()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = "Settings",
                                        tint = TDriveColors.OnCanvas,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            TwoToneTitle(
                                bold = "Hey, $firstName",
                                muted = "What are we keeping today?",
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }

                // ── Search pill (opens the Search tab) ──
                item {
                    SearchPill(
                        placeholder = "Search your files",
                        onClick = onNavigateToSearch,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                // ── Quick actions ──
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconCircleButton(Icons.Rounded.CreateNewFolder, "New folder", onNewFolderClick)
                        IconCircleButton(Icons.Rounded.CloudUpload, "Back up now", { viewModel.backupNow() })
                        IconCircleButton(Icons.Rounded.History, "Recent", onNavigateToRecentUploads)
                        IconCircleButton(Icons.Rounded.DeleteOutline, "Trash", onNavigateToTrash)
                    }
                }

                // ── Update banner ──
                if (availableUpdate != null) {
                    item {
                        UpdateAvailableBanner(
                            updateInfo = availableUpdate!!,
                            onUpdateClick = {
                                haptics.click()
                                showDownloadDialog = true
                                viewModel.startUpdateDownload(availableUpdate!!)
                            },
                            onDismissClick = {
                                haptics.click()
                                viewModel.dismissUpdate(availableUpdate!!.versionCode)
                            }
                        )
                    }
                }

                // ── Bento: storage + backup ──
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max)
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StorageBentoCard(
                            stats = storageStats,
                            modifier = Modifier
                                .weight(1.25f)
                                .fillMaxHeight()
                        )
                        BackupBentoCard(
                            enabled = isBackupEnabled,
                            lastBackupTime = lastBackupTime,
                            onToggle = { viewModel.toggleBackup(it) },
                            onOpen = onNavigateToSettings,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }

                // ── Month photo cards ──
                if (monthCovers.isEmpty()) {
                    item {
                        Text(
                            text = "Nothing here yet. Drop something in to get started!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TDriveColors.OnCanvasMuted,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                } else {
                    item {
                        SectionHeader(
                            title = "Your months",
                            action = "See all",
                            onAction = onNavigateToRecentUploads
                        )
                    }
                    items(monthCovers.take(3), key = { "month_${it.filter.year}_${it.filter.month}" }) { cover ->
                        MonthPhotoCard(
                            title = cover.filter.displayName,
                            count = cover.count,
                            coverPath = cover.coverPath,
                            onClick = {
                                HomeMonthLink.pending = cover.filter
                                onNavigateToRecentUploads()
                            },
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }

                // ── Favorites ──
                if (favoriteFiles.isNotEmpty()) {
                    item { SectionHeader(title = "Favorites") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(favoriteFiles, key = { "fav_${it.chatId}_${it.messageId}" }) { file ->
                                Box(modifier = Modifier.width(180.dp)) {
                                    FileCard(
                                        file = file,
                                        onClick = { onFileClick(file) },
                                        onLongPress = { },
                                        onVisible = { viewModel.requestThumbnail(file.thumbnailFileId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDownloadDialog && availableUpdate != null) {
            LiveUpdateDownloadDialog(
                updateInfo = availableUpdate!!,
                downloadState = downloadState,
                onCancelClick = {
                    haptics.click()
                    viewModel.cancelUpdateDownload(availableUpdate!!.fileId)
                    showDownloadDialog = false
                },
                onInstallClick = {
                    haptics.click()
                    if (downloadState is UpdateDownloadState.ReadyToInstall) {
                        viewModel.installApk((downloadState as UpdateDownloadState.ReadyToInstall).apkFile)
                    }
                },
                onDismissClick = {
                    haptics.click()
                    showDownloadDialog = false
                    viewModel.resetDownloadState()
                },
                onRetryClick = {
                    haptics.click()
                    viewModel.startUpdateDownload(availableUpdate!!)
                }
            )
        }

        // Automatic update prompt dialog when app opens (shown only after splash finishes)
        if (showUpdatePromptDialog && availableUpdate != null && !showDownloadDialog && AppSessionState.hasPlayedSplash) {
            UpdatePromptDialog(
                updateInfo = availableUpdate!!,
                onUpdateClick = {
                    haptics.click()
                    showUpdatePromptDialog = false
                    showDownloadDialog = true
                    viewModel.startUpdateDownload(availableUpdate!!)
                },
                onDismissClick = {
                    haptics.click()
                    showUpdatePromptDialog = false
                }
            )
        }
    }
}

/** Total used space plus a proportional bar and per-type counts. */
@Composable
private fun StorageBentoCard(
    stats: List<com.telegramdrive.app.domain.model.StorageStat>,
    modifier: Modifier = Modifier
) {
    val video = stats.filter { it.fileType == FileType.VIDEO }
    val image = stats.filter { it.fileType == FileType.IMAGE }
    val docs = stats.filter { it.fileType != FileType.VIDEO && it.fileType != FileType.IMAGE }
    val videoBytes = video.sumOf { it.totalSize }
    val imageBytes = image.sumOf { it.totalSize }
    val docBytes = docs.sumOf { it.totalSize }
    val total = videoBytes + imageBytes + docBytes

    BentoCard(modifier = modifier) {
        Text(
            text = "Storage",
            style = MaterialTheme.typography.labelLarge,
            color = TDriveColors.OnCanvasMuted
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = formatFileSize(total),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = TDriveColors.OnCanvas
        )
        Spacer(Modifier.height(14.dp))
        // proportional segmented bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(TDriveColors.FillSoft),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (total > 0) {
                listOf(videoBytes to VideoColor, imageBytes to ImageColor, docBytes to DocColor).forEach { (bytes, color) ->
                    if (bytes > 0) {
                        Box(
                            modifier = Modifier
                                .weight((bytes.toFloat() / total).coerceAtLeast(0.04f))
                                .fillMaxHeight()
                                .background(color)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        StorageLegendRow(VideoColor, "Videos", video.sumOf { it.fileCount })
        StorageLegendRow(ImageColor, "Photos", image.sumOf { it.fileCount })
        StorageLegendRow(DocColor, "Files", docs.sumOf { it.fileCount })
    }
}

@Composable
private fun StorageLegendRow(color: Color, label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TDriveColors.OnCanvasMuted,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
            color = TDriveColors.OnCanvas
        )
    }
}

/** Camera-backup status with an inline toggle. */
@Composable
private fun BackupBentoCard(
    enabled: Boolean,
    lastBackupTime: Long,
    onToggle: (Boolean) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    BentoCard(modifier = modifier, onClick = onOpen) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Backup",
                style = MaterialTheme.typography.labelLarge,
                color = TDriveColors.OnCanvasMuted
            )
            TDriveSwitch(
                checked = enabled,
                onCheckedChange = onToggle,
                activeColor = TDriveColors.Accent,
                inactiveColor = Color.White.copy(alpha = 0.22f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (enabled) "On" else "Paused",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = if (enabled) TDriveColors.Accent else TDriveColors.OnCanvas
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = when {
                enabled && lastBackupTime > 0L -> "Last run ${formatRelativeTime(lastBackupTime)}"
                enabled -> "Waiting for the first run"
                else -> "Tap to configure"
            },
            style = MaterialTheme.typography.bodySmall,
            color = TDriveColors.OnCanvasMuted
        )
    }
}
