package com.telegramdrive.app.ui.screen.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.telegramdrive.app.domain.model.FileType
import com.telegramdrive.app.domain.model.StorageStat
import com.telegramdrive.app.ui.theme.TDriveColors
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

// Vibrant shape colors
private val VideoColorStart = Color(0xFFFF5C93) // Berry / Magenta gradient start
private val VideoColorEnd = Color(0xFFBA2D65)   // Magenta gradient end
private val ImageColorStart = Color(0xFF6C8FFF) // Electric blue gradient start
private val ImageColorEnd = Color(0xFF3F63E2)   // Deep blue gradient end
private val DocColorStart = Color(0xFF00FFB2)   // Radiant emerald gradient start
private val DocColorEnd = Color(0xFF00A86B)     // Mint emerald gradient end

@Composable
fun StorageDashboard(
    stats: List<StorageStat>,
    modifier: Modifier = Modifier
) {
    // 3 categories: Videos, Images, Docs & All other
    val videoBytes = stats.filter { it.fileType == FileType.VIDEO }.sumOf { it.totalSize }
    val videoCount = stats.filter { it.fileType == FileType.VIDEO }.sumOf { it.fileCount }

    val imageBytes = stats.filter { it.fileType == FileType.IMAGE }.sumOf { it.totalSize }
    val imageCount = stats.filter { it.fileType == FileType.IMAGE }.sumOf { it.fileCount }

    val docBytes = stats.filter { it.fileType != FileType.VIDEO && it.fileType != FileType.IMAGE }.sumOf { it.totalSize }
    val docCount = stats.filter { it.fileType != FileType.VIDEO && it.fileType != FileType.IMAGE }.sumOf { it.fileCount }

    val totalSize = videoBytes + imageBytes + docBytes

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = TDriveColors.SurfaceElevated,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header: Title on left, total formatted storage on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Space",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
                Text(
                    text = formatSizeFull(totalSize),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = ImageColorStart
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stacked Pyramid Shape Visualizer (Left side)
                PyramidStorageVisualizer(
                    videoBytes = videoBytes,
                    imageBytes = imageBytes,
                    docBytes = docBytes,
                    totalBytes = totalSize,
                    modifier = Modifier.size(width = 145.dp, height = 130.dp)
                )

                // Divider
                Box(
                    modifier = Modifier
                        .height(112.dp)
                        .width(1.dp)
                        .background(Color(0xFF4A4A4C).copy(alpha = 0.4f))
                )

                // Stats List (Right side)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    StatItem(
                        icon = Icons.Rounded.SmartDisplay,
                        color = VideoColorStart,
                        badgeShape = BadgeShape.SQUARE,
                        count = "$videoCount files",
                        sizeLabel = formatSizeFull(videoBytes)
                    )

                    StatItem(
                        icon = Icons.Rounded.PhotoLibrary,
                        color = ImageColorStart,
                        badgeShape = BadgeShape.CIRCLE,
                        count = "$imageCount files",
                        sizeLabel = formatSizeFull(imageBytes)
                    )

                    StatItem(
                        icon = Icons.AutoMirrored.Rounded.TextSnippet,
                        color = DocColorStart,
                        badgeShape = BadgeShape.SOFT_TRIANGLE,
                        count = "$docCount files",
                        sizeLabel = formatSizeFull(docBytes)
                    )
                }
            }
        }
    }
}

/**
 * Visualizer showing 3 soft-cornered shapes stacked like a pyramid:
 * - Base Left: Soft Rounded Square (Videos)
 * - Base Right: Soft Circle (Images)
 * - Apex (nestled on top): Soft Rounded Triangle (Docs & Others)
 * Sized proportionally based on storage usage.
 */
