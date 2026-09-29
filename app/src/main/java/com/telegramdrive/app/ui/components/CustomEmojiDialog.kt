package com.telegramdrive.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.styropyr0.prismal.sources.rememberPrismalGlassLayer
import com.telegramdrive.app.ui.utils.ExactContourEmojiSticker
import com.telegramdrive.app.ui.utils.rememberAppHaptics

@Composable
fun CustomEmojiDialog(
    backdrop: com.styropyr0.prismal.PrismalBackdrop? = null,
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = rememberAppHaptics()
    backdrop?.readSamplingState()

    var textInput by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    val quickSuggestions = remember {
        listOf("🦊", "🪐", "🕹️", "🥑", "🏎️", "🦁", "🍩", "🌮", "👑", "🧸", "🕶️", "☕", "🍿", "🎧", "📸", "💡", "🌈")
    }

    val displayEmoji = remember(textInput) {
        val trimmed = textInput.trim()
        if (trimmed.isNotEmpty()) trimmed else "✨"
    }
    val isEnabled = textInput.trim().isNotEmpty()

    GlassDialogHost(onDismiss = onDismiss, scrimStrength = 0.55f) {
        Column(
            modifier = Modifier.fillMaxWidth(0.88f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassPanel(backdrop = backdrop, modifier = Modifier.fillMaxWidth()) {
                GlassHeroSlot(
                    chips = {
                        GlassChip {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            GlassChipText("Live Preview")
                        }
                    }
                ) {
                    ExactContourEmojiSticker(
                        emoji = displayEmoji,
                        size = 64.dp,
                        rotation = -5f
                    )
                }

                Spacer(Modifier.height(22.dp))
                Text(
                    text = "Custom Emoji Sticker",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = GlassPanelTokens.TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Drop any emoji here to make it yours",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                    color = GlassPanelTokens.TextSecondary
                )

                Spacer(Modifier.height(22.dp))
                GlassSectionHeader("Emoji")
                Spacer(Modifier.height(14.dp))

                BasicTextField(
                    value = textInput,
                    onValueChange = { input ->
                        // Keep up to 2 emoji graphemes
                        textInput = input.take(4)
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = GlassPanelTokens.TextPrimary,
                        fontWeight = FontWeight.Normal,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(CircleShape)
                                .background(GlassPanelTokens.PillFill)
                                .padding(horizontal = 18.dp),
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
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                        color = Color.White.copy(alpha = 0.4f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                innerTextField()
                            }
                            if (textInput.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.14f))
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

                Spacer(Modifier.height(24.dp))
                GlassSectionHeader("Quick pick")
                Spacer(Modifier.height(14.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(quickSuggestions) { em ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(GlassPanelTokens.PillFill)
                                .then(
                                    if (em == textInput.trim())
                                        Modifier.border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                                    else Modifier
                                )
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassSecondaryButton(
                    text = "Cancel",
                    onClick = {
                        haptics.click()
                        onDismiss()
                    }
                )
                GlassPrimaryButton(
                    text = "Set Sticker",
                    enabled = isEnabled,
                    onClick = {
                        val trimmed = textInput.trim()
                        if (trimmed.isNotEmpty()) {
                            haptics.success()
                            onEmojiSelected(trimmed)
                            onDismiss()
                        }
                    }
                )
            }
        }
    }
}
