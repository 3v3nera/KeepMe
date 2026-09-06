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
        LazyVerticalStaggeredGrid(
            state = gridState,
            columns = StaggeredGridCells.Fixed(2),
            verticalItemSpacing = 8.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 120.dp),
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
                        var hasAnimated by rememberSaveable(file.chatId, file.messageId) {
                            mutableStateOf(initialLoadCompleted)
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
                            }
                        }

                        Box(modifier = Modifier.animateItem()) {
                            FileCard(
                                file = file,
                                isSelected = selectedFileIds.contains(file.messageId),
                                onClick = { onFileClick(file) },
                                onLongPress = { onFileLongPress(file) },
                                onVisible = { onFileVisible(file) },
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
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(TDriveColors.Primary)
            )
            Text(
                text = monthName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                ),
                color = Color.White
            )
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.08f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Text(
                text = "$count ${if (count == 1) "item" else "items"}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = TDriveColors.OnSurfaceDim,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