@Composable
private fun PyramidStorageVisualizer(
    videoBytes: Long,
    imageBytes: Long,
    docBytes: Long,
    totalBytes: Long,
    modifier: Modifier = Modifier
) {
    // Proportional dimensions in dp (min 30dp, max 66dp)
    val minDim = 30f
    val maxDim = 66f

    fun computeTargetDim(bytes: Long): Float {
        if (totalBytes <= 0L) return 46f // Default balanced size when empty
        if (bytes <= 0L) return minDim * 0.72f // Diminutive preview when 0 bytes
        val fraction = (bytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        val scaled = sqrt(fraction) // Square-root area scaling for natural visual weight
        return minDim + (maxDim - minDim) * scaled
    }

    val targetVideoDim = computeTargetDim(videoBytes)
    val targetImageDim = computeTargetDim(imageBytes)
    val targetDocDim = computeTargetDim(docBytes)

    // Smooth spring physics when storage updates
    val animVideoDim by animateFloatAsState(
        targetValue = targetVideoDim,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "animVideoDim"
    )
    val animImageDim by animateFloatAsState(
        targetValue = targetImageDim,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "animImageDim"
    )
    val animDocDim by animateFloatAsState(
        targetValue = targetDocDim,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "animDocDim"
    )

    // Vector painters for micro-icons inside each shape
    val videoPainter = rememberVectorPainter(Icons.Rounded.SmartDisplay)
    val imagePainter = rememberVectorPainter(Icons.Rounded.PhotoLibrary)
    val docPainter = rememberVectorPainter(Icons.AutoMirrored.Rounded.TextSnippet)

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Convert dp to px
        val sqDim = animVideoDim.dp.toPx()
        val circDim = animImageDim.dp.toPx()
        val triDim = animDocDim.dp.toPx()
        val triHeight = triDim * 0.88f // Classic isosceles proportion

        // Solid Pseudo-Physics:
        // 1. Base layer: Square (left) and Circle (right) do NOT overlap.
        // They sit side-by-side with a clean, tangible physical separation (zero penetration).
        val baseGap = 4.dp.toPx()
        val totalBaseWidth = sqDim + circDim + baseGap
        val creviceX = sqDim + (baseGap / 2f)

        // 2. Triangle resting on top: Solid rigid body resting tangentially above the base shapes.
        // No penetration into either the square or the circle.
        val baseLevelY = max(sqDim, circDim)
        val sqTopRel = baseLevelY - sqDim
        val circTopRel = baseLevelY - circDim
        val restingClearance = 3.dp.toPx() // Tangent resting contact gap (no penetration)
        val triBottomY = min(sqTopRel, circTopRel) - restingClearance
        val triTopY = triBottomY - triHeight

        val triHalfW = triDim / 2f
        val triLeft = creviceX - triHalfW
        val triRight = creviceX + triHalfW

        // Overall bounding box of all 3 stacked solid shapes
        val boundMinX = min(0f, triLeft)
        val boundMaxX = max(totalBaseWidth, triRight)
        val boundMinY = triTopY
        val boundMaxY = baseLevelY
        val boundWidth = boundMaxX - boundMinX
        val boundHeight = boundMaxY - boundMinY

        // Centering translation so the stacked group sits centered in the canvas
        val offsetX = (canvasWidth - boundWidth) / 2f - boundMinX
        val offsetY = (canvasHeight - boundHeight) / 2f - boundMinY

        // Coordinates in shifted space
        val sqLeft = offsetX
        val sqTop = offsetY + (baseLevelY - sqDim)
        val sqRight = sqLeft + sqDim
        val sqBottom = offsetY + baseLevelY
        val sqCornerRadius = sqDim * 0.28f

        // Circle positioned tangent to square + gap (SOLID PHYSICS: NO OVERLAP)
        val circLeft = sqRight + baseGap
        val circTop = offsetY + (baseLevelY - circDim)
        val circRadius = circDim / 2f
        val circCenterX = circLeft + circRadius
        val circCenterY = circTop + circRadius

        // Triangle resting on top (SOLID PHYSICS: NO OVERLAP)
        val triApexX = offsetX + creviceX
        val triApexY = offsetY + triTopY
        val triBaseY = offsetY + triBottomY
        val triCornerRadius = triDim * 0.22f

        // 1. Draw Videos: Soft Rounded Square (Base Left)
        val sqBrush = Brush.linearGradient(
            colors = listOf(VideoColorStart, VideoColorEnd),
            start = Offset(sqLeft, sqTop),
            end = Offset(sqRight, sqBottom)
        )
        drawRoundRect(
            brush = sqBrush,
            topLeft = Offset(sqLeft, sqTop),
            size = Size(sqDim, sqDim),
            cornerRadius = CornerRadius(sqCornerRadius, sqCornerRadius)
        )
        // Soft glowing stroke
        drawRoundRect(
            color = Color.White.copy(alpha = 0.2f),
            topLeft = Offset(sqLeft, sqTop),
            size = Size(sqDim, sqDim),
            cornerRadius = CornerRadius(sqCornerRadius, sqCornerRadius),
            style = Stroke(width = 1.2.dp.toPx())
        )
        // Micro icon in square
        if (sqDim >= 22.dp.toPx()) {
            val iconDim = min(sqDim * 0.48f, 26.dp.toPx())
            translate(
                left = sqLeft + (sqDim - iconDim) / 2f,
                top = sqTop + (sqDim - iconDim) / 2f
            ) {
                with(videoPainter) {
                    draw(
                        size = Size(iconDim, iconDim),
                        colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.94f))
                    )
                }
            }
        }

        // 2. Draw Images: Soft Circle (Base Right)
        val circBrush = Brush.linearGradient(
            colors = listOf(ImageColorStart, ImageColorEnd),
            start = Offset(circLeft, circTop),
            end = Offset(circLeft + circDim, circTop + circDim)
        )
        drawCircle(
            brush = circBrush,
            radius = circRadius,
            center = Offset(circCenterX, circCenterY)
        )
        // Soft glowing stroke
        drawCircle(
            color = Color.White.copy(alpha = 0.2f),
            radius = circRadius,
            center = Offset(circCenterX, circCenterY),
            style = Stroke(width = 1.2.dp.toPx())
        )
        // Micro icon in circle
        if (circDim >= 22.dp.toPx()) {
            val iconDim = min(circDim * 0.48f, 26.dp.toPx())
            translate(
                left = circCenterX - iconDim / 2f,
                top = circCenterY - iconDim / 2f
            ) {
                with(imagePainter) {
                    draw(
                        size = Size(iconDim, iconDim),
                        colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.94f))
                    )
                }
            }
        }

        // 3. Draw Docs: Soft Rounded Triangle (Apex on top)
        val triPath = createRoundedTrianglePath(
            apex = Offset(triApexX, triApexY),
            bottomRight = Offset(triApexX + triHalfW, triBaseY),
            bottomLeft = Offset(triApexX - triHalfW, triBaseY),
            cornerRadius = triCornerRadius
        )

        // Drop shadow onto base shapes to emphasize pyramid stacking
        drawPath(
            path = triPath,
            color = Color.Black.copy(alpha = 0.35f)
        )

        // Triangle gradient fill
        val triBrush = Brush.linearGradient(
            colors = listOf(DocColorStart, DocColorEnd),
            start = Offset(triApexX, triApexY),
            end = Offset(triApexX, triBaseY)
        )
        drawPath(
            path = triPath,
            brush = triBrush
        )
        // Soft glowing stroke
        drawPath(
            path = triPath,
            color = Color.White.copy(alpha = 0.25f),
            style = Stroke(width = 1.2.dp.toPx())
        )
        // Micro icon in triangle
        if (triDim >= 22.dp.toPx()) {
            val iconDim = min(triDim * 0.44f, 22.dp.toPx())
            // Triangle centroid is 2/3 down from apex
            val triCentroidY = triApexY + (triBaseY - triApexY) * 0.62f
            translate(
                left = triApexX - iconDim / 2f,
                top = triCentroidY - iconDim / 2f
            ) {
                with(docPainter) {
                    draw(
                        size = Size(iconDim, iconDim),
                        colorFilter = ColorFilter.tint(Color(0xFF003D26).copy(alpha = 0.88f))
                    )
                }
            }
        }
    }
}

