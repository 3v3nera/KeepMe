package com.telegramdrive.app.ui.screen.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.telegramdrive.app.BuildConfig
import com.telegramdrive.app.domain.update.UpdateDownloadState
import com.telegramdrive.app.ui.components.TDriveSwitch
import com.telegramdrive.app.ui.screen.home.StorageDashboard
import com.telegramdrive.app.ui.screen.home.components.LiveUpdateDownloadDialog
import com.telegramdrive.app.ui.screen.home.components.UpdateAvailableBanner
import com.telegramdrive.app.ui.theme.TDriveColors
import com.telegramdrive.app.ui.utils.rememberAppHaptics
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val haptics = rememberAppHaptics()
    val context = LocalContext.current

    // User & Statistics
    val currentUser by viewModel.currentUser.collectAsState()
    val userProfilePhotoPath by viewModel.userProfilePhotoPath.collectAsState()
    val totalFilesCount by viewModel.totalFilesCount.collectAsState()
    val foldersCount by viewModel.foldersCount.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()

    // Backup & Sync
    val isBackupEnabled by viewModel.isBackupEnabled.collectAsState()
    val isWifiOnly by viewModel.isWifiOnly.collectAsState()
    val isChargingOnly by viewModel.isChargingOnly.collectAsState()
    val backupHour by viewModel.backupHour.collectAsState()
    val backupMinute by viewModel.backupMinute.collectAsState()
    val backupScope by viewModel.backupScope.collectAsState()
    val lastBackupTime by viewModel.lastBackupTime.collectAsState()
    val isBackingUpNow by viewModel.isBackingUpNow.collectAsState()

    // Custom Folder
    val isCustomFolderEnabled by viewModel.isCustomFolderBackupEnabled.collectAsState()
    val customFolderName by viewModel.customFolderName.collectAsState()

    // Updates
    val updateChannelUsername by viewModel.updateChannelUsername.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val availableUpdate by viewModel.availableUpdate.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val manualCheckMessage by viewModel.manualCheckMessage.collectAsState()

    // Dialog & Sheet states
    var showCustomFolderSheet by remember { mutableStateOf(false) }
    var showStorageSheet by remember { mutableStateOf(false) }
    var showUpdatesSheet by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    LaunchedEffect(downloadState) {
        if (downloadState is UpdateDownloadState.Downloading || downloadState is UpdateDownloadState.ReadyToInstall) {
            showDownloadDialog = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.reloadUserProfile()
    }

    // Media permissions for backup
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    val mediaPermissionsState = rememberMultiplePermissionsState(permissions = permissionsToRequest)

    // Storage Access Framework launcher for custom folder selection
    val openFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {}
            val docFile = DocumentFile.fromTreeUri(context, uri)
            val name = docFile?.name ?: "Custom Folder"
            viewModel.setCustomFolder(uri.toString(), name)
            viewModel.toggleCustomFolderBackup(true)
        }
    }

    val checkBatteryExemption: () -> Boolean = {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }
    var batteryExemptState by remember { mutableStateOf(checkBatteryExemption()) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                batteryExemptState = checkBatteryExemption()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val requestBatteryOptimizationExemption: () -> Unit = {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.telegramdrive.app.ui.components.ScreenBackButton(onBack = onBack)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Settings",
                    style = com.telegramdrive.app.ui.components.screenTitleStyle(),
                    color = TDriveColors.OnCanvas
                )
            }

            // ── Profile: avatar, name, then three plain stats (no boxes) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(TDriveColors.SurfaceElevated),
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
                            text = currentUser?.firstName?.take(1)?.uppercase() ?: "K",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TDriveColors.OnCanvas
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val displayName = remember(currentUser) {
                        val full = "${currentUser?.firstName.orEmpty()} ${currentUser?.lastName.orEmpty()}".trim()
                        if (full.isNotEmpty()) full else "Cloud User"
                    }
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TDriveColors.OnCanvas,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val phoneOrUser = remember(currentUser) {
                        val phone = currentUser?.phoneNumber
                        val username = currentUser?.usernames?.activeUsernames?.firstOrNull()
                        when {
                            !phone.isNullOrBlank() -> "+$phone"
                            !username.isNullOrBlank() -> "@$username"
                            else -> "Telegram Vault"
                        }
                    }
                    Text(
                        text = phoneOrUser,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TDriveColors.OnCanvasMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                ProfileStat("$totalFilesCount", "Files", Modifier.weight(1f))
                ProfileStat("${foldersCount.coerceAtLeast(1)}", "Folders", Modifier.weight(1f))
                ProfileStat(if (isBackupEnabled) "On" else "Off", "Backup", Modifier.weight(1f), accent = isBackupEnabled)
            }

            // ── Backup ──
            SettingsSectionLabel("Backup")
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Rounded.CloudUpload,
                    title = "Auto-backup",
                    subtitle = when {
                        !isBackupEnabled -> "Off"
                        lastBackupTime > 0L -> "On · last run ${com.telegramdrive.app.utils.formatRelativeTime(lastBackupTime)}"
                        else -> "On · waiting for the first run"
                    },
                    trailing = {
                        TDriveSwitch(
                            checked = isBackupEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (mediaPermissionsState.allPermissionsGranted) {
                                        viewModel.toggleBackup(true)
                                        showTimePickerDialog = true
                                    } else {
                                        mediaPermissionsState.launchMultiplePermissionRequest()
                                    }
                                } else {
                                    viewModel.toggleBackup(false)
                                }
                            },
                            activeColor = TDriveColors.Accent
                        )
                    }
                )
                val dim = if (isBackupEnabled) 1f else 0.45f
                SettingsRow(
                    icon = Icons.Rounded.Schedule,
                    title = "Daily time",
                    value = formatTime(backupHour, backupMinute),
                    showChevron = true,
                    modifier = Modifier.alpha(dim),
                    onClick = {
                        haptics.click()
                        showTimePickerDialog = true
                    }
                )
                SettingsRow(
                    icon = Icons.Rounded.PhotoLibrary,
                    title = "Scope",
                    modifier = Modifier.alpha(dim),
                    trailing = {
                        SegmentedPill(
                            options = listOf("From today", "Everything"),
                            selectedIndex = if (backupScope == "ALL_TIME") 1 else 0,
                            onSelect = { index ->
                                haptics.click()
                                viewModel.setBackupScope(if (index == 1) "ALL_TIME" else "FROM_TODAY")
                            }
                        )
                    }
                )
                SettingsRow(
                    icon = Icons.Rounded.SignalCellularAlt,
                    title = "Use mobile data",
                    subtitle = "Otherwise Wi-Fi only",
                    modifier = Modifier.alpha(dim),
                    trailing = {
                        TDriveSwitch(
                            checked = !isWifiOnly,
                            onCheckedChange = { viewModel.toggleMobileData(it) },
                            activeColor = TDriveColors.Accent
                        )
                    }
                )
                SettingsRow(
                    icon = Icons.Rounded.BatteryChargingFull,
                    title = "Only while charging",
                    modifier = Modifier.alpha(dim),
                    trailing = {
                        TDriveSwitch(
                            checked = isChargingOnly,
                            onCheckedChange = { viewModel.toggleChargingOnly(it) },
                            activeColor = TDriveColors.Accent
                        )
                    }
                )
                SettingsRow(
                    icon = if (batteryExemptState) Icons.Rounded.CheckCircle else Icons.Rounded.BatteryAlert,
                    title = "Background access",
                    value = if (batteryExemptState) "Unrestricted" else "Limited",
                    valueColor = if (batteryExemptState) TDriveColors.Accent else Color(0xFFFFB74D),
                    showChevron = !batteryExemptState,
                    showDivider = false,
                    onClick = if (batteryExemptState) null else ({
                        haptics.click()
                        requestBatteryOptimizationExemption()
                    })
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            com.telegramdrive.app.ui.components.GlassPrimaryButton(
                text = if (isBackingUpNow) "Backing up…" else "Back up now",
                loading = isBackingUpNow,
                onClick = {
                    haptics.click()
                    viewModel.triggerBackupNow()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            // ── Library ──
            SettingsSectionLabel("Library")
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Rounded.FolderSpecial,
                    title = "Custom folder backup",
                    value = customFolderName ?: "Not set",
                    showChevron = true,
                    onClick = {
                        haptics.click()
                        showCustomFolderSheet = true
                    }
                )
                SettingsRow(
                    icon = Icons.Rounded.PieChart,
                    title = "Storage & cache",
                    value = com.telegramdrive.app.ui.components.formatFileSize(storageStats.sumOf { it.totalSize }),
                    showChevron = true,
                    onClick = {
                        haptics.click()
                        showStorageSheet = true
                    }
                )
                SettingsRow(
                    icon = Icons.Rounded.SystemUpdate,
                    title = "App updates",
                    value = if (availableUpdate != null) "Update available" else "v${BuildConfig.VERSION_NAME}",
                    valueColor = if (availableUpdate != null) TDriveColors.Accent else TDriveColors.OnCanvasMuted,
                    showChevron = true,
                    showDivider = false,
                    onClick = {
                        haptics.click()
                        showUpdatesSheet = true
                    }
                )
            }

            // ── Account ──
            SettingsSectionLabel("Account")
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Rounded.Lock,
                    title = "Privacy & security",
                    showChevron = true,
                    onClick = {
                        haptics.click()
                        showPrivacyDialog = true
                    }
                )
                SettingsRow(
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    title = "Log out",
                    destructive = true,
                    showDivider = false,
                    onClick = {
                        haptics.click()
                        showLogoutConfirmDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(NavBarBottomInset))
        }
    }

    // ── SHEET: CUSTOM FOLDER BACKUP ──
    if (showCustomFolderSheet) {
        com.telegramdrive.app.ui.components.GlassSheetDialog(
            title = "Custom Folder Backup",
            subtitle = "Choose any directory on your phone to back up to Telegram.",
            onDismiss = { showCustomFolderSheet = false }
        ) {
            com.telegramdrive.app.ui.components.GlassInfoBlock {
                Text("Selected directory", style = MaterialTheme.typography.labelMedium, color = com.telegramdrive.app.ui.components.GlassPanelTokens.TextSecondary)
                Text(
                    text = customFolderName ?: "No folder selected yet",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = if (customFolderName != null) TDriveColors.Secondary else Color(0xFFFFB74D)
                )
            }
            com.telegramdrive.app.ui.components.GlassActionRow(
                icon = Icons.Rounded.FolderOpen,
                title = if (customFolderName == null) "Choose Folder" else "Change Folder",
                onClick = { openFolderLauncher.launch(null) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable backup for folder", style = MaterialTheme.typography.bodyLarge, color = com.telegramdrive.app.ui.components.GlassPanelTokens.TextPrimary)
                TDriveSwitch(
                    checked = isCustomFolderEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && customFolderName.isNullOrBlank()) {
                            openFolderLauncher.launch(null)
                        } else {
                            viewModel.toggleCustomFolderBackup(enabled)
                        }
                    },
                    activeColor = TDriveColors.Secondary
                )
            }
        }
    }

    // ── SHEET: STORAGE & CACHE ──
    if (showStorageSheet) {
        com.telegramdrive.app.ui.components.GlassSheetDialog(
            title = "Your Space & Cache",
            onDismiss = { showStorageSheet = false }
        ) {
            StorageDashboard(stats = storageStats)
            com.telegramdrive.app.ui.components.GlassActionRow(
                icon = Icons.Rounded.CleaningServices,
                title = "Clear Local Cache",
                tint = Color(0xFFFF8A65),
                onClick = {
                    haptics.click()
                    context.cacheDir.deleteRecursively()
                    showStorageSheet = false
                }
            )
        }
    }

    // ── SHEET: APP UPDATES ──
    if (showUpdatesSheet) {
        com.telegramdrive.app.ui.components.GlassSheetDialog(
            title = "App Updates & Releases",
            onDismiss = { showUpdatesSheet = false }
        ) {
            com.telegramdrive.app.ui.components.GlassInfoBlock {
                Text("Installed version", style = MaterialTheme.typography.bodySmall, color = com.telegramdrive.app.ui.components.GlassPanelTokens.TextSecondary)
                Text(
                    "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                    fontWeight = FontWeight.Medium,
                    color = com.telegramdrive.app.ui.components.GlassPanelTokens.TextPrimary
                )
            }

            if (availableUpdate != null) {
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

            com.telegramdrive.app.ui.components.GlassActionRow(
                icon = Icons.Rounded.Refresh,
                title = if (isCheckingUpdate) "Checking @$updateChannelUsername..." else "Check for Updates",
                onClick = {
                    if (!isCheckingUpdate) {
                        haptics.click()
                        viewModel.checkForUpdates()
                    }
                }
            )
        }
    }

    // ── PRIVACY DIALOG ──
    if (showPrivacyDialog) {
        com.telegramdrive.app.ui.components.GlassAlertDialog(
            title = "Account Privacy & Security",
            message = "Your files are transmitted directly between your phone and the official Telegram MTProto infrastructure.\n\nEnd-to-end sessions are handled locally on device. No middleman servers ever view or store your files.",
            dismissText = "Close",
            onDismiss = { showPrivacyDialog = false }
        )
    }

    // ── LOGOUT CONFIRMATION DIALOG ──
    if (showLogoutConfirmDialog) {
        com.telegramdrive.app.ui.components.GlassAlertDialog(
            title = "Log out of Telegram?",
            message = "This will clear your credentials and end your active cloud session.",
            confirmText = "Log Out",
            destructive = true,
            onConfirm = {
                haptics.click()
                showLogoutConfirmDialog = false
                viewModel.logout()
            },
            onDismiss = { showLogoutConfirmDialog = false }
        )
    }

    // ── TIME PICKER DIALOG ──
    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = backupHour,
            initialMinute = backupMinute,
            is24Hour = false
        )
        com.telegramdrive.app.ui.components.GlassSheetDialog(
            title = "Set Daily Backup Time",
            doneText = "Cancel",
            onDismiss = { showTimePickerDialog = false },
            actions = {
                com.telegramdrive.app.ui.components.GlassPrimaryButton(
                    text = "Save Time",
                    onClick = {
                        viewModel.setBackupTime(timePickerState.hour, timePickerState.minute)
                        showTimePickerDialog = false
                    }
                )
            }
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = com.telegramdrive.app.ui.components.GlassPanelTokens.PillFill,
                        selectorColor = Color.White,
                        containerColor = Color.Transparent,
                        periodSelectorBorderColor = Color.White.copy(alpha = 0.2f),
                        clockDialSelectedContentColor = Color(0xFF0B0F1A),
                        clockDialUnselectedContentColor = Color.White,
                        periodSelectorSelectedContainerColor = Color.White,
                        periodSelectorUnselectedContainerColor = Color.Transparent,
                        periodSelectorSelectedContentColor = Color(0xFF0B0F1A),
                        periodSelectorUnselectedContentColor = Color.White,
                        timeSelectorSelectedContainerColor = Color.White,
                        timeSelectorUnselectedContainerColor = com.telegramdrive.app.ui.components.GlassPanelTokens.PillFill,
                        timeSelectorSelectedContentColor = Color(0xFF0B0F1A),
                        timeSelectorUnselectedContentColor = Color.White
                    )
                )
            }
        }
    }

    // ── LIVE UPDATE DOWNLOAD DIALOG ──
    if (showDownloadDialog && availableUpdate != null) {
        LiveUpdateDownloadDialog(
            updateInfo = availableUpdate!!,
            downloadState = downloadState,
            onCancelClick = {
                viewModel.cancelUpdateDownload(availableUpdate!!.fileId)
                showDownloadDialog = false
            },
            onInstallClick = {
                if (downloadState is UpdateDownloadState.ReadyToInstall) {
                    viewModel.installApk((downloadState as UpdateDownloadState.ReadyToInstall).apkFile)
                }
            },
            onDismissClick = {
                showDownloadDialog = false
                viewModel.resetDownloadState()
            },
            onRetryClick = {
                viewModel.startUpdateDownload(availableUpdate!!)
            }
        )
    }
}

