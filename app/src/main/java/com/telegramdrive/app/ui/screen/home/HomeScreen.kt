package com.telegramdrive.app.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.lerp
import kotlinx.coroutines.delay
import com.telegramdrive.app.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.telegramdrive.app.domain.model.TelegramFile
import com.telegramdrive.app.ui.components.FileCard
import com.telegramdrive.app.ui.components.TDriveSwitch
import com.telegramdrive.app.ui.theme.TDriveColors
import com.telegramdrive.app.ui.theme.TDriveMotion
import com.telegramdrive.app.utils.formatRelativeTime
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import coil.compose.AsyncImage
import java.io.File

import com.telegramdrive.app.ui.components.screenTabSwipe
import com.telegramdrive.app.ui.utils.rememberAppHaptics
import com.telegramdrive.app.domain.update.UpdateDownloadState
import com.telegramdrive.app.ui.screen.home.components.LiveUpdateDownloadDialog
import com.telegramdrive.app.ui.screen.home.components.UpdateAvailableBanner
import com.telegramdrive.app.ui.screen.home.components.UpdatePromptDialog

/** Session holder to track whether the cold-start splash animation has played */
object AppSessionState {
    var hasPlayedSplash: Boolean by mutableStateOf(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToFolder: (String, Long) -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSettings: () -> Unit,
    onNavigateToRecentUploads: () -> Unit = {},
    onFileClick: (TelegramFile) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val haptics = rememberAppHaptics()
    val recentFiles by viewModel.recentFiles.collectAsState()
    val favoriteFiles by viewModel.favoriteFiles.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val userProfilePhotoPath by viewModel.userProfilePhotoPath.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()


    val greeting = remember(isRefreshing) {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val greetings = when (hour) {
            in 0..4 -> listOf(
                "Up so late",
                "Still awake",
                "Night owl hours",
                "Burning midnight oil",
                "Can't sleep"
            )
            in 5..7 -> listOf(
                "Early start",
                "Rise and shine",
                "Up with the sun",
                "Fresh morning",
                "Early bird"
            )
            in 8..11 -> listOf(
                "Good morning",
                "Have a great day",
                "Ready to conquer",
                "Bright and early",
                "Morning vibes"
            )
            in 12..16 -> listOf(
                "Good afternoon",
                "Midday hustle",
                "Day going well",
                "Halfway through",
                "Afternoon grind"
            )
            in 17..21 -> listOf(
                "Good evening",
                "Unwinding time",
                "Evening calm",
                "Wrapping up the day",
                "Time to relax"
            )
            else -> listOf(
                "Winding down",
                "Calling it a night",
                "Rest well soon",
                "Late evening peace",
                "Ready for bed"
            )
        }
        greetings.random()
    }

    // ── Staggered entrance flag — true after first composition ──
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        entered = true
        viewModel.refresh()
    }



    val availableUpdate by viewModel.availableUpdate.collectAsState()
    val downloadState by viewModel.updateDownloadState.collectAsState()
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showUpdatePromptDialog by remember { mutableStateOf(false) }

    // When an update is detected, automatically prompt the user immediately!
    LaunchedEffect(availableUpdate) {
        if (availableUpdate != null && downloadState is UpdateDownloadState.Idle) {
            showUpdatePromptDialog = true
        }
    }

    LaunchedEffect(downloadState) {
        if (downloadState is UpdateDownloadState.Downloading || downloadState is UpdateDownloadState.ReadyToInstall) {
            showDownloadDialog = true
        }
    }

