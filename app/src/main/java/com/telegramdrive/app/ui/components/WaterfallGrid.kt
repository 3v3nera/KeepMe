package com.telegramdrive.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telegramdrive.app.domain.model.TelegramFile
import com.telegramdrive.app.ui.theme.TDriveColors
import com.telegramdrive.app.ui.utils.rememberAppHaptics
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Items that already ran their entrance animation this session (kept out of saved state). */
private val animatedItemKeys = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())

@Composable
fun WaterfallGrid(
    files: List<TelegramFile>,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    isLoading: Boolean = false,
    selectedFileIds: Set<Long> = emptySet(),
    inSelectionMode: Boolean = false,
    groupByMonth: Boolean = true,
    onFileClick: (TelegramFile) -> Unit,
    onFileLongPress: (TelegramFile) -> Unit,
    onFileDragSelect: (TelegramFile) -> Unit = {},
    onFileVisible: (TelegramFile) -> Unit = {},
    onEmptyAreaLongPress: (Offset) -> Unit = {},
    /** Opt-in empty message (only shown when [isLoading] is false), e.g. "This folder is empty". */
    emptyText: String = "",
    itemBadge: ((TelegramFile) -> String?)? = null,
    /** Identity used for selection; message ids repeat across chats, so cross-folder lists override this. */
    selectionKey: (TelegramFile) -> Long = { it.messageId },
    modifier: Modifier = Modifier
) {
    val haptics = rememberAppHaptics()

    var initialLoadCompleted by rememberSaveable { mutableStateOf(files.isNotEmpty()) }
    LaunchedEffect(files.isNotEmpty()) {
        if (files.isNotEmpty()) {
            initialLoadCompleted = true
        }
    }

    // Group files by month (January, February, March, etc.) using true date taken
    val monthGroups = remember(files, groupByMonth) {
        if (!groupByMonth || files.isEmpty()) {
            listOf("All Files" to files)
        } else {
            files.groupBy { com.telegramdrive.app.utils.FileDateUtils.formatMonthHeader(it.effectiveDate) }.toList()
        }
    }

    Box(modifier = modifier) {
        if (emptyText.isNotEmpty() && !isLoading && files.isEmpty()) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyLarge,
                color = GlassPanelTokens.TextSecondary,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        LazyVerticalStaggeredGrid(
            state = gridState,
            columns = StaggeredGridCells.Fixed(2),
            verticalItemSpacing = 8.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = NavBarMetrics.contentBottomPadding),
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLoading && files.isEmpty()) {
                items(10) {
                    ShimmerFileCard()
                }
            } else {
                monthGroups.forEach { (monthName, groupFiles) ->
                    if (groupByMonth && monthGroups.size > 0 && files.isNotEmpty()) {
                        item(
                            key = "header_${monthName}",
                            span = StaggeredGridItemSpan.FullLine
                        ) {
                            MonthDividerHeader(
                                monthName = monthName,
                                count = groupFiles.size
                            )
                        }
                    }

                    items(
                        count = groupFiles.size,
                        key = { "${groupFiles[it].chatId}_${groupFiles[it].messageId}" }
                    ) { index ->
                        val file = groupFiles[index]

                        // ── Staggered first-load entry only on initial fetch ──
                        val itemKey = "${file.chatId}_${file.messageId}"
                        var hasAnimated by remember(itemKey) {
                            mutableStateOf(initialLoadCompleted || itemKey in animatedItemKeys)
                        }
                        val alpha = remember(file.chatId, file.messageId) { Animatable(if (hasAnimated) 1f else 0f) }
                        val translateY = remember(file.chatId, file.messageId) { Animatable(if (hasAnimated) 0f else 24f) }

                        LaunchedEffect(file.chatId, file.messageId) {
                            if (!hasAnimated) {
                                val delayMs = (index * 25L).coerceAtMost(250L)
                                kotlinx.coroutines.delay(delayMs)
                                coroutineScope {
                                    launch { alpha.animateTo(1f, tween(240)) }
                                    launch { translateY.animateTo(0f, tween(260)) }
                                }
                                hasAnimated = true
                                animatedItemKeys.add(itemKey)
                            }
                        }

                        Box(modifier = Modifier.animateItem()) {
                            FileCard(
                                file = file,
                                isSelected = selectedFileIds.contains(selectionKey(file)),
                                onClick = { onFileClick(file) },
                                onLongPress = { onFileLongPress(file) },
                                onVisible = { onFileVisible(file) },
                                badge = itemBadge?.invoke(file),
                                modifier = Modifier
                                    .graphicsLayer {
                                        this.alpha = alpha.value
                                        this.translationY = translateY.value
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthDividerHeader(
    monthName: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = monthName,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            ),
            color = TDriveColors.OnCanvas
        )
        Text(
            text = if (count == 1) "1 item" else "$count items",
            style = MaterialTheme.typography.labelMedium,
            color = TDriveColors.OnCanvasMuted,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .border(1.dp, TDriveColors.LineSoft, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}