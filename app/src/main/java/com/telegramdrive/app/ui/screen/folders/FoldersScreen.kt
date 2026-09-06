package com.telegramdrive.app.ui.screen.folders

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import com.telegramdrive.app.ui.components.screenTabSwipe
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.telegramdrive.app.domain.model.TelegramFolder
import com.telegramdrive.app.ui.theme.TDriveColors
import com.telegramdrive.app.ui.theme.TDriveMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalDensity
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.prismalGlassEffects
import com.styropyr0.prismal.sources.prismalGlassLayer
import com.styropyr0.prismal.shapes.PrismalCapsule
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import com.telegramdrive.app.ui.components.LiquidGlassCreateFolderDialog
import com.telegramdrive.app.ui.components.LiquidGlassFolderOptionsDialog
import com.telegramdrive.app.ui.components.TactileFolderCard
import com.telegramdrive.app.ui.components.TactileTrashFolderCard
import com.telegramdrive.app.ui.utils.rememberAppHaptics

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FoldersScreen(
    viewModel: FoldersViewModel = hiltViewModel(),
    onFolderClick: (TelegramFolder) -> Unit,
    onTrashClick: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onNewFolderClick: () -> Unit = {}
) {
    val haptics = rememberAppHaptics()
    val folders by viewModel.folders.collectAsState()

    var selectedFolderForOptions by remember { mutableStateOf<TelegramFolder?>(null) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .screenTabSwipe(
                onSwipeNext = {},
                onSwipePrev = onNavigateToSearch
            ),
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Custom Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Drive",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            spotColor = Color(0xFF3863E5).copy(alpha = 0.40f)
                        )
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(
                            width = 1.2.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.75f),
                                    Color.White.copy(alpha = 0.20f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .clickable {
                            haptics.click()
                            onNewFolderClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CreateNewFolder,
                        contentDescription = "New Folder",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            val savedMessages = folders.firstOrNull { it.isSavedMessages }
            val customFolders = folders.filter { !it.isSavedMessages }

            // Track folders that have already executed their entrance animation once
            val animatedFolderKeys = remember { mutableSetOf<String>() }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 130.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Saved Messages (Root cloud storage)
                if (savedMessages != null) {
                    item(key = "saved_messages") {
                        val key = "saved_messages"
                        val hasAnimated = key in animatedFolderKeys
                        val alpha = remember { Animatable(if (hasAnimated) 1f else 0f) }
                        val translateY = remember { Animatable(if (hasAnimated) 0f else 20f) }

                        LaunchedEffect(key) {
                            if (!hasAnimated) {
                                coroutineScope {
                                    launch { alpha.animateTo(1f, tween(240)) }
                                    launch { translateY.animateTo(0f, tween(260)) }
                                }
                                animatedFolderKeys.add(key)
                            }
                        }

                        TactileFolderCard(
                            folder = savedMessages,
                            onClick = { onFolderClick(savedMessages) },
                            onLongClick = { selectedFolderForOptions = savedMessages },
                            modifier = Modifier.graphicsLayer {
                                this.alpha = alpha.value
                                translationY = translateY.value
                            }
                        )
                    }
                }

                // 2. Trash (Placed directly near Saved Messages)
                item(key = "trash_folder") {
                    val key = "trash_folder"
                    val hasAnimated = key in animatedFolderKeys
                    val alpha = remember { Animatable(if (hasAnimated) 1f else 0f) }
                    val translateY = remember { Animatable(if (hasAnimated) 0f else 20f) }

                    LaunchedEffect(key) {
                        if (!hasAnimated) {
                            val delayMs = 40L
                            kotlinx.coroutines.delay(delayMs)
                            coroutineScope {
                                launch { alpha.animateTo(1f, tween(240)) }
                                launch { translateY.animateTo(0f, tween(260)) }
                            }
                            animatedFolderKeys.add(key)
                        }
                    }

                    TactileTrashFolderCard(
                        onClick = onTrashClick,
                        modifier = Modifier.graphicsLayer {
                            this.alpha = alpha.value
                            translationY = translateY.value
                        }
                    )
                }

                // 3. Custom User Folders
                itemsIndexed(customFolders, key = { _, f -> f.id }) { index, folder ->
                    val key = "folder_${folder.id}"
                    val hasAnimated = key in animatedFolderKeys
                    val alpha = remember { Animatable(if (hasAnimated) 1f else 0f) }
                    val translateY = remember { Animatable(if (hasAnimated) 0f else 20f) }

                    LaunchedEffect(key) {
                        if (!hasAnimated) {
                            val delayMs = ((index + 2) * 40L).coerceAtMost(300L)
                            kotlinx.coroutines.delay(delayMs)
                            coroutineScope {
                                launch { alpha.animateTo(1f, tween(240)) }
                                launch { translateY.animateTo(0f, tween(260)) }
                            }
                            animatedFolderKeys.add(key)
                        }
                    }

                    TactileFolderCard(
                        folder = folder,
                        onClick = { onFolderClick(folder) },
                        onLongClick = { selectedFolderForOptions = folder },
                        modifier = Modifier.graphicsLayer {
                            this.alpha = alpha.value
                            translationY = translateY.value
                        }
                    )
                }
            }
        }


        // Liquid Glass Folder Options Dialog (Rename, Sticker, Delete)
        if (selectedFolderForOptions != null) {
            LiquidGlassFolderOptionsDialog(
                folder = selectedFolderForOptions!!,
                onRename = { newName, newEmoji ->
                    val folder = selectedFolderForOptions ?: return@LiquidGlassFolderOptionsDialog
                    viewModel.renameFolder(
                        chatId = folder.id,
                        newName = newName,
                        emoji = newEmoji
                    )
                },
                onDelete = {
                    val folder = selectedFolderForOptions ?: return@LiquidGlassFolderOptionsDialog
                    viewModel.deleteFolder(folder.id)
                },
                onDismiss = {
                    selectedFolderForOptions = null
                }
            )
        }
    }
}

@Composable
fun GrainyGradientBox(
    modifier: Modifier = Modifier,
    colors: List<Color>,
    content: @Composable BoxScope.() -> Unit
) {
    val noiseBrush = remember {
        val width = 64
        val height = 64
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val alpha = kotlin.random.Random.nextInt(15, 35)
                bitmap.setPixel(x, y, android.graphics.Color.argb(alpha, 0, 50, 20)) // Dark greenish noise
            }
        }
        val shader = android.graphics.BitmapShader(bitmap, android.graphics.Shader.TileMode.REPEAT, android.graphics.Shader.TileMode.REPEAT)
        androidx.compose.ui.graphics.ShaderBrush(shader)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(colors))
            .background(noiseBrush),
        contentAlignment = Alignment.Center,
        content = content
    )
}