// ── STAT BOX COMPONENT (3 IN ROW ON PROFILE CARD) ──
@Composable
private fun StatBox(
    value: String,
    label: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = TDriveColors.SurfaceElevated,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = TDriveColors.OnSurfaceDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── GROUPED NAVIGATION ROW COMPONENT ──
@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = iconBg,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TDriveColors.OnSurfaceDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = TDriveColors.OnSurfaceDim,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val ampm = if (hour >= 12) "PM" else "AM"
    val displayHour = when (hour % 12) {
        0 -> 12
        else -> hour % 12
    }
    return String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, ampm)
}

private val NavBarBottomInset = 36.dp

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
        color = TDriveColors.OnCanvasMuted,
        modifier = Modifier.padding(start = 28.dp, end = 24.dp, top = 28.dp, bottom = 10.dp)
    )
}

/** Rounded card whose rows are separated by hairlines. */
@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(TDriveColors.Surface)
            .border(1.dp, TDriveColors.LineSoft, RoundedCornerShape(24.dp)),
        content = content
    )
}

/** One settings line: icon, title (+ subtitle), then a value, a control, or a chevron. */
@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    valueColor: Color = TDriveColors.OnCanvasMuted,
    showChevron: Boolean = false,
    showDivider: Boolean = true,
    destructive: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val tint = if (destructive) TDriveColors.Error else TDriveColors.OnCanvas
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (destructive) TDriveColors.Error else TDriveColors.OnCanvasMuted,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = tint
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TDriveColors.OnCanvasMuted
                    )
                }
            }
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = valueColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 150.dp)
                )
            }
            trailing?.invoke()
            if (showChevron) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = TDriveColors.OnCanvasFaint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (showDivider) {
            Box(
                Modifier
                    .padding(start = 54.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(TDriveColors.LineSoft)
            )
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            ),
            color = if (accent) TDriveColors.Accent else TDriveColors.OnCanvas
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TDriveColors.OnCanvasMuted
        )
    }
}

/** Compact two-option switch used for "From today / Everything". */
@Composable
private fun SegmentedPill(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(TDriveColors.FillSoft)
            .padding(3.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) Color(0xFF0B0C0F) else TDriveColors.OnCanvasMuted,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (selected) TDriveColors.OnCanvas else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }
    }
}