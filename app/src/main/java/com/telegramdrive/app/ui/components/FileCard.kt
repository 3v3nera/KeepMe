package com.telegramdrive.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.valentinilk.shimmer.shimmer
import androidx.compose.ui.platform.LocalContext
import coil.size.Size
import com.telegramdrive.app.domain.model.FileType
import com.telegramdrive.app.domain.model.TelegramFile
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.animation.core.tween
import com.telegramdrive.app.ui.theme.TDriveMotion
import com.telegramdrive.app.ui.theme.TDriveColors
import com.telegramdrive.app.ui.utils.rememberAppHaptics
import com.telegramdrive.app.utils.formatRelativeTime

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileCard(
    file: TelegramFile,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onVisible: () -> Unit = {},
    badge: String? = null
) {
    val haptics = rememberAppHaptics()

    LaunchedEffect(file.thumbnailPath, file.thumbnailFileId) {
        if (file.thumbnailPath == null && file.thumbnailFileId != null) {
            onVisible()
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed || isSelected) 0.94f else 1f,
        animationSpec = TDriveMotion.cardPress,
        label = "scale"
    )

    // Animate selection overlay alpha
    val selectionAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.28f else 0f,
        animationSpec = tween(TDriveMotion.chipSwitchMs),
        label = "selectionAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = {
                    haptics.click()
                    onClick()
                },
                onLongClick = {
                    haptics.heavyClick()
                    onLongPress()
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(
            2.dp, MaterialTheme.colorScheme.primary
        ) else null
    ) {
        Box {
            when (file.fileType) {
                FileType.IMAGE -> ImageFileCard(file)
                FileType.VIDEO -> VideoFileCard(file)
                else -> DocumentFileCard(file)
            }

            // ── Animated overlay + checkmark + star via graphicsLayer ──
            Box(modifier = Modifier.matchParentSize()) {
                // Selection wash
                if (selectionAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { alpha = selectionAlpha }
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }

                // Checkmark — springs in from 0 scale when selected
                val checkAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = tween(TDriveMotion.chipSwitchMs),
                    label = "checkAlpha"
                )
                val checkScale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.3f,
                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 600f),
                    label = "checkScale"
                )
                Box(modifier = Modifier.align(Alignment.TopStart)) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp)
                            .graphicsLayer { alpha = checkAlpha; scaleX = checkScale; scaleY = checkScale }
                    )
                }

                if (badge != null) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Favourite star — fades in/out
                val starAlpha by animateFloatAsState(
                    targetValue = if (!isSelected && file.isFavorite) 1f else 0f,
                    animationSpec = tween(TDriveMotion.chipSwitchMs),
                    label = "starAlpha"
                )
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = "Favorite",
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                            .graphicsLayer { alpha = starAlpha }
                    )
                }
            }

        }
    }
}

@Composable
fun ImageFileCard(file: TelegramFile) {
    val context = LocalContext.current
    val model = file.thumbnailPath ?: file.localFilePath
    // Resolve the file once per path, not on every recomposition while scrolling
    val resolvedFile = remember(model) { model?.let { java.io.File(it).takeIf { f -> f.exists() } } }

    val cardModifier = Modifier
        .fillMaxWidth()
        .then(
            if (file.width != null && file.height != null && file.height > 0)
                Modifier.aspectRatio((file.width.toFloat() / file.height.toFloat()).coerceIn(0.55f, 1.8f))
            else
                Modifier.height(200.dp)
        )

    if (model == null) {
        Box(
            modifier = cardModifier
                .shimmer()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(resolvedFile ?: model)
                .crossfade(140)
                .build(),
            contentDescription = file.fileName,
            contentScale = ContentScale.Crop,
            modifier = cardModifier.background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}

@Composable
fun VideoFileCard(file: TelegramFile) {
    Box {
        val context = LocalContext.current
        val model = file.thumbnailPath ?: file.localFilePath
        val hasThumbnail = remember(model) { !model.isNullOrBlank() && java.io.File(model).exists() }

        val cardModifier = Modifier
            .fillMaxWidth()
            .then(
                if (file.width != null && file.height != null && file.height > 0)
                    Modifier.aspectRatio((file.width.toFloat() / file.height.toFloat()).coerceIn(0.55f, 1.8f))
                else
                    Modifier.height(180.dp)
            )

        if (!hasThumbnail) {
            Box(
                modifier = cardModifier
                    .background(TDriveColors.SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayCircle,
                        contentDescription = "Tap to load",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Tap to load",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.2.sp
                        ),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(java.io.File(model!!))
                    .crossfade(140)
                    .build(),
                contentDescription = file.fileName,
                contentScale = ContentScale.Crop,
                modifier = cardModifier.background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Icon(
                imageVector = Icons.Rounded.PlayCircle,
                contentDescription = "Play Video",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.Center)
            )
        }

        val durationText = formatDuration(file.duration)
        if (durationText.isNotEmpty()) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            ) {
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun DocumentFileCard(file: TelegramFile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shared Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, TDriveColors.LineSoft)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(TDriveColors.Secondary, RoundedCornerShape(50))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (file.fileType) {
                            FileType.AUDIO -> "Audio"
                            FileType.DOCUMENT -> "Document"
                            else -> "File"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Arrow button
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowOutward,
                    contentDescription = "Open",
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = file.fileName,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.AccessTime,
                    contentDescription = "Time",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                val timeString = if (file.effectiveDate > 0L) formatRelativeTime(file.effectiveDate) else "Recent"
                Text(
                    text = "${formatFileSize(file.fileSize)} • $timeString",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val ext = file.fileName.substringAfterLast('.', "").uppercase().take(4)
            if (ext.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = ext,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TDriveColors.Primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
