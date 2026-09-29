package com.telegramdrive.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.telegramdrive.app.ui.utils.rememberAppHaptics
import java.util.Locale

private enum class TunerCategory(val title: String, val icon: String) {
    OPTICS("Optics", "🔮"),
    CLARITY("Clarity", "🌫️"),
    PHYSICAL("Shape", "📐")
}

/**
 * Floating HUD Popup Tuner:
 * Anchored to the top of the screen with ZERO background dimming,
 * leaving the navigation FAB, bottom bar, and folder cards 100% visible and interactive below.
 */
@Composable
fun LiquidGlassFabTunerPopup(
    params: FabTuningParams,
    isFoldersTab: Boolean,
    onParamsChange: (FabTuningParams) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = rememberAppHaptics()
    var selectedCategory by remember { mutableStateOf(TunerCategory.OPTICS) }
    var isMinimized by remember { mutableStateOf(false) }

    Popup(
        alignment = Alignment.TopCenter,
        properties = PopupProperties(
            focusable = false,              // Allows touches outside to scroll the background!
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp, start = 12.dp, end = 12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(spring(dampingRatio = 0.8f, stiffness = 400f))
                    .shadow(20.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black, spotColor = Color.Black)
                    .border(
                        width = 1.2.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.24f),
                                Color(0xFF6C8FFF).copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.06f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0C101A).copy(alpha = 0.94f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // ── 1. Top HUD Header ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF6C8FFF).copy(alpha = 0.20f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = null,
                                    tint = Color(0xFF6C8FFF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "FAB Live Tuner",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isFoldersTab) "Folders Tab Preset (Live)" else "Standard Preset (Live)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF00E5A0)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Reset button
                            IconButton(
                                onClick = {
                                    haptics.click()
                                    onReset()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Reset Defaults",
                                    tint = Color(0xFF8E9BAE),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Minimize / Expand toggle
                            IconButton(
                                onClick = {
                                    haptics.click()
                                    isMinimized = !isMinimized
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMinimized) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = if (isMinimized) "Expand" else "Minimize",
                                    tint = Color(0xFF8E9BAE),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Close / Done button
                            IconButton(
                                onClick = {
                                    haptics.success()
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF6C8FFF))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Save & Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // ── 2. Category Selector & Sliders (Hidden if minimized) ──
                    if (!isMinimized) {
                        Spacer(Modifier.height(12.dp))

                        // Category Tab Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TunerCategory.values().forEach { category ->
                                val isSelected = category == selectedCategory
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color(0xFF6C8FFF) else Color.White.copy(alpha = 0.08f)
                                        )
                                        .clickable {
                                            haptics.click()
                                            selectedCategory = category
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${category.icon} ${category.title}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF8E9BAE)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Category Sliders Content (Max height constrained so bottom remains open)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 250.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            when (selectedCategory) {
                                TunerCategory.OPTICS -> {
                                    CompactSliderItem(
                                        label = "Refraction Z-Distance",
                                        value = params.refractionAmount,
                                        valueDisplay = "${params.refractionAmount.toInt()} dp",
                                        range = 0f..100f,
                                        onValueChange = { onParamsChange(params.copy(refractionAmount = it)) }
                                    )

                                    CompactSliderItem(
                                        label = "Dome Curvature Zone",
                                        value = params.refractionHeight,
                                        valueDisplay = if (params.refractionHeight >= 28f) "${params.refractionHeight.toInt()} dp (Full Dome)" else "${params.refractionHeight.toInt()} dp",
                                        range = 0f..28f,
                                        onValueChange = { onParamsChange(params.copy(refractionHeight = it)) }
                                    )

                                    CompactSliderItem(
                                        label = "Prism Dispersion (Chromatic)",
                                        value = params.chromaticAberration,
                                        valueDisplay = String.format(Locale.US, "%.2f", params.chromaticAberration),
                                        range = 0f..0.35f,
                                        onValueChange = { onParamsChange(params.copy(chromaticAberration = it)) }
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "3D Spherical Normal",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                        Switch(
                                            checked = params.depthEffect,
                                            onCheckedChange = {
                                                haptics.click()
                                                onParamsChange(params.copy(depthEffect = it))
                                            },
                                            modifier = Modifier.height(28.dp),
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = Color(0xFF6C8FFF)
                                            )
                                        )
                                    }
                                }

                                TunerCategory.CLARITY -> {
                                    CompactSliderItem(
                                        label = "Glass Blur Radius",
                                        value = params.blurRadius,
                                        valueDisplay = "${params.blurRadius.toInt()} dp",
                                        range = 0f..40f,
                                        onValueChange = { onParamsChange(params.copy(blurRadius = it)) }
                                    )

                                    CompactSliderItem(
                                        label = "Color Saturation Boost",
                                        value = params.saturation,
                                        valueDisplay = String.format(Locale.US, "%.1fx", params.saturation),
                                        range = 0.8f..2.5f,
                                        onValueChange = { onParamsChange(params.copy(saturation = it)) }
                                    )
                                }

                                TunerCategory.PHYSICAL -> {
                                    CompactSliderItem(
                                        label = "Physical Elevation Shadow",
                                        value = params.elevation,
                                        valueDisplay = "${params.elevation.toInt()} dp",
                                        range = 0f..36f,
                                        onValueChange = { onParamsChange(params.copy(elevation = it)) }
                                    )

                                    CompactSliderItem(
                                        label = "Button Diameter",
                                        value = params.fabSize,
                                        valueDisplay = "${params.fabSize.toInt()} dp",
                                        range = 46f..68f,
                                        onValueChange = { onParamsChange(params.copy(fabSize = it)) }
                                    )

                                    CompactSliderItem(
                                        label = "Backing Tint Alpha",
                                        value = params.backingTintAlpha,
                                        valueDisplay = "${(params.backingTintAlpha * 100).toInt()}%",
                                        range = 0f..0.80f,
                                        onValueChange = { onParamsChange(params.copy(backingTintAlpha = it)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactSliderItem(
    label: String,
    value: Float,
    valueDisplay: String,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = valueDisplay,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6C8FFF)
                )
            }
        }

        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFF6C8FFF),
                inactiveTrackColor = Color.White.copy(alpha = 0.12f)
            )
        )
    }
}