/**
 * Constructs a smooth rounded triangle path using quadratic Bezier curves for all 3 soft corners.
 */
private fun createRoundedTrianglePath(
    apex: Offset,
    bottomRight: Offset,
    bottomLeft: Offset,
    cornerRadius: Float
): Path {
    val p0 = apex
    val p1 = bottomRight
    val p2 = bottomLeft

    // Vector 0 -> 1
    val d01x = p1.x - p0.x
    val d01y = p1.y - p0.y
    val l01 = hypot(d01x, d01y).coerceAtLeast(1f)
    val u01x = d01x / l01
    val u01y = d01y / l01

    // Vector 1 -> 2
    val d12x = p2.x - p1.x
    val d12y = p2.y - p1.y
    val l12 = hypot(d12x, d12y).coerceAtLeast(1f)
    val u12x = d12x / l12
    val u12y = d12y / l12

    // Vector 2 -> 0
    val d20x = p0.x - p2.x
    val d20y = p0.y - p2.y
    val l20 = hypot(d20x, d20y).coerceAtLeast(1f)
    val u20x = d20x / l20
    val u20y = d20y / l20

    val maxRadius = min(l01, min(l12, l20)) * 0.35f
    val r = cornerRadius.coerceIn(2f, maxRadius)

    return Path().apply {
        // Start before corner 0 (along side 2->0)
        moveTo(p0.x - u20x * r, p0.y - u20y * r)
        // Corner 0: Apex curve
        quadraticTo(p0.x, p0.y, p0.x + u01x * r, p0.y + u01y * r)
        // Line down to corner 1
        lineTo(p1.x - u01x * r, p1.y - u01y * r)
        // Corner 1: Bottom-right curve
        quadraticTo(p1.x, p1.y, p1.x + u12x * r, p1.y + u12y * r)
        // Line across to corner 2
        lineTo(p2.x - u12x * r, p2.y - u12y * r)
        // Corner 2: Bottom-left curve
        quadraticTo(p2.x, p2.y, p2.x + u20x * r, p2.y + u20y * r)
        close()
    }
}

