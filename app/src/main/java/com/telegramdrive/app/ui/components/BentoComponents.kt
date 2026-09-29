package com.telegramdrive.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.telegramdrive.app.ui.theme.TDriveColors
import com.telegramdrive.app.ui.utils.rememberAppHaptics
import java.io.File

/** Shared card corner radius for the bento layout. */
val BentoRadius: Dp = 24.dp

/** Flat raised card: the basic tile of the redesigned UI. */
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val haptics = rememberAppHaptics()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "bentoScale"
    )
    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(BentoRadius))
            .background(TDriveColors.Surface)
            .border(1.dp, TDriveColors.LineSoft, RoundedCornerShape(BentoRadius))
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = {
                        haptics.click()
                        onClick()
                    }
                ) else Modifier
            )
            .padding(contentPadding),
        content = content
    )
}

/** Two-tone heading: bold white lead-in, muted follow-up ("Hey, Anna / How can we help?"). */
@Composable
fun TwoToneTitle(bold: String, muted: String, modifier: Modifier = Modifier) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = TDriveColors.OnCanvas, fontWeight = FontWeight.Bold)) {
                append(bold)
            }
            append("\n")
            withStyle(SpanStyle(color = TDriveColors.OnCanvasMuted, fontWeight = FontWeight.Normal)) {
                append(muted)
            }
        },
        style = MaterialTheme.typography.headlineLarge.copy(lineHeight = 38.sp),
        modifier = modifier
    )
}

/** Section title with an optional trailing action ("See all"). */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    val haptics = rememberAppHaptics()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = TDriveColors.OnCanvas
        )
        if (action != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelLarge,
                color = TDriveColors.OnCanvasMuted,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable {
                        haptics.click()
                        onAction()
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

/** Round icon button with a small label underneath, used for the quick-action row. */
@Composable
fun IconCircleButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TDriveColors.OnCanvas
) {
    val haptics = rememberAppHaptics()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "circleScale"
    )
    Column(
        modifier = modifier
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    haptics.click()
                    onClick()
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(TDriveColors.SurfaceElevated)
                .border(1.dp, TDriveColors.LineSoft, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TDriveColors.OnCanvasMuted,
            maxLines = 1
        )
    }
}

/** Read-only pill that looks like a search field and navigates to Search when tapped. */
@Composable
fun SearchPill(
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberAppHaptics()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(CircleShape)
            .background(TDriveColors.SurfaceElevated)
            .border(1.dp, TDriveColors.LineSoft, CircleShape)
            .clickable {
                haptics.click()
                onClick()
            }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = TDriveColors.OnCanvasMuted,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = placeholder,
            style = MaterialTheme.typography.bodyLarge,
            color = TDriveColors.OnCanvasFaint
        )
    }
}

/** Label on the left, value on the right, hairline underneath (ski-pass details style). */
@Composable
fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    valueColor: Color = TDriveColors.OnCanvas
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TDriveColors.OnCanvasMuted
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showDivider) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(TDriveColors.LineSoft)
            )
        }
    }
}

/**
 * Photo card with the month written huge across the bottom (Panora style): cover image, a small
 * outlined count chip in the corner and a soft scrim so the title always reads.
 */
@Composable
fun MonthPhotoCard(
    title: String,
    count: Int,
    coverPath: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberAppHaptics()
    val context = LocalContext.current
    val cover = remember(coverPath) { coverPath?.let { File(it).takeIf { f -> f.exists() } } }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(TDriveColors.SurfaceElevated)
            .clickable {
                haptics.click()
                onClick()
            }
    ) {
        if (cover != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(cover).crossfade(true).build(),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        // bottom scrim so the huge title reads on any photo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.10f),
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.62f)
                    )
                )
        )
        // outlined glass count chip
        Row(
            modifier = Modifier
                .padding(14.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.28f))
                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text(
                text = if (count == 1) "1 item" else "$count items",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.5).sp
            ),
            color = Color.White.copy(alpha = 0.90f),
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, bottom = 6.dp)
        )
    }
}

/** Horizontal gap helper for bento rows. */
@Composable
fun BentoGap(size: Dp = 12.dp) = Spacer(Modifier.width(size))
