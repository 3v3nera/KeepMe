package com.telegramdrive.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.telegramdrive.app.ui.screen.folders.FolderContentScreen
import com.telegramdrive.app.ui.screen.folders.FoldersScreen
import com.telegramdrive.app.ui.screen.home.HomeScreen
import com.telegramdrive.app.ui.screen.search.SearchScreen
import com.telegramdrive.app.ui.screen.trash.TrashScreen
import com.telegramdrive.app.ui.components.FloatingBottomBar
import com.telegramdrive.app.ui.components.LiquidGlassCreateFolderDialog
import com.telegramdrive.app.ui.theme.TDriveMotion
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import com.styropyr0.prismal.sources.prismalGlassLayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import com.telegramdrive.app.ui.screen.folders.FoldersViewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.draw.blur
import androidx.compose.runtime.mutableFloatStateOf
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import com.telegramdrive.app.ui.utils.rememberAppHaptics

@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backdrop = rememberPrismalGlassLayer()
    val foldersViewModel: FoldersViewModel = hiltViewModel()
    val localContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = rememberAppHaptics()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val currentChatId = navBackStackEntry?.arguments?.getString("chatId")?.toLongOrNull()

    var showCreateFolderAndUploadDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var pendingFolderName by remember { mutableStateOf<String?>(null) }
    var pendingFolderEmoji by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val folderToCreate = pendingFolderName
            val emojiToCreate = pendingFolderEmoji
            pendingFolderName = null
            pendingFolderEmoji = null
            if (folderToCreate != null) {
                coroutineScope.launch {
                    val newChatId = foldersViewModel.createFolderAndGetId(folderToCreate, emojiToCreate)
                    foldersViewModel.uploadFiles(uris, localContext, newChatId)
                }
            } else {
                foldersViewModel.uploadFiles(uris, localContext, currentChatId)
            }
        } else {
            pendingFolderName = null
            pendingFolderEmoji = null
        }
    }
    
    val items = listOf(
        BottomNavItem("Home", Screen.Home.route, Icons.Rounded.Home),
        BottomNavItem("Search", Screen.Search.route, Icons.Rounded.Search),
        BottomNavItem("Folders", Screen.Folders.route, Icons.Rounded.Folder)
    )

    // Effective route so Folders remains highlighted when browsing inside a folder
    val effectiveRoute = when {
        currentRoute?.startsWith("folder_content") == true -> Screen.Folders.route
        else -> currentRoute
    }

    // Dynamic scroll-aware visibility
    var isBottomBarVisible by remember { mutableStateOf(true) }
    var currentSelectionActions by remember { mutableStateOf<com.telegramdrive.app.ui.components.SelectionBarActions?>(null) }

    LaunchedEffect(currentRoute) {
        isBottomBarVisible = true
        currentSelectionActions = null
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -8f) {
                    // Scrolling down (finger moves up) -> hide bottom bar
                    isBottomBarVisible = false
                } else if (available.y > 8f) {
                    // Scrolling up (finger moves down) -> show bottom bar
                    isBottomBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    val targetColors = remember(effectiveRoute) {
        when (effectiveRoute) {
            Screen.Home.route -> listOf(
                com.telegramdrive.app.ui.theme.TDriveColors.Background,
                com.telegramdrive.app.ui.theme.TDriveColors.BackgroundAlt,
                androidx.compose.ui.graphics.Color(0xFF2C1635),
                androidx.compose.ui.graphics.Color(0xFF381528),
                com.telegramdrive.app.ui.theme.TDriveColors.Background,
                com.telegramdrive.app.ui.theme.TDriveColors.SurfaceElevated,
                com.telegramdrive.app.ui.theme.TDriveColors.BackgroundAlt,
                androidx.compose.ui.graphics.Color(0xFF4A1C3C),
                com.telegramdrive.app.ui.theme.TDriveColors.Background
            )
            Screen.Trash.route -> listOf(
                com.telegramdrive.app.ui.theme.TDriveColors.Background,
                com.telegramdrive.app.ui.theme.TDriveColors.BackgroundAlt,
                androidx.compose.ui.graphics.Color(0xFF3A1015),
                androidx.compose.ui.graphics.Color(0xFF2A080A),
                com.telegramdrive.app.ui.theme.TDriveColors.Background,
                com.telegramdrive.app.ui.theme.TDriveColors.SurfaceElevated,
                com.telegramdrive.app.ui.theme.TDriveColors.BackgroundAlt,
                androidx.compose.ui.graphics.Color(0xFF4A1015),
                com.telegramdrive.app.ui.theme.TDriveColors.Background
            )
            Screen.Folders.route -> listOf(
                com.telegramdrive.app.ui.theme.TDriveColors.Background,
                com.telegramdrive.app.ui.theme.TDriveColors.BackgroundAlt,
                androidx.compose.ui.graphics.Color(0xFF0F3025),
                androidx.compose.ui.graphics.Color(0xFF0A241B),
                com.telegramdrive.app.ui.theme.TDriveColors.Background,
                com.telegramdrive.app.ui.theme.TDriveColors.SurfaceElevated,
                com.telegramdrive.app.ui.theme.TDriveColors.BackgroundAlt,
                androidx.compose.ui.graphics.Color(0xFF144535),
                com.telegramdrive.app.ui.theme.TDriveColors.Background
            )
            Screen.Settings.route -> listOf(
                com.telegramdrive.app.ui.theme.TDriveColors.Background,
                com.telegramdrive.app.ui.theme.TDriveColors.BackgroundAlt,
                androidx.compose.ui.graphics.Color(0xFF162540),
                androidx.compose.ui.graphics.Color(0xFF101B30),
                com.telegramdrive.app.ui.theme.TDriveColors.Background,
                com.telegramdrive.app.ui.theme.TDriveColors.SurfaceElevated,
                com.telegramdrive.app.ui.theme.TDriveColors.BackgroundAlt,
                androidx.compose.ui.graphics.Color(0xFF1E3560),
                com.telegramdrive.app.ui.theme.TDriveColors.Background
            )
            else -> listOf(
                com.telegramdrive.app.ui.theme.TDriveColors.Background,
                com.telegramdrive.app.ui.theme.TDriveColors.BackgroundAlt,
                com.telegramdrive.app.ui.theme.TDriveColors.SurfaceElevated,
                com.telegramdrive.app.ui.theme.TDriveColors.PrimaryContainer,
                com.telegramdrive.app.ui.theme.TDriveColors.Background,
                com.telegramdrive.app.ui.theme.TDriveColors.SecondaryContainer,
                com.telegramdrive.app.ui.theme.TDriveColors.BackgroundAlt,
                com.telegramdrive.app.ui.theme.TDriveColors.Primary.copy(alpha = 0.2f),
                com.telegramdrive.app.ui.theme.TDriveColors.Background
            )
        }
    }

    var activeMediaViewer by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    var viewerDismissProgress by remember { mutableFloatStateOf(0f) }
    val targetBlur = if (activeMediaViewer != null) {
        28.dp * (1f - viewerDismissProgress).coerceIn(0f, 1f)
    } else {
        0.dp
    }
    val galleryBlur by androidx.compose.animation.core.animateDpAsState(
        targetValue = targetBlur,
        animationSpec = tween(120),
        label = "galleryBlur"
    )

    val isTrashScreen = currentRoute == Screen.Trash.route
    val isMediaViewer = currentRoute?.startsWith("media_viewer") == true
    val isSettingsScreen = currentRoute == Screen.Settings.route
    val isSplashFinished = com.telegramdrive.app.ui.screen.home.AppSessionState.hasPlayedSplash
    val shouldShowOnScreen = isSplashFinished && !isTrashScreen && !isMediaViewer && !isSettingsScreen && (activeMediaViewer == null) && currentRoute != null

    Box(modifier = modifier.fillMaxSize()) {
        com.telegramdrive.app.ui.components.AnimatedMeshBackground(targetColors = targetColors)

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                val effectiveBottomBarVisible = (shouldShowOnScreen && isBottomBarVisible) || (currentSelectionActions != null)
                AnimatedVisibility(
                    visible = effectiveBottomBarVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it * 2 },
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                    ) + fadeIn(tween(180)),
                    exit = slideOutVertically(
                        targetOffsetY = { it * 2 },
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                    ) + fadeOut(tween(150))
                ) {
                    FloatingBottomBar(
                        items = items.map { Triple(it.title, it.route, it.icon) },
                        currentRoute = effectiveRoute,
                        backdrop = backdrop,
                        onNavigate = { route ->
                            if (route == Screen.Folders.route && currentDestination?.route?.startsWith("folder_content") == true) {
                                if (!navController.popBackStack(Screen.Folders.route, inclusive = false)) {
                                    navController.navigate(Screen.Folders.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            } else {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        onUploadClick = {
                            if (currentRoute == Screen.Home.route ||
                                currentRoute == Screen.Search.route ||
                                currentRoute == Screen.Folders.route
                            ) {
                                showCreateFolderAndUploadDialog = true
                            } else {
                                filePickerLauncher.launch(arrayOf("*/*"))
                            }
                        },
                        selectionActions = currentSelectionActions,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        ) { innerPadding ->
        val context = androidx.compose.ui.platform.LocalContext.current
        val handleFileClick: (com.telegramdrive.app.domain.model.TelegramFile) -> Unit = { file ->
            val hasLocalFile = !file.localFilePath.isNullOrBlank() && java.io.File(file.localFilePath).exists()
            if (hasLocalFile && (file.fileType == com.telegramdrive.app.domain.model.FileType.DOCUMENT ||
                                 file.fileType == com.telegramdrive.app.domain.model.FileType.AUDIO ||
                                 file.fileType == com.telegramdrive.app.domain.model.FileType.OTHER)) {
                com.telegramdrive.app.ui.screen.preview.FileOpenUtils.openDocumentInExternalApp(context, file)
            } else {
                viewerDismissProgress = 0f
                activeMediaViewer = Pair(file.chatId, file.messageId)
            }
        }

        val navigateToTab: (String) -> Unit = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .prismalGlassLayer(backdrop)
                .then(if (activeMediaViewer != null) Modifier.blur(galleryBlur) else Modifier),
            // Default tab-switch transitions (cross-dissolve — no directional slide)
            enterTransition = { TDriveMotion.tabEnter },
            exitTransition  = { TDriveMotion.tabExit },
            popEnterTransition  = { TDriveMotion.tabEnter },
            popExitTransition   = { TDriveMotion.tabExit }
        ) {

            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToFolder = { folderName, chatId ->
                        navController.navigate(Screen.FolderContent.createRoute(folderName, chatId))
                    },
                    onNavigateToFolders = { navigateToTab(Screen.Folders.route) },
                    onNavigateToSearch = { navigateToTab(Screen.Search.route) },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToRecentUploads = {
                        navController.navigate(Screen.RecentUploads.route)
                    },
                    onFileClick = handleFileClick
                )
            }
            
            composable(Screen.Search.route) {
                SearchScreen(
                    onFileClick = handleFileClick,
                    onNavigateToHome = { navigateToTab(Screen.Home.route) },
                    onNavigateToFolders = { navigateToTab(Screen.Folders.route) }
                )
            }
            
            composable(Screen.Folders.route) {
                FoldersScreen(
                    onFolderClick = { folder ->
                        navController.navigate(Screen.FolderContent.createRoute(folder.name, folder.id))
                    },
                    onTrashClick = {
                        navController.navigate(Screen.Trash.route)
                    },
                    onNavigateToSearch = { navigateToTab(Screen.Search.route) },
                    onNewFolderClick = { showCreateFolderDialog = true }
                )
            }
            
            composable(
                route = Screen.Trash.route,
                enterTransition    = { TDriveMotion.screenEnter },
                exitTransition     = { TDriveMotion.screenExit },
                popEnterTransition = { TDriveMotion.screenPopEnter },
                popExitTransition  = { TDriveMotion.screenPopExit }
            ) {
                TrashScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            
            // ── Drill-down screens: slide in from the right, slide out to the right ──
            composable(
                route = Screen.FolderContent.route,
                arguments = listOf(
                    androidx.navigation.navArgument("folderName") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("chatId") { type = androidx.navigation.NavType.StringType }
                ),
                enterTransition    = { TDriveMotion.screenEnter },
                exitTransition     = {
                    if (targetState.destination.route?.startsWith("media_viewer") == true) {
                        fadeOut(tween(160))
                    } else {
                        TDriveMotion.screenExit
                    }
                },
                popEnterTransition = {
                    if (initialState.destination.route?.startsWith("media_viewer") == true) {
                        fadeIn(tween(180))
                    } else {
                        TDriveMotion.screenPopEnter
                    }
                },
                popExitTransition  = { TDriveMotion.screenPopExit }
            ) { backStackEntry ->
                val encodedFolderName = backStackEntry.arguments?.getString("folderName") ?: ""
                val folderName = android.net.Uri.decode(encodedFolderName)
                val chatId = backStackEntry.arguments?.getString("chatId")?.toLongOrNull() ?: 0L
                FolderContentScreen(
                    folderName = folderName,
                    onBack = { navController.popBackStack() },
                    onFileClick = handleFileClick,
                    onSelectionActionsChange = { actions ->
                        currentSelectionActions = actions
                    }
                )
            }
            
            composable(
                route = Screen.Settings.route,
                enterTransition    = { TDriveMotion.screenEnter },
                exitTransition     = { TDriveMotion.screenExit },
                popEnterTransition = { TDriveMotion.screenPopEnter },
                popExitTransition  = { TDriveMotion.screenPopExit }
            ) {
                com.telegramdrive.app.ui.screen.settings.SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.RecentUploads.route,
                enterTransition    = { TDriveMotion.screenEnter },
                exitTransition     = { TDriveMotion.screenExit },
                popEnterTransition = { TDriveMotion.screenPopEnter },
                popExitTransition  = { TDriveMotion.screenPopExit }
            ) {
                com.telegramdrive.app.ui.screen.home.RecentUploadsScreen(
                    onBack = { navController.popBackStack() },
                    onFileClick = handleFileClick,
                    onSelectionActionsChange = { actions ->
                        currentSelectionActions = actions
                    }
                )
            }

            composable(
                route = Screen.MediaViewer.route,
                arguments = listOf(
                    androidx.navigation.navArgument("folderId") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("fileId") { type = androidx.navigation.NavType.StringType }
                ),
                enterTransition    = { TDriveMotion.overlayEnter },
                exitTransition     = { fadeOut(tween(160)) },
                popEnterTransition = { TDriveMotion.overlayEnter },
                popExitTransition  = { fadeOut(tween(160)) }
            ) { backStackEntry ->
                val fId = backStackEntry.arguments?.getString("folderId")?.toLongOrNull() ?: 0L
                val mId = backStackEntry.arguments?.getString("fileId")?.toLongOrNull() ?: 0L
                key(mId) {
                    com.telegramdrive.app.ui.screen.preview.FullScreenViewerScreen(
                        folderId = fId,
                        fileId = mId,
                        onClose = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    if (showCreateFolderDialog) {
        LiquidGlassCreateFolderDialog(
            backdrop = backdrop,
            title = "Create Folder",
            subtitle = "Make a new private spot in your cloud for your files",
            confirmText = "Create",
            onConfirm = { name, emoji ->
                foldersViewModel.createFolder(name, emoji)
                showCreateFolderDialog = false
            },
            onDismiss = { showCreateFolderDialog = false }
        )
    }

    if (showCreateFolderAndUploadDialog) {
        LiquidGlassCreateFolderDialog(
            backdrop = backdrop,
            title = "Create & Upload",
            subtitle = "Name your new folder to start dropping files into it:",
            confirmText = "Upload Files",
            onConfirm = { name, emoji ->
                pendingFolderName = name
                pendingFolderEmoji = emoji
                showCreateFolderAndUploadDialog = false
                filePickerLauncher.launch(arrayOf("*/*"))
            },
            onDismiss = { showCreateFolderAndUploadDialog = false }
        )
    }

    // ── LIVE MEDIA VIEWER OVERLAY DIRECTLY OVER GALLERY ──
    AnimatedVisibility(
        visible = activeMediaViewer != null,
        enter = fadeIn(tween(160)),
        exit = fadeOut(tween(140))
    ) {
        val target = activeMediaViewer
        if (target != null) {
            BackHandler {
                activeMediaViewer = null
                viewerDismissProgress = 0f
            }

            key(target.second) {
                com.telegramdrive.app.ui.screen.preview.FullScreenViewerScreen(
                    folderId = target.first,
                    fileId = target.second,
                    onSwipeProgress = { progress ->
                        viewerDismissProgress = progress
                    },
                    onClose = {
                        activeMediaViewer = null
                        viewerDismissProgress = 0f
                    }
                )
            }
        }
    }
    }
}
data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
