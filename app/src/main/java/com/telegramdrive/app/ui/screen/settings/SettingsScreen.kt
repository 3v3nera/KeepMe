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
        containerColor = TDriveColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── TOP BAR HEADER: "< Profile" ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = TDriveColors.SurfaceElevated,
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(onClick = {
                        haptics.click()
                        onBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── 1. MAIN PROFILE CARD ──
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(26.dp),
                color = TDriveColors.SurfaceElevated,
                border = BorderStroke(1.dp, TDriveColors.Outline)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Avatar + Name + Phone + Edit Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar: Rounded squircle with Telegram profile photo
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF5A8BFF), Color(0xFF85B6FF))
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
                                    text = currentUser?.firstName?.take(1) ?: "A",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            val displayName = remember(currentUser) {
                                val first = currentUser?.firstName.orEmpty()
                                val last = currentUser?.lastName.orEmpty()
                                val full = "$first $last".trim()
                                if (full.isNotEmpty()) full else "Cloud User"
                            }
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val phoneOrUser = remember(currentUser) {
                                val user = currentUser
                                val phone = user?.phoneNumber
                                val activeUsername = user?.usernames?.activeUsernames?.firstOrNull()
                                if (!phone.isNullOrBlank()) {
                                    "+$phone"
                                } else if (!activeUsername.isNullOrBlank()) {
                                    "@$activeUsername"
                                } else {
                                    "Telegram Vault"
                                }
                            }
                            Text(
                                text = phoneOrUser,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TDriveColors.OnSurfaceDim
                            )
                        }

                        IconButton(
                            onClick = {
                                haptics.click()
                                showPrivacyDialog = true
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = "Edit Profile",
                                tint = TDriveColors.OnSurfaceDim,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Streak Pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF2A201C),
                        border = BorderStroke(0.5.dp, Color(0xFFFF8A3D).copy(alpha = 0.35f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "🔥 12",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFFF9E59)
                            )
                            Text(
                                text = " | Cloud Streak",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = Color(0xFFD4C8C2)
                            )
                        }
                    }

                    // Row of 3 Stat Counters with REAL CLOUD TITLES
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Stat 1: Total Files
                        StatBox(
                            value = "$totalFilesCount",
                            label = "Files",
                            icon = Icons.Rounded.Description,
                            iconColor = Color(0xFFFF6D3A),
                            modifier = Modifier.weight(1f)
                        )

                        // Stat 2: Folders
                        StatBox(
                            value = "${foldersCount.coerceAtLeast(1)}",
                            label = "Folders",
                            icon = Icons.Rounded.Folder,
                            iconColor = Color(0xFFA855F7),
                            modifier = Modifier.weight(1f)
                        )

                        // Stat 3: Cloud Sync Status
                        StatBox(
                            value = if (isBackupEnabled) "100%" else "Ready",
                            label = "Cloud Sync",
                            icon = Icons.Rounded.CloudDone,
                            iconColor = Color(0xFF00E5A0),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── 2. AUTO-BACKUP & SYNC CARD (KEPT DIRECTLY OUTSIDE ON SCREEN) ──
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                color = TDriveColors.SurfaceElevated,
                border = BorderStroke(1.dp, TDriveColors.Outline)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header with master toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TDriveColors.PrimaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.CloudUpload,
                                    contentDescription = null,
                                    tint = TDriveColors.Primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Auto-Backup & Sync",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                "Sync photos & videos to 'Camera Backup [TG]'",
                                style = MaterialTheme.typography.bodySmall,
                                color = TDriveColors.OnSurfaceDim
                            )
                        }
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
                            activeColor = TDriveColors.Primary
                        )
                    }

                    HorizontalDivider(color = TDriveColors.Outline.copy(alpha = 0.5f))

                    // Daily Backup Time Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptics.click()
                                showTimePickerDialog = true
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = TDriveColors.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    "Daily Backup Time",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White
                                )
                                Text(
                                    "Runs daily at ${formatTime(backupHour, backupMinute)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TDriveColors.Primary.copy(alpha = 0.9f)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = TDriveColors.PrimaryContainer,
                            border = BorderStroke(1.dp, TDriveColors.Primary.copy(alpha = 0.5f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = formatTime(backupHour, backupMinute),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TDriveColors.Primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Rounded.Edit,
                                    contentDescription = null,
                                    tint = TDriveColors.Primary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = TDriveColors.Outline.copy(alpha = 0.5f))

                    // Backup Scope Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Backup Scope",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (backupScope == "FROM_TODAY") TDriveColors.PrimaryContainer else TDriveColors.Surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (backupScope == "FROM_TODAY") TDriveColors.Primary else TDriveColors.OutlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        haptics.click()
                                        viewModel.setBackupScope("FROM_TODAY")
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        "From Today",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (backupScope == "FROM_TODAY") TDriveColors.Primary else Color.White
                                    )
                                    Text("New media only", style = MaterialTheme.typography.bodySmall, color = TDriveColors.OnSurfaceDim)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (backupScope == "ALL_TIME") TDriveColors.PrimaryContainer else TDriveColors.Surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (backupScope == "ALL_TIME") TDriveColors.Primary else TDriveColors.OutlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        haptics.click()
                                        viewModel.setBackupScope("ALL_TIME")
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        "Day One",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (backupScope == "ALL_TIME") TDriveColors.Primary else Color.White
                                    )
                                    Text("Entire camera roll", style = MaterialTheme.typography.bodySmall, color = TDriveColors.OnSurfaceDim)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = TDriveColors.Outline.copy(alpha = 0.5f))

                    // Upload on Mobile Data
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Upload on Mobile Data", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        TDriveSwitch(
                            checked = !isWifiOnly,
                            onCheckedChange = { viewModel.toggleMobileData(it) },
                            activeColor = TDriveColors.Primary
                        )
                    }

                    // While Charging Only
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("While Charging Only", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        TDriveSwitch(
                            checked = isChargingOnly,
                            onCheckedChange = { viewModel.toggleChargingOnly(it) },
                            activeColor = TDriveColors.Primary
                        )
                    }

                    // Background Reliability Status
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (batteryExemptState) TDriveColors.Secondary.copy(alpha = 0.12f) else Color(0xFFFFB74D).copy(alpha = 0.12f),
                        border = BorderStroke(
                            1.dp,
                            if (batteryExemptState) TDriveColors.Secondary.copy(alpha = 0.3f) else Color(0xFFFFB74D).copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    if (batteryExemptState) Icons.Rounded.CheckCircle else Icons.Rounded.BatteryAlert,
                                    contentDescription = null,
                                    tint = if (batteryExemptState) TDriveColors.Secondary else Color(0xFFFFB74D),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (batteryExemptState) "Unrestricted Background Active" else "Battery Optimized (May sleep)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (batteryExemptState) TDriveColors.Secondary else Color(0xFFFFB74D)
                                )
                            }
                            if (!batteryExemptState) {
                                TextButton(onClick = { requestBatteryOptimizationExemption() }) {
                                    Text("Allow", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFFFB74D))
                                }
                            }
                        }
                    }

                    // Action Button: Back Up Camera Photos Now
                    Button(
                        onClick = {
                            haptics.click()
                            viewModel.triggerBackupNow()
                        },
                        enabled = !isBackingUpNow,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TDriveColors.Primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isBackingUpNow) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Backing up camera photos...", fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Back Up Camera Photos Now", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── 3. STANDALONE "APPEARANCE" CARD ──
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { haptics.click() },
                shape = RoundedCornerShape(18.dp),
                color = TDriveColors.SurfaceElevated,
                border = BorderStroke(1.dp, TDriveColors.Outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.LightMode,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = TDriveColors.OnSurfaceDim,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 4. "YOUR INFORMATION" / PREFERENCES SECTION WITH REAL TITLES ──
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Your Information",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = TDriveColors.OnSurfaceDim,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = TDriveColors.SurfaceElevated,
                    border = BorderStroke(1.dp, TDriveColors.Outline)
                ) {
                    Column {
                        // Custom Folder Backup
                        SettingsNavRow(
                            icon = Icons.Rounded.FolderSpecial,
                            iconBg = Color(0xFFFF3366),
                            title = "Custom Folder Backup",
                            subtitle = customFolderName ?: "Choose custom device directory",
                            onClick = {
                                haptics.click()
                                showCustomFolderSheet = true
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 58.dp),
                            color = TDriveColors.Outline.copy(alpha = 0.5f)
                        )

                        // Your Space & Cache
                        SettingsNavRow(
                            icon = Icons.Rounded.PieChart,
                            iconBg = Color(0xFF00C48C),
                            title = "Your Space & Cache",
                            subtitle = "Storage visualizer and disk cleaner",
                            onClick = {
                                haptics.click()
                                showStorageSheet = true
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 58.dp),
                            color = TDriveColors.Outline.copy(alpha = 0.5f)
                        )

                        // App Updates & Releases
                        SettingsNavRow(
                            icon = Icons.Rounded.SystemUpdate,
                            iconBg = Color(0xFFFF9500),
                            title = "App Updates & Releases",
                            subtitle = "v${BuildConfig.VERSION_NAME} • @$updateChannelUsername",
                            onClick = {
                                haptics.click()
                                showUpdatesSheet = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 5. "OTHER INFORMATION" SECTION ──
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Other Information",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = TDriveColors.OnSurfaceDim,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = TDriveColors.SurfaceElevated,
                    border = BorderStroke(1.dp, TDriveColors.Outline)
                ) {
                    Column {
                        SettingsNavRow(
                            icon = Icons.Rounded.Lock,
                            iconBg = Color(0xFF3F4450),
                            title = "Account Privacy & Security",
                            subtitle = "Telegram MTProto and session credentials",
                            onClick = {
                                haptics.click()
                                showPrivacyDialog = true
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 58.dp),
                            color = TDriveColors.Outline.copy(alpha = 0.5f)
                        )

                        SettingsNavRow(
                            icon = Icons.AutoMirrored.Rounded.Logout,
                            iconBg = Color(0xFF9333EA),
                            title = "Log Out",
                            subtitle = "Disconnect Telegram Cloud session",
                            onClick = {
                                haptics.click()
                                showLogoutConfirmDialog = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }

    // ── SHEET: CUSTOM FOLDER BACKUP ──
    if (showCustomFolderSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCustomFolderSheet = false },
            containerColor = TDriveColors.Background,
            dragHandle = { BottomSheetDefaults.DragHandle(color = TDriveColors.Outline) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Custom Folder Backup", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text("Choose any directory on your phone to back up to Telegram.", style = MaterialTheme.typography.bodySmall, color = TDriveColors.OnSurfaceDim)

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = TDriveColors.SurfaceElevated,
                    border = BorderStroke(1.dp, TDriveColors.OutlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Selected Directory:", style = MaterialTheme.typography.labelMedium, color = TDriveColors.OnSurfaceDim)
                        Text(
                            text = customFolderName ?: "No folder selected yet",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = if (customFolderName != null) TDriveColors.Secondary else Color(0xFFFFB74D)
                        )
                        Button(
                            onClick = { openFolderLauncher.launch(null) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TDriveColors.SecondaryContainer, contentColor = TDriveColors.Secondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (customFolderName == null) "Choose Folder" else "Change Folder")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Backup For Folder", style = MaterialTheme.typography.bodyLarge, color = Color.White)
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

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // ── SHEET: STORAGE & CACHE ──
    if (showStorageSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStorageSheet = false },
            containerColor = TDriveColors.Background,
            dragHandle = { BottomSheetDefaults.DragHandle(color = TDriveColors.Outline) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Your Space & Cache", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)

                // Storage visualizer with SOLID PHYSICS (zero overlap)
                StorageDashboard(stats = storageStats)

                Button(
                    onClick = {
                        haptics.click()
                        val cacheDir = context.cacheDir
                        cacheDir.deleteRecursively()
                        showStorageSheet = false
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TDriveColors.SurfaceElevated, contentColor = Color.White),
                    border = BorderStroke(1.dp, TDriveColors.OutlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFFF6D3A))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Local Cache")
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // ── SHEET: APP UPDATES ──
    if (showUpdatesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showUpdatesSheet = false },
            containerColor = TDriveColors.Background,
            dragHandle = { BottomSheetDefaults.DragHandle(color = TDriveColors.Outline) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("App Updates & Releases", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = TDriveColors.SurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Installed Version", style = MaterialTheme.typography.bodySmall, color = TDriveColors.OnSurfaceDim)
                            Text("v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TDriveColors.Primary.copy(alpha = 0.15f)
                        ) {
                            Text("Latest", color = TDriveColors.Primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                        }
                    }
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

                Button(
                    onClick = {
                        haptics.click()
                        viewModel.checkForUpdates()
                    },
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isCheckingUpdate,
                    colors = ButtonDefaults.buttonColors(containerColor = TDriveColors.Primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Checking @$updateChannelUsername...")
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check for Updates", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // ── PRIVACY DIALOG ──
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Close", color = TDriveColors.Primary)
                }
            },
            title = { Text("Account Privacy & Security", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your files are transmitted directly between your phone and the official Telegram MTProto infrastructure.", style = MaterialTheme.typography.bodyMedium, color = TDriveColors.OnSurfaceDim)
                    Text("End-to-end sessions are handled locally on device. No middleman servers ever view or store your files.", style = MaterialTheme.typography.bodyMedium, color = TDriveColors.OnSurfaceDim)
                }
            },
            containerColor = TDriveColors.SurfaceElevated
        )
    }

    // ── LOGOUT CONFIRMATION DIALOG ──
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.click()
                        showLogoutConfirmDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Cancel", color = TDriveColors.OnSurfaceDim)
                }
            },
            title = { Text("Log out of Telegram?", fontWeight = FontWeight.Bold) },
            text = { Text("This will clear your credentials and end your active cloud session.", color = TDriveColors.OnSurfaceDim) },
            containerColor = TDriveColors.SurfaceElevated
        )
    }

    // ── TIME PICKER DIALOG ──
    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = backupHour,
            initialMinute = backupMinute,
            is24Hour = false
        )
        Dialog(onDismissRequest = { showTimePickerDialog = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = TDriveColors.SurfaceElevated,
                border = BorderStroke(1.dp, TDriveColors.Outline)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text("Set Daily Backup Time", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = TDriveColors.Surface,
                            selectorColor = TDriveColors.Primary,
                            containerColor = TDriveColors.Surface,
                            periodSelectorBorderColor = TDriveColors.Outline,
                            periodSelectorSelectedContainerColor = TDriveColors.PrimaryContainer,
                            periodSelectorSelectedContentColor = TDriveColors.Primary,
                            periodSelectorUnselectedContentColor = TDriveColors.OnSurface,
                            timeSelectorSelectedContainerColor = TDriveColors.PrimaryContainer,
                            timeSelectorSelectedContentColor = TDriveColors.Primary,
                            timeSelectorUnselectedContainerColor = TDriveColors.Surface,
                            timeSelectorUnselectedContentColor = Color.White
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showTimePickerDialog = false }) {
                            Text("Cancel", color = TDriveColors.OnSurfaceDim)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.setBackupTime(timePickerState.hour, timePickerState.minute)
                                showTimePickerDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TDriveColors.Primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Time", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
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
        color = Color(0xFF191F2D),
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
