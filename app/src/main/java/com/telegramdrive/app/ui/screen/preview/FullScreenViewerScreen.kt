package com.telegramdrive.app.ui.screen.preview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.telegramdrive.app.domain.model.FileType
import com.telegramdrive.app.ui.theme.TDriveMotion
import com.telegramdrive.app.ui.utils.rememberAppHaptics
import coil.compose.AsyncImage
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.prismalGlassEffects
import com.styropyr0.prismal.shapes.PrismalCapsule
import com.styropyr0.prismal.shapes.PrismalRoundedRectangle
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenViewerScreen(
    folderId: Long? = null,
    fileId: Long? = null,
    viewModel: MediaViewerViewModel = hiltViewModel(key = folderId?.toString() ?: "viewer"),
    onSwipeProgress: ((Float) -> Unit)? = null,
    onClose: () -> Unit
) {
    LaunchedEffect(folderId, fileId) {
        if (folderId != null && fileId != null) {
            viewModel.setTarget(folderId, fileId)
        }
    }

    val files by viewModel.files.collectAsState()

    val targetIndex = remember(files, fileId) {
        if (fileId != null && files.isNotEmpty()) {
            files.indexOfFirst { it.messageId == fileId }.takeIf { it >= 0 } ?: 0
        } else {
            0
        }
    }

    if (files.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = com.telegramdrive.app.ui.theme.TDriveColors.Primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = targetIndex,
        pageCount = { files.size }
    )

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val appHaptics = rememberAppHaptics()
    val backdrop = rememberPrismalGlassLayer()

    // ── Swipe-down-to-dismiss state ──
    val swipeOffsetY = remember { Animatable(0f) }
    val dismissThreshold = 280f
    // Tracks whether haptic has fired for this drag gesture
    var hapticFiredThisDrag by remember { mutableStateOf(false) }

    // Track whether the current image is zoomed in
    var currentPageScale by remember { mutableFloatStateOf(1f) }

    // ── Info strip + chrome visibility (toggle on single tap) ──
    var showInfo by remember { mutableStateOf(false) }
    var showChrome by remember { mutableStateOf(true) }

    LaunchedEffect(pagerState.settledPage) {
        showInfo = false
        showChrome = true
    }

    val dismissProgress = (swipeOffsetY.value / dismissThreshold).coerceIn(0f, 1f)
    val bgAlpha = (1f - dismissProgress).coerceIn(0f, 1f)
    val chromeAlpha = (1f - dismissProgress * 2.5f).coerceIn(0f, 1f)

    LaunchedEffect(dismissProgress) {
        onSwipeProgress?.invoke(dismissProgress)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070E).copy(alpha = 0.55f * bgAlpha))
    ) {
        // ── 1. Stationary Ambient Backdrop (Pins to screen, blends during horizontal swipe, fades on pull-down) ──
        val currentPage = pagerState.currentPage
        val offsetFraction = pagerState.currentPageOffsetFraction
        val nextPageIndex = when {
            offsetFraction > 0.001f && currentPage < files.size - 1 -> currentPage + 1
            offsetFraction < -0.001f && currentPage > 0 -> currentPage - 1
            else -> currentPage
        }
        val horizontalProgress = abs(offsetFraction).coerceIn(0f, 1f)

        val currentFileForBg = files.getOrNull(currentPage)
        val currentModel = currentFileForBg?.thumbnailPath?.takeIf { java.io.File(it).exists() }
            ?: currentFileForBg?.localFilePath?.takeIf { java.io.File(it).exists() }

        val nextFileForBg = files.getOrNull(nextPageIndex)
        val nextModel = nextFileForBg?.thumbnailPath?.takeIf { java.io.File(it).exists() }
            ?: nextFileForBg?.localFilePath?.takeIf { java.io.File(it).exists() }

        // Current photo ambient background
        if (currentModel != null) {
            val fileObj = java.io.File(currentModel)
            AsyncImage(
                model = fileObj,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(50.dp)
                    .graphicsLayer {
                        alpha = (1f - horizontalProgress) * 0.45f * bgAlpha
                    }
            )
        }

        // Blending adjacent photo ambient background
        if (nextModel != null && nextModel != currentModel) {
            val nextFileObj = java.io.File(nextModel)
            AsyncImage(
                model = nextFileObj,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(50.dp)
                    .graphicsLayer {
                        alpha = horizontalProgress * 0.45f * bgAlpha
                    }
            )
        }

        // ── 2. Pager with swipe-down-to-dismiss (Only foreground content translates) ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .prismalGlassLayer(backdrop)
                .offset { IntOffset(0, swipeOffsetY.value.roundToInt()) }
                .graphicsLayer {
                    alpha = 1f - dismissProgress * 0.35f
                    scaleX = 1f - dismissProgress * 0.12f
                    scaleY = 1f - dismissProgress * 0.12f
                }
                .pointerInput(currentPageScale, showInfo) {
                    if (currentPageScale > 1.1f) return@pointerInput
                    val touchSlop = viewConfiguration.touchSlop

                    awaitEachGesture {
                        val firstDown = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                        var totalX = 0f
                        var totalY = 0f
                        var isVertical: Boolean? = null
                        var triggeredSwipeUp = false
                        var triggeredSwipeDownCloseInfo = false

                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull() ?: break

                            if (change.pressed) {
                                val panX = change.position.x - change.previousPosition.x
                                val panY = change.position.y - change.previousPosition.y
                                totalX += panX
                                totalY += panY

                                // Determine whether user intended a vertical swipe (swiping up/down)
                                if (isVertical == null) {
                                    val absX = abs(totalX)
                                    val absY = abs(totalY)
                                    if (absX > touchSlop * 0.3f || absY > touchSlop * 0.3f) {
                                        // Forgiving angle: any swipe roughly 35° or steeper from horizontal is treated as VERTICAL
                                        isVertical = absY >= absX * 0.7f
                                    }
                                }

                                if (isVertical == true) {
                                    // Consume in Initial pass: HorizontalPager will NEVER see or react to this swipe!
                                    change.consume()

                                    if (totalY < -35f && !showInfo && !triggeredSwipeUp) {
                                        triggeredSwipeUp = true
                                        showInfo = true
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } else if (totalY > 35f && showInfo && !triggeredSwipeDownCloseInfo) {
                                        triggeredSwipeDownCloseInfo = true
                                        showInfo = false
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } else if (totalY > 0f && !showInfo) {
                                        coroutineScope.launch {
                                            swipeOffsetY.snapTo(totalY)
                                        }
                                        if (totalY >= dismissThreshold && !hapticFiredThisDrag) {
                                            appHaptics.heavyClick()
                                            hapticFiredThisDrag = true
                                        } else if (totalY < dismissThreshold) {
                                            hapticFiredThisDrag = false
                                        }
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        // Gesture completed
                        if (isVertical == true) {
                            coroutineScope.launch {
                                if (totalY < -35f && !showInfo) {
                                    showInfo = true
                                } else if (totalY > 35f && showInfo) {
                                    showInfo = false
                                } else if (!showInfo && swipeOffsetY.value > dismissThreshold) {
                                    swipeOffsetY.animateTo(2000f, tween(200))
                                    onClose()
                                } else {
                                    swipeOffsetY.animateTo(0f, TDriveMotion.dismissDragSpring)
                                }
                                hapticFiredThisDrag = false
                            }
                        }
                    }
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                userScrollEnabled = currentPageScale <= 1.1f,
                key = { files[it].messageId }
            ) { pageIndex ->
                val file = files[pageIndex]
                val isCurrentPage = pagerState.settledPage == pageIndex

                when (file.fileType) {
                    FileType.IMAGE -> ImagePreviewPage(
                        file = file,
                        onScaleChanged = { if (pageIndex == pagerState.settledPage) currentPageScale = it },
                        onTap = {
                            showInfo = !showInfo
                            showChrome = !showChrome
                        }
                    )
                    FileType.VIDEO -> VideoPreviewPage(file = file, isCurrentPage = isCurrentPage)
                    else -> DocumentPreviewPage(file = file)
                }
            }
        }

        // ── Close button — fades with chrome ──
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn(tween(160)),
            exit  = fadeOut(tween(140)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 40.dp, start = 8.dp)
                .graphicsLayer { alpha = chromeAlpha }
        ) {
            IconButton(
                onClick = {
                    appHaptics.click()
                    onClose()
                },
                modifier = Modifier.size(48.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close viewer",
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        // ── Page indicator — fades with chrome ──
        if (files.size > 1) {
            AnimatedVisibility(
                visible = showChrome,
                enter = fadeIn(tween(160)),
                exit  = fadeOut(tween(140)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 44.dp, end = 16.dp)
                    .graphicsLayer { alpha = chromeAlpha }
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${files.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // ── Bottom info strip (tap to toggle) ──
        val currentFile = files.getOrNull(pagerState.settledPage)
        AnimatedVisibility(
            visible = showInfo && currentFile != null,
            enter = fadeIn(tween(200)) + slideInVertically(tween(240)) { it },
            exit  = fadeOut(tween(160)) + slideOutVertically(tween(200)) { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer { alpha = chromeAlpha }
        ) {
            if (currentFile != null) {
                MediaDetailsGlassSheet(
                    file = currentFile,
                    backdrop = backdrop,
                    context = context
                )
            }
        }

        // ── Snackbar host ──
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 180.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Prismal Liquid Glass Media Details Sheet (Matching FloatingBottomBar)
// ─────────────────────────────────────────────────────────────
@Composable
private fun MediaDetailsGlassSheet(
    file: com.telegramdrive.app.domain.model.TelegramFile,
    backdrop: com.styropyr0.prismal.PrismalBackdrop,
    context: android.content.Context,
    viewModel: MediaPreviewViewModel = hiltViewModel(key = file.messageId.toString())
) {
    val density = LocalDensity.current
    val appHaptics = rememberAppHaptics()
    val localPath by viewModel.localPath.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()

    LaunchedEffect(saveResult) {
        if (saveResult?.startsWith("Saved") == true) {
            appHaptics.success()
        }
    }

    LaunchedEffect(file.messageId) {
        viewModel.prepareFile(file)
    }

    val dateText = remember(file.effectiveDate) {
        com.telegramdrive.app.utils.FileDateUtils.formatFullDateTime(file.effectiveDate)
    }

    val sizeText = remember(file.fileSize) {
        when {
            file.fileSize >= 1_048_576L -> "%.1f MB".format(file.fileSize / 1_048_576.0)
            file.fileSize >= 1024L      -> "%.1f KB".format(file.fileSize / 1024.0)
            else                        -> "${file.fileSize} B"
        }
    }

    val fileExt = remember(file.fileName) {
        val ext = file.fileName.substringAfterLast('.', "")
        if (ext.isNotBlank()) ext.uppercase() else when (file.fileType) {
            FileType.IMAGE -> "IMG"
            FileType.VIDEO -> "VID"
            FileType.AUDIO -> "AUDIO"
            else -> "DOC"
        }
    }

    val fileIcon = remember(file.fileType) {
        when (file.fileType) {
            FileType.IMAGE -> Icons.Rounded.Image
            FileType.VIDEO -> Icons.Rounded.Videocam
            else -> Icons.Rounded.InsertDriveFile
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 20.dp)
            .navigationBarsPadding()
            .shadow(12.dp, RoundedCornerShape(28.dp))
            .drawPrismalGlass(
                backdrop = backdrop,
                shape = { PrismalRoundedRectangle(28.dp) },
                effects = prismalGlassEffects(density)
            )
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // ── Drag pill handle ──
            Box(
                modifier = Modifier
                    .size(width = 38.dp, height = 4.dp)
                    .background(Color.White.copy(alpha = 0.45f), CircleShape)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(14.dp))

            // ── Header: Icon + File Name + Upload Date ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = fileIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.fileName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Metadata chips row ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Size Badge (Electric Emerald in Monospace)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = sizeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = Color(0xFF00E5A0),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }

                // Format Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = fileExt,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = Color(0xFF6C8FFF),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }

                // Cloud Storage Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cloud,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Telegram Cloud",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Action Buttons Row ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Save to Gallery / Download (Solid Electric Blue)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .shadow(8.dp, CircleShape)
                        .background(
                            color = if (saveResult?.startsWith("Saved") == true) Color(0xFF00E5A0) else Color(0xFF6C8FFF),
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .clickable(enabled = !isSaving) {
                            appHaptics.click()
                            viewModel.saveToGallery(context, file)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Saving to your phone…",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        } else if (saveResult?.startsWith("Saved") == true) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Saved to Gallery! 🎉",
                                color = Color.Black,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Save to Gallery",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }

                // Share button (Solid Elevated Surface)
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .shadow(6.dp, CircleShape)
                        .background(
                            color = Color(0xFF1E283C),
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .clickable {
                            appHaptics.click()
                            val activePath = localPath ?: file.localFilePath
                            FileOpenUtils.shareFileInExternalApp(
                                context,
                                if (activePath != null) file.copy(localFilePath = activePath) else file
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