    val topPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .screenTabSwipe(
                onSwipeNext = onNavigateToSearch,
                onSwipePrev = {}
            )
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
                contentPadding = PaddingValues(top = topPadding + 16.dp, bottom = 120.dp)
            ) {
                // Top Header — slides down on entry
                item {
                    AnimatedVisibility(
                        visible = entered,
                        enter = slideInVertically(tween(320)) { -it / 2 } + fadeIn(tween(280))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Top Center Brand Logo
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_keepme_wordmark),
                                    contentDescription = "KeepMe Logo",
                                    modifier = Modifier.height(28.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFFFFB74D), Color(0xFFFF8A65))
                                                )
                                            ),
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
                                                text = currentUser?.firstName?.take(1) ?: "U",
                                                color = Color.White,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = greeting,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = currentUser?.let { "${it.firstName} ${it.lastName}" } ?: "User",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF2C2C2E),
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                haptics.click()
                                                onNavigateToSettings()
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.Settings,
                                                contentDescription = "Settings",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // OTA Update Available Banner
                if (availableUpdate != null) {
                    item {
                        AnimatedVisibility(
                            visible = entered,
                            enter = slideInVertically(tween(340)) { it / 3 } + fadeIn(tween(300))
                        ) {
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
                }

                // Hero Title — slides up with slight delay
                item {
                    AnimatedVisibility(
                        visible = entered,
                        enter = slideInVertically(tween(340, delayMillis = 60)) { it / 3 } + fadeIn(tween(300, delayMillis = 60))
                    ) {
                        Text(
                            text = "Your stash,\nsafe and sound.",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                        )
                    }
                }

                // Storage Dashboard — fades in with delay
                item {
                    AnimatedVisibility(
                        visible = entered,
                        enter = fadeIn(tween(360, delayMillis = 100)) + slideInVertically(tween(340, delayMillis = 100)) { it / 4 }
                    ) {
                        StorageDashboard(
                            stats = storageStats,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // Camera Backup Toggle
                item {
                    val isBackupEnabled by viewModel.isBackupEnabled.collectAsState()
                    val lastBackupTime by viewModel.lastBackupTime.collectAsState()

                    AnimatedVisibility(
                        visible = entered,
                        enter = fadeIn(tween(380, delayMillis = 140)) + slideInVertically(tween(360, delayMillis = 140)) { it / 4 }
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .clickable {
                                    haptics.click()
                                    onNavigateToSettings()
                                },
                            shape = RoundedCornerShape(24.dp), // More rounded pill-like
                            color = Color.White,
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically, 
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Rounded square icon with pale background
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isBackupEnabled) Color(0xFFE8F5E9) else Color(0xFFFFE0B2),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CloudUpload,
                                            contentDescription = null,
                                            tint = if (isBackupEnabled) Color(0xFF43A047) else Color(0xFFE65100),
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "Auto-Backup & Sync",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFF2C2C2E)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Icon(
                                                Icons.Rounded.ChevronRight,
                                                contentDescription = null,
                                                tint = Color(0xFF6C8FFF),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Text(
                                            text = if (isBackupEnabled && lastBackupTime > 0L) {
                                                "All caught up • Last: ${formatRelativeTime(lastBackupTime)}"
                                            } else if (isBackupEnabled) {
                                                "All caught up • Tap to configure"
                                            } else {
                                                "Paused • Tap to turn on"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFA0A0A5)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                TDriveSwitch(
                                    checked = isBackupEnabled,
                                    onCheckedChange = { enabled ->
                                        viewModel.toggleBackup(enabled)
                                    },
                                    activeColor = Color(0xFF00E5A0),
                                    inactiveColor = Color(0xFFE0E0E5)
                                )
                            }
                        }
                    }
                }
                // Recent Files Section
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Stuff",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        if (recentFiles.isNotEmpty()) {
                            TextButton(onClick = {
                                haptics.click()
                                onNavigateToRecentUploads()
                            }) {
                                Text("See all", color = TDriveColors.Primary)
                            }
                        }
                    }
                }

                if (recentFiles.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No recent files yet. Drop something in to get started!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TDriveColors.OnSurfaceDim
                            )
                        }
                    }
                } else {
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(recentFiles, key = { "${it.chatId}_${it.messageId}" }) { file ->
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

                // Favorite Files Section (if any)
                if (favoriteFiles.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Favorites",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
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

@Composable
fun FilterChip(
    label: String,
    count: Int,
    isSelected: Boolean
) {
    val chipColor by animateColorAsState(
        targetValue = if (isSelected) TDriveColors.Primary else Color.White,
        animationSpec = tween(TDriveMotion.chipSwitchMs),
        label = "filterChipColor"
    )

    Surface(
        shape = CircleShape,
        color = chipColor,
        modifier = Modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = TDriveColors.OnSurface
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = TDriveColors.OnSurface
            )
        }
    }
}