private enum class BadgeShape {
    SQUARE,
    CIRCLE,
    SOFT_TRIANGLE
}

@Composable
private fun StatItem(
    icon: ImageVector,
    color: Color,
    badgeShape: BadgeShape,
    count: String,
    sizeLabel: String
) {
    val shapeModifier = when (badgeShape) {
        BadgeShape.SQUARE -> Modifier.clip(RoundedCornerShape(14.dp))
        BadgeShape.CIRCLE -> Modifier.clip(CircleShape)
        BadgeShape.SOFT_TRIANGLE -> Modifier.clip(
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        // Larger badge container (44.dp) with soft corners
        Box(
            modifier = Modifier
                .size(44.dp)
                .then(shapeModifier)
                .background(color.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            // Larger, bolder icon (24.dp)
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = sizeLabel,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
            Text(
                text = count,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFA0A0A5)
            )
        }
    }
}

private fun formatSize(bytes: Long, numberOnly: Boolean = false): String {
    if (bytes <= 0) return if (numberOnly) "0" else "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val formatted = String.format(Locale.US, "%.1f", bytes / Math.pow(1024.0, digitGroups.toDouble()))
    return if (numberOnly) formatted else "$formatted ${units[digitGroups]}"
}

private fun formatSizeFull(bytes: Long): String = formatSize(bytes, false)


