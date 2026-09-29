package com.telegramdrive.app.ui.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.collection.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Spacer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Real Die-Cut Vinyl Sticker Generator:
 * Takes any emoji string and generates an authentic die-cut sticker:
 * 1. Rasterizes the emoji glyph to an ARGB_8888 bitmap.
 * 2. Morphologically dilates the exact alpha silhouette by strokeRadius.
 * 3. Draws a hard, tactile drop shadow offset to bottom-right (dx, dy).
 * 4. Fills the dilated contour with pure opaque white vinyl backing.
 * 5. Composites the colorful emoji glyph on top.
 * 6. Caches in LRU cache for instant 60/120 FPS rendering.
 */
object EmojiStickerGenerator {
    private val cache = LruCache<String, ImageBitmap>(150)

    /** Cache lookup only (no rendering) so composables can show cached stickers with zero delay. */
    fun peekStickerBitmap(emoji: String, targetSizePx: Int, strokeRadiusPx: Int = 7): ImageBitmap? =
        cache.get("$emoji-$targetSizePx-$strokeRadiusPx")

    fun getStickerBitmap(
        emoji: String,
        targetSizePx: Int = 160,
        strokeRadiusPx: Int = 7,
        shadowDx: Int = 5,
        shadowDy: Int = 7
    ): ImageBitmap {
        val cacheKey = "$emoji-$targetSizePx-$strokeRadiusPx"
        cache.get(cacheKey)?.let { return it }

        val bitmap = createContourSticker(emoji, targetSizePx, strokeRadiusPx, shadowDx, shadowDy)
        val imageBitmap = bitmap.asImageBitmap()
        cache.put(cacheKey, imageBitmap)
        return imageBitmap
    }

    private fun createContourSticker(
        emoji: String,
        size: Int,
        strokeRadius: Int,
        shadowDx: Int,
        shadowDy: Int
    ): Bitmap {
        val boundedSize = size.coerceIn(64, 256)
        val effectiveRadius = (strokeRadius * (boundedSize / 140f)).toInt().coerceAtLeast(3)
        val effectiveShadowX = (shadowDx * (boundedSize / 140f)).toInt().coerceAtLeast(2)
        val effectiveShadowY = (shadowDy * (boundedSize / 140f)).toInt().coerceAtLeast(3)

        // Step 1: Draw emoji in center of canvas
        val emojiBitmap = Bitmap.createBitmap(boundedSize, boundedSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(emojiBitmap)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = boundedSize * 0.60f
            textAlign = Paint.Align.CENTER
        }
        val fontMetrics = textPaint.fontMetrics
        val baseline = (boundedSize / 2f) - (fontMetrics.descent + fontMetrics.ascent) / 2f
        canvas.drawText(emoji, boundedSize / 2f, baseline, textPaint)

        // Step 2: Extract pixels and build opacity mask
        val pixels = IntArray(boundedSize * boundedSize)
        emojiBitmap.getPixels(pixels, 0, boundedSize, 0, 0, boundedSize, boundedSize)
        val isOpaque = BooleanArray(boundedSize * boundedSize)
        for (i in pixels.indices) {
            val a = (pixels[i] ushr 24) and 0xFF
            if (a > 35) isOpaque[i] = true
        }

        // Step 3: Circular dilation offsets
        val offsets = mutableListOf<Pair<Int, Int>>()
        val rSq = effectiveRadius * effectiveRadius
        for (dy in -effectiveRadius..effectiveRadius) {
            for (dx in -effectiveRadius..effectiveRadius) {
                if (dx * dx + dy * dy <= rSq) {
                    offsets.add(dx to dy)
                }
            }
        }

        val dilated = BooleanArray(boundedSize * boundedSize)
        for (y in 0 until boundedSize) {
            val yOffset = y * boundedSize
            for (x in 0 until boundedSize) {
                if (isOpaque[yOffset + x]) {
                    for (k in offsets.indices) {
                        val (dx, dy) = offsets[k]
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until boundedSize && ny in 0 until boundedSize) {
                            dilated[ny * boundedSize + nx] = true
                        }
                    }
                }
            }
        }

        // Step 4: Construct output bitmap with shadow + white border + emoji
        val outW = boundedSize + effectiveShadowX
        val outH = boundedSize + effectiveShadowY
        val outPixels = IntArray(outW * outH)

        // Dark hard tactile shadow color (matches reference image drop shadow)
        val shadowColor = 0x3D080C18.toInt()
        val whiteColor = 0xFFFFFFFF.toInt()

        // 4a. Draw hard shadow
        for (y in 0 until boundedSize) {
            val srcRow = y * boundedSize
            val dstRow = (y + effectiveShadowY) * outW
            for (x in 0 until boundedSize) {
                if (dilated[srcRow + x]) {
                    outPixels[dstRow + (x + effectiveShadowX)] = shadowColor
                }
            }
        }

        // 4b. Draw white vinyl border & composite emoji on top
        for (y in 0 until boundedSize) {
            val srcRow = y * boundedSize
            val dstRow = y * outW
            for (x in 0 until boundedSize) {
                if (dilated[srcRow + x]) {
                    val p = pixels[srcRow + x]
                    val a = (p ushr 24) and 0xFF
                    if (a > 0) {
                        // Alpha blend emoji pixel over pure white background
                        val invA = 255 - a
                        val r = (((p ushr 16) and 0xFF) * a + 255 * invA) / 255
                        val g = (((p ushr 8) and 0xFF) * a + 255 * invA) / 255
                        val b = ((p and 0xFF) * a + 255 * invA) / 255
                        outPixels[dstRow + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                    } else {
                        outPixels[dstRow + x] = whiteColor
                    }
                }
            }
        }

        emojiBitmap.recycle()
        val outBitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(outPixels, 0, outW, 0, 0, outW, outH)
        return outBitmap
    }
}

/**
 * Reusable Die-Cut Vinyl Sticker Composable:
 * Renders the emoji with exact contour white border and drop shadow.
 */
@Composable
fun ExactContourEmojiSticker(
    emoji: String,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    rotation: Float = -6f
) {
    val density = LocalDensity.current
    val sizePx = remember(size, density) {
        with(density) { (size * 1.5f).roundToPx() }
    }
    val renderSize = sizePx.coerceIn(80, 200)
    // Cached stickers appear instantly; new ones are rasterized off the main thread so
    // scrolling and screen transitions never wait on the pixel work.
    val stickerBitmap by produceState(
        initialValue = EmojiStickerGenerator.peekStickerBitmap(emoji, renderSize),
        emoji, renderSize
    ) {
        if (value == null) {
            value = withContext(Dispatchers.Default) {
                EmojiStickerGenerator.getStickerBitmap(emoji, renderSize)
            }
        }
    }
    val bitmap = stickerBitmap
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = emoji,
            modifier = modifier
                .size(size)
                .graphicsLayer { rotationZ = rotation }
        )
    } else {
        Spacer(modifier.size(size))
    }
}
