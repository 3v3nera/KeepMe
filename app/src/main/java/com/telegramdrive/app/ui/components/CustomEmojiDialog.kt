package com.telegramdrive.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.prismalGlassEffects
import com.styropyr0.prismal.shapes.PrismalRoundedRectangle
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import com.telegramdrive.app.ui.theme.TDriveColors
import com.telegramdrive.app.ui.utils.ExactContourEmojiSticker
import com.telegramdrive.app.ui.utils.RecentEmojiManager
import com.telegramdrive.app.ui.utils.rememberAppHaptics
import kotlinx.coroutines.launch

@Composable
fun CustomEmojiDialog(
    backdrop: com.styropyr0.prismal.PrismalBackdrop? = null,
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptics = rememberAppHaptics()
    val fallbackBackdrop = rememberPrismalGlassLayer()
    val effectiveBackdrop = backdrop ?: fallbackBackdrop
    effectiveBackdrop.readSamplingState()

    var textInput by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val scrimAlpha = remember { Animatable(0f) }
    val cardScale = remember { Animatable(0.92f) }
    val cardAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { scrimAlpha.animateTo(1f, tween(200)) }
        launch {
            cardScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch { cardAlpha.animateTo(1f, tween(160)) }
        kotlinx.coroutines.delay(100)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    androidx.activity.compose.BackHandler(onBack = onDismiss)

    // Quick suggestions if user wants to tap
    val quickSuggestions = remember {
        listOf("🦊", "🪐", "🕹️", "🥑", "🏎️", "🦁", "🍩", "🌮", "👑", "🧸", "🕶️", "☕", "🍿", "🎧", "📸", "💡", "🌈")
    }

    // Determine the active preview emoji
    val displayEmoji = remember(textInput) {
        val trimmed = textInput.trim()
        if (trimmed.isNotEmpty()) trimmed else "✨"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { this.alpha = scrimAlpha.value }
                .background(Color(0xFF01040A).copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        // ── Outer Obsidian Frosted Glass Bento Shell ──
        Column(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .graphicsLayer {
                    scaleX = cardScale.value
                    scaleY = cardScale.value
                    alpha = cardAlpha.value
                }
                .shadow(
                    elevation = 32.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = Color.Black.copy(alpha = 0.70f),
                    ambientColor = Color.Black.copy(alpha = 0.35f)
                )
                .drawPrismalGlass(
                    backdrop = effectiveBackdrop,
                    shape = { PrismalRoundedRectangle(28.dp) },
                    effects = prismalGlassEffects(density)
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F141E).copy(alpha = 0.80f),
                            Color(0xFF05080E).copy(alpha = 0.90f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .clip(RoundedCornerShape(28.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Top Bento Hero Card with Live Die-Cut Sticker Preview ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.16f),
                                Color.White.copy(alpha = 0.06f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.36f),
                                Color.White.copy(alpha = 0.12f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top tag row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .shadow(4.dp, RoundedCornerShape(11.dp), spotColor = Color(0xFF6C8FFF).copy(alpha = 0.35f))
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFF1E293B).copy(alpha = 0.90f), Color(0xFF0F172A).copy(alpha = 0.95f))
                                ),
                                shape = RoundedCornerShape(11.dp)
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = TDriveColors.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.25f))
                            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Sticker Preview",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Live Die-Cut Vinyl Sticker Preview ──
                Box(
                    modifier = Modifier
                        .size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ExactContourEmojiSticker(
                        emoji = displayEmoji,
                        size = 62.dp,
                        rotation = -5f
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Custom Emoji Sticker",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Drop any emoji here to make it yours",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    color = Color(0xFFCBD5E1)
                )
            }

            // ── Inset Emoji Input Field ──
            BasicTextField(
                value = textInput,
                onValueChange = { input ->
                    // Keep up to 2 emoji graphemes
                    textInput = input.take(4)
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(TDriveColors.Primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.50f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (textInput.isEmpty()) {
                                Text(
                                    text = "Type or paste an emoji...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp
                                    ),
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center
                                )
                            }
                            innerTextField()
                        }

                        if (textInput.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable {
                                        haptics.tick()
                                        textInput = ""
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            )

            // ── Quick Suggestions Row ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.40f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Quick Pick",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        letterSpacing = 0.2.sp
                    ),
                    color = Color(0xFF94A3B8)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickSuggestions) { em ->
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                .clickable {
                                    haptics.tick()
                                    textInput = em
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = em, fontSize = 20.sp)
                        }
                    }
                }
            }

            // ── Action Buttons: "Cancel" & "Set Sticker" ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Cancel Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                        .clickable {
                            haptics.click()
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFCBD5E1)
                    )
                }

                // Set Sticker Button
                val isEnabled = textInput.trim().isNotEmpty()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .shadow(
                            elevation = if (isEnabled) 10.dp else 0.dp,
                            shape = CircleShape,
                            spotColor = Color(0xFF3863E5).copy(alpha = 0.50f)
                        )
                        .clip(CircleShape)
                        .background(
                            if (isEnabled) Color.White else Color.White.copy(alpha = 0.08f)
                        )
                        .border(
                            1.dp,
                            if (isEnabled) Color.White else Color.White.copy(alpha = 0.12f),
                            CircleShape
                        )
                        .clickable(enabled = isEnabled) {
                            val em = textInput.trim()
                            if (em.isNotEmpty()) {
                                haptics.success()
                                RecentEmojiManager.addRecentEmoji(context, em)
                                onEmojiSelected(em)
                                onDismiss()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Set Sticker",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) Color(0xFF0F172A) else Color.White.copy(alpha = 0.30f)
                    )
                }
            }
        }
    }
}
