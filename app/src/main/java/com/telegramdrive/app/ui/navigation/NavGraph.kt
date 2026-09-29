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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import com.telegramdrive.app.ui.components.LocalNavBarBounds
import com.telegramdrive.app.ui.components.NavBarBounds
import androidx.compose.runtime.CompositionLocalProvider
import com.telegramdrive.app.ui.components.folderContainerTransition

@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backdrop = rememberPrismalGlassLayer()
    val navBarBounds = remember { mutableStateOf(NavBarBounds()) }
    val foldersViewModel: FoldersViewModel = hiltViewModel()
    val localContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = rememberAppHaptics()

    LaunchedEffect(Unit) {
        foldersViewModel.events.collect { msg ->
            android.widget.Toast.makeText(localContext, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val currentChatId = navBackStackEntry?.arguments?.getString("chatId")?.toLongOrNull()

    var showCreateFolderAndUploadDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderForOptions by remember { mutableStateOf<com.telegramdrive.app.domain.model.TelegramFolder?>(null) }
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
                    if (newChatId != null) foldersViewModel.uploadFiles(uris, localContext, newChatId)
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
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            // Use what content actually consumed so pull-to-refresh overscroll and
            // non-scrolling gestures never toggle the bar
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput) {
                    if (consumed.y < -8f) {
                        // Scrolling down (finger moves up) -> hide bottom bar
                        isBottomBarVisible = false
                    } else if (consumed.y > 8f) {
                        // Scrolling up (finger moves down) -> show bottom bar
                        isBottomBarVisible = true
                    }
                }
                return Offset.Zero
            }
        }
    }

    var activeMediaViewer by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    var viewerDismissProgress by remember { mutableFloatStateOf(0f) }
    val targetBlur = when {
        // No blur behind the viewer: its scrim is ~opaque, and re-blurring the whole app every
        // frame of a swipe-to-dismiss was the main source of jank there
        activeMediaViewer != null -> 0.dp
        showCreateFolderDialog || showCreateFolderAndUploadDialog || folderForOptions != null -> 24.dp
        else -> 0.dp
    }
    val screenContentBlur by androidx.compose.animation.core.animateDpAsState(
        targetValue = targetBlur,
        animationSpec = tween(200),
        label = "screenContentBlur"
    )

    val isTrashScreen = currentRoute == Screen.Trash.route
    val isMediaViewer = currentRoute?.startsWith("media_viewer") == true
    val isSettingsScreen = currentRoute == Screen.Settings.route
    val isSplashFinished = com.telegramdrive.app.ui.screen.home.AppSessionState.hasPlayedSplash
    val isAnyFolderDialogShowing = showCreateFolderDialog || showCreateFolderAndUploadDialog || folderForOptions != null
    val shouldShowOnScreen = isSplashFinished && !isTrashScreen && !isMediaViewer && !isSettingsScreen && (activeMediaViewer == null) && !isAnyFolderDialogShowing && currentRoute != null

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                val effectiveBottomBarVisible = (shouldShowOnScreen && isBottomBarVisible) ||
                    (currentSelectionActions != null && activeMediaViewer == null && !isMediaViewer)
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
                        isFoldersTab = (currentRoute == Screen.Folders.route),
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
                        onBoundsChanged = { bounds -> navBarBounds.value = bounds },
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

        // Ignore taps while a transition is running: a double tap must not push a screen twice
        // or pop past the previous one
        val canNavigate: () -> Boolean = {
            navController.currentBackStackEntry?.lifecycle?.currentState == androidx.lifecycle.Lifecycle.State.RESUMED
        }
        val navigateTo: (String) -> Unit = { route -> if (canNavigate()) navController.navigate(route) }
        val goBack: () -> Unit = { if (canNavigate()) navController.popBackStack() }

        val navigateToTab: (String) -> Unit = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        CompositionLocalProvider(LocalNavBarBounds provides navBarBounds) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .prismalGlassLayer(backdrop)
                .then(if (screenContentBlur > 0.dp) Modifier.blur(screenContentBlur) else Modifier)
        ) {
            com.telegramdrive.app.ui.components.CanvasBackground()

            @OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
            androidx.compose.animation.SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
            CompositionLocalProvider(com.telegramdrive.app.ui.components.LocalSharedTransitionScope provides this) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize(),
                // Default tab-switch transitions (cross-dissolve — no directional slide)
                enterTransition = { TDriveMotion.tabEnter },
                exitTransition  = { TDriveMotion.tabExit },
                popEnterTransition  = { TDriveMotion.tabEnter },
                popExitTransition   = { TDriveMotion.tabExit }
            ) {

            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToFolder = { folderName, chatId ->
                        navigateTo(Screen.FolderContent.createRoute(folderName, chatId))
                    },
                    onNavigateToFolders = { navigateToTab(Screen.Folders.route) },
                    onNavigateToSearch = { navigateToTab(Screen.Search.route) },
                    onNavigateToSettings = {
                        navigateTo(Screen.Settings.route)
                    },
                    onNavigateToRecentUploads = {
                        navigateTo(Screen.RecentUploads.route)
                    },
                    onNavigateToTrash = { navigateTo(Screen.Trash.route) },
                    onNewFolderClick = { showCreateFolderDialog = true },
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
            
            composable(
                Screen.Folders.route,
                // The folder card expands into the folder screen (shared bounds), so just dissolve here
                exitTransition = {
                    if (targetState.destination.route?.startsWith("folder_content") == true) fadeOut(tween(160)) else TDriveMotion.tabExit
                },
                popEnterTransition = {
                    if (initialState.destination.route?.startsWith("folder_content") == true) fadeIn(tween(260)) else TDriveMotion.tabEnter
                }
            ) {
            CompositionLocalProvider(com.telegramdrive.app.ui.components.LocalNavAnimatedScope provides this) {
                FoldersScreen(
                    backdrop = backdrop,
                    onFolderClick = { folder ->
                        navigateTo(Screen.FolderContent.createRoute(folder.name, folder.id))
                    },
                    onTrashClick = {
                        navigateTo(Screen.Trash.route)
                    },
                    onNavigateToSearch = { navigateToTab(Screen.Search.route) },
                    onNewFolderClick = { showCreateFolderDialog = true },
                    onFolderOptions = { folderForOptions = it }
                )
            }
            }
            
            composable(
                route = Screen.Trash.route,
                enterTransition    = { TDriveMotion.screenEnter },
                exitTransition     = { TDriveMotion.screenExit },
                popEnterTransition = { TDriveMotion.screenPopEnter },
                popExitTransition  = { TDriveMotion.screenPopExit }
            ) {
                TrashScreen(
                    onBack = goBack
                )
            }
            
            // ── Drill-down screens: slide in from the right, slide out to the right ──
            composable(
                route = Screen.FolderContent.route,
                arguments = listOf(
                    androidx.navigation.navArgument("folderName") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("chatId") { type = androidx.navigation.NavType.StringType }
                ),
                enterTransition    = {
                    if (initialState.destination.route == Screen.Folders.route) androidx.compose.animation.EnterTransition.None
                    else TDriveMotion.screenEnter
                },
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
                popExitTransition  = {
                    if (targetState.destination.route == Screen.Folders.route) fadeOut(tween(200))
                    else TDriveMotion.screenPopExit
                }
            ) { backStackEntry ->
                val encodedFolderName = backStackEntry.arguments?.getString("folderName") ?: ""
                val folderName = android.net.Uri.decode(encodedFolderName)
                val chatId = backStackEntry.arguments?.getString("chatId")?.toLongOrNull() ?: 0L
                CompositionLocalProvider(com.telegramdrive.app.ui.components.LocalNavAnimatedScope provides this) {
                // The whole screen is the shared container: it grows out of the tapped card (and shrinks back into it)
                Box(modifier = Modifier.fillMaxSize().folderContainerTransition(chatId)) {
                com.telegramdrive.app.ui.components.CanvasBackground()
                FolderContentScreen(
                    folderName = folderName,
                    chatId = chatId,
                    onBack = goBack,
                    onFileClick = handleFileClick,
                    onNewFolderClick = { showCreateFolderDialog = true },
                    onSelectionActionsChange = { actions ->
                        currentSelectionActions = actions
                    },
                    backdrop = backdrop
                )
                }
                }
            }
            
            composable(
                route = Screen.Settings.route,
                enterTransition    = { TDriveMotion.screenEnter },
                exitTransition     = { TDriveMotion.screenExit },
                popEnterTransition = { TDriveMotion.screenPopEnter },
                popExitTransition  = { TDriveMotion.screenPopExit }
            ) {
                com.telegramdrive.app.ui.screen.settings.SettingsScreen(
                    onBack = goBack
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
                    onBack = goBack,
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
            }

            // Soft fade under the status bar so scrolling content never collides with the clock/icons
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 10.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                com.telegramdrive.app.ui.theme.TDriveColors.Canvas.copy(alpha = 0.92f),
                                androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    )
            )
    }
    }
}

    // Popups live outside the glass-source layer. Each is wrapped in AnimatedVisibility so it
    // fades/settles out instead of vanishing; the options popup remembers its folder for the exit.
    val popupExit = fadeOut(tween(150)) + androidx.compose.animation.scaleOut(targetScale = 0.97f, animationSpec = tween(150))
    val lastOptionsFolder = remember { mutableStateOf<com.telegramdrive.app.domain.model.TelegramFolder?>(null) }
    if (folderForOptions != null) lastOptionsFolder.value = folderForOptions

    AnimatedVisibility(
        visible = folderForOptions != null,
        enter = androidx.compose.animation.EnterTransition.None,
        exit = popupExit
    ) {
        lastOptionsFolder.value?.let { folder ->
            com.telegramdrive.app.ui.components.LiquidGlassFolderOptionsDialog(
                folder = folder,
                backdrop = null,
                onRename = { newName, newEmoji ->
                    foldersViewModel.renameFolder(chatId = folder.id, newName = newName, emoji = newEmoji)
                },
                onDelete = { foldersViewModel.deleteFolder(folder.id) },
                onDismiss = { folderForOptions = null }
            )
        }
    }

    AnimatedVisibility(
        visible = showCreateFolderDialog,
        enter = androidx.compose.animation.EnterTransition.None,
        exit = popupExit
    ) {
        LiquidGlassCreateFolderDialog(
            backdrop = null,
            title = "Create Folder",
            subtitle = if (currentRoute?.startsWith("folder_content") == true)
                "Telegram has no nested folders, so this is added next to your others"
            else "Make a new private spot in your cloud for your files",
            confirmText = "Create",
            onConfirm = { name, emoji ->
                foldersViewModel.createFolder(name, emoji)
                showCreateFolderDialog = false
            },
            onDismiss = { showCreateFolderDialog = false }
        )
    }

    AnimatedVisibility(
        visible = showCreateFolderAndUploadDialog,
        enter = androidx.compose.animation.EnterTransition.None,
        exit = popupExit
    ) {
        LiquidGlassCreateFolderDialog(
            backdrop = null,
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
