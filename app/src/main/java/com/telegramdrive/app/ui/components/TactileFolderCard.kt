package com.telegramdrive.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telegramdrive.app.domain.model.TelegramFolder
import com.telegramdrive.app.ui.theme.TDriveMotion
import com.telegramdrive.app.ui.utils.FolderEmojiUtils
import com.telegramdrive.app.ui.utils.rememberAppHaptics

/**
 * Exact Folder Silhouette replicated 1:1 from the Reference Design:
 * - Mathematical ratio matching the reference crop:
 *   - Aspect ratio: 1.104 : 1 (width : height)
 *   - Tab top width: 36% of folder width
 *   - S-curve diagonal slope: 25% of folder width
 *   - Shoulder drop: exactly 9.8% of folder height (gentle organic dip)
 *   - Top-left corner: generous squircle radius (19% of width ~28-32dp)
 *   - Top-right corner: soft rounded shoulder (18% of width ~26-28dp)
 *   - Bottom corners: pillowy rounded base (18% of width ~28-30dp)
 * - Uses cubic Bezier math (k = 0.447715f) for continuous, organic curves.
 */
class ExactFolderShape(
    private val tabWidthRatio: Float = 0.36f,
    private val slopeWidthRatio: Float = 0.25f,
    private val shoulderDropRatio: Float = 0.098f,
    private val cornerTopLeftRatio: Float = 0.19f,
    private val cornerTopRightRatio: Float = 0.18f,
    private val cornerBottomRatio: Float = 0.18f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width
        val h = size.height

        val tabW = w * tabWidthRatio
        val slopeW = w * slopeWidthRatio
        val shoulderY = h * shoulderDropRatio
        val cTL = w * cornerTopLeftRatio
        val cTR = w * cornerTopRightRatio
        val cB = w * cornerBottomRatio
        val k = 0.447715f // 1 - 0.552285 (standard cubic Bezier circle constant)

        val path = Path().apply {
            // 1. Start on left vertical edge at end of top-left corner
            moveTo(0f, cTL)

            // 2. Top-left corner arc: from (0, cTL) to (cTL, 0)
            cubicTo(
                0f, cTL * k,
                cTL * k, 0f,
                cTL, 0f
            )

            // 3. Tab horizontal top edge to start of diagonal slope
            lineTo(tabW, 0f)

            // 4. Smooth cubic Bezier S-curve dipping down to shoulder
            cubicTo(
                tabW + slopeW * 0.48f, 0f,
                tabW + slopeW * 0.52f, shoulderY,
                tabW + slopeW, shoulderY
            )

            // 5. Shoulder horizontal top edge across to top-right corner
            lineTo(w - cTR, shoulderY)

            // 6. Top-right corner arc: from (w - cTR, shoulderY) to (w, shoulderY + cTR)
            cubicTo(
                w - cTR * k, shoulderY,
                w, shoulderY + cTR * (1f - k),
                w, shoulderY + cTR
            )

            // 7. Right vertical edge down to bottom-right corner
            lineTo(w, h - cB)

            // 8. Bottom-right corner arc: from (w, h - cB) to (w - cB, h)
            cubicTo(
                w, h - cB * k,
                w - cB * k, h,
                w - cB, h
            )

            // 9. Bottom horizontal edge across to bottom-left corner
            lineTo(cB, h)

            // 10. Bottom-left corner arc: from (cB, h) to (0, h - cB)
            cubicTo(
                cB * k, h,
                0f, h - cB * k,
                0f, h - cB
            )

            // Close back to (0, cTL)
            close()
        }
        return Outline.Generic(path)
    }
}

val ExactFolderSleeveShape = ExactFolderShape()

