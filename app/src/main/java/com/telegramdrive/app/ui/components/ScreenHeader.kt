package com.telegramdrive.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.telegramdrive.app.ui.theme.TDriveMotion
import com.telegramdrive.app.ui.utils.rememberAppHaptics

/**
 * The one back button used by every sub-screen: dark circle, white arrow, morphing into a close
 * icon while a multi-select is active.
 */
@Composable
fun ScreenBackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    inSelectionMode: Boolean = false,
    onClearSelection: () -> Unit = {}
) {
    val haptics = rememberAppHaptics()
    Surface(
        shape = CircleShape,
        color = Color(0xFF2C2C2E),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier.size(48.dp)
    ) {
        AnimatedContent(
            targetState = inSelectionMode,
            transitionSpec = { TDriveMotion.iconMorphIn },
            label = "headerIcon"
        ) { inSelection ->
            IconButton(onClick = {
                haptics.click()
                if (inSelection) onClearSelection() else onBack()
            }) {
                Icon(
                    imageVector = if (inSelection) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = if (inSelection) "Clear Selection" else "Back",
                    tint = Color.White
                )
            }
        }
    }
}

/** Title style shared by every sub-screen header. */
@Composable
fun screenTitleStyle(): TextStyle =
    MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