/**
 * TactileFolderCard matching the exact visual reference:
 * - Color Profile: Porcelain matte ceramic gradient (#F3F5F7 -> #EAEDF1 -> #DFE3E8 -> #E4E7EC)
 * - Peeking Cards: Clean white cards with mint checkmark badge & pencil icon (subtle 4.5° tilt)
 * - Sticker: Die-cut vinyl emoji sticker with white border contour, realistic drop shadow, and -6° playful tilt
 * - Clean bottom-left area (monogram removed)
 * - Below: Folder name + minimal capsule count pill ("Figma  4")
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TactileFolderCard(
    folder: TelegramFolder,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptics = rememberAppHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = TDriveMotion.cardPress,
        label = "folderScale"
    )

    Column(
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
                    onLongClick()
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Main Folder Composition with Peeking Cards ──
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.92f) // Composition aspect ratio matching reference
        ) {
            val cardW = maxWidth
            val flapH = cardW / 1.104f // Exact 1.104 : 1 front flap ratio

            // ── Peeking Card 1 (Back Left — Mint Green Checkmark Badge) ──
            Surface(
                modifier = Modifier
                    .width(cardW * 0.44f)
                    .height(flapH * 0.52f)
                    .offset(x = cardW * 0.14f, y = cardW * 0.015f)
                    .graphicsLayer { rotationZ = -1.5f }
                    .shadow(4.dp, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF7F8FA),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 8.dp, top = 8.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Box(
                        modifier = Modifier
                            .size(19.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34D399)), // Exact luminous mint emerald
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // ── Peeking Card 2 (Front Right — Tilted Stylus/Pencil Card) ──
            Surface(
                modifier = Modifier
                    .width(cardW * 0.48f)
                    .height(flapH * 0.54f)
                    .offset(x = cardW * 0.28f, y = cardW * 0.005f)
                    .graphicsLayer { rotationZ = 4.5f } // Gentle tilt matching reference
                    .shadow(6.dp, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 14.dp, top = 10.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    // Minimal angled stylus icon from reference (< angle stroke)
                    StylusPenIcon(
                        modifier = Modifier.size(15.dp),
                        tint = Color(0xFF64748B)
                    )
                }
            }

            // ── Front Folder Flap (Exact Shape & Porcelain Color Profile) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(flapH)
                    .align(Alignment.BottomCenter)
                    .shadow(
                        elevation = 8.dp,
                        shape = ExactFolderSleeveShape,
                        ambientColor = Color(0x35000000),
                        spotColor = Color(0x28000000)
                    )
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF3F5F7), // Clean porcelain tab top
                                Color(0xFFEAEDF1), // Gentle ceramic transition
                                Color(0xFFDFE3E8), // Matte body porcelain
                                Color(0xFFE4E7EC)  // Subtle bottom ambient light
                            )
                        ),
                        shape = ExactFolderSleeveShape
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.95f),
                                    Color(0xFFCAD0D8)
                                )
                            )
                        ),
                        shape = ExactFolderSleeveShape
                    )
            ) {
                // Die-Cut Vinyl Sticker Emblem on front (Lower-Right with playful -6° tilt)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 18.dp, bottom = 20.dp)
                ) {
                    val displayEmoji = when {
                        folder.isSavedMessages -> folder.emoji ?: "☁️"
                        else -> folder.emoji ?: FolderEmojiUtils.getDeterministicEmoji(folder.id)
                    }

                    if (folder.isSavedMessages && folder.emoji == null) {
                        TelegramCloudVinylSticker()
                    } else {
                        EmojiVinylSticker(emoji = displayEmoji)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Label underneath ("Figma") ──
        Text(
            text = folder.name,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

/**
 * Trash Folder Variant with the exact same shape, rendered in soft rose porcelain
 */
@Composable
fun TactileTrashFolderCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberAppHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = TDriveMotion.cardPress,
        label = "trashScale"
    )

    Column(
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
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.92f)
        ) {
            val cardW = maxWidth
            val flapH = cardW / 1.104f

            // Peeking Card 1 (Rose Tint with Delete Badge)
            Surface(
                modifier = Modifier
                    .width(cardW * 0.44f)
                    .height(flapH * 0.52f)
                    .offset(x = cardW * 0.14f, y = cardW * 0.015f)
                    .graphicsLayer { rotationZ = -1.5f }
                    .shadow(4.dp, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFFEDEE),
                border = BorderStroke(1.dp, Color(0xFFFFCDD2))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 8.dp, top = 8.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Box(
                        modifier = Modifier
                            .size(19.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            // Peeking Card 2 (Front Right)
            Surface(
                modifier = Modifier
                    .width(cardW * 0.48f)
                    .height(flapH * 0.54f)
                    .offset(x = cardW * 0.28f, y = cardW * 0.005f)
                    .graphicsLayer { rotationZ = 4.5f }
                    .shadow(6.dp, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFFFCDD2))
            ) {}

            // Front Sleeve (Soft Rose Tinted Porcelain)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(flapH)
                    .align(Alignment.BottomCenter)
                    .shadow(
                        elevation = 8.dp,
                        shape = ExactFolderSleeveShape,
                        ambientColor = Color(0x35000000),
                        spotColor = Color(0x28000000)
                    )
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFF1F2),
                                Color(0xFFFFE4E6),
                                Color(0xFFFECDCE),
                                Color(0xFFFEE2E4)
                            )
                        ),
                        shape = ExactFolderSleeveShape
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.95f),
                                    Color(0xFFFCA5A5)
                                )
                            )
                        ),
                        shape = ExactFolderSleeveShape
                    )
            ) {
                // Die-cut Trash Sticker on lower-right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 18.dp, bottom = 20.dp)
                ) {
                    EmojiVinylSticker(emoji = "🗑️")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Label underneath ("Trash") ──
        Text(
            text = "Trash",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

/**
 * Exact Stylus/Pen angle icon matching the reference card (< angle nib)
 */
@Composable
private fun StylusPenIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF64748B)
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 1.6.dp.toPx()
        val path = Path().apply {
            moveTo(size.width * 0.85f, size.height * 0.18f)
            lineTo(size.width * 0.18f, size.height * 0.65f)
            lineTo(size.width * 0.68f, size.height * 0.82f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

/**
 * Authentic Die-Cut Vinyl Emoji Sticker:
 * Exact contour white vinyl outline following the emoji silhouette and hard drop shadow.
 */
@Composable
fun EmojiVinylSticker(
    emoji: String,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp
) {
    com.telegramdrive.app.ui.utils.ExactContourEmojiSticker(
        emoji = emoji,
        modifier = modifier,
        size = size,
        rotation = -6f
    )
}

/**
 * Die-Cut Telegram Cloud Sticker with exact contour white border and hard drop shadow.
 */
@Composable
private fun TelegramCloudVinylSticker(
    modifier: Modifier = Modifier,
    size: Dp = 54.dp
) {
    com.telegramdrive.app.ui.utils.ExactContourEmojiSticker(
        emoji = "☁️",
        modifier = modifier,
        size = size,
        rotation = -6f
    )
}
