package com.telegramdrive.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun TelegramDriveTheme(
    content: @Composable () -> Unit
) {
    val scheme = darkColorScheme(
        background = TDriveColors.Canvas,
        onBackground = TDriveColors.OnCanvas,
        surface = TDriveColors.Surface,
        onSurface = TDriveColors.OnCanvas,
        surfaceVariant = TDriveColors.SurfaceElevated,
        onSurfaceVariant = TDriveColors.OnCanvasMuted,
        primary = TDriveColors.Primary,
        onPrimary = Color(0xFF06231B),
        secondary = TDriveColors.Secondary,
        error = TDriveColors.Error,
        outline = TDriveColors.Outline
    )

    MaterialTheme(
        colorScheme = scheme,
        typography = TDriveTypography,
        shapes = TDriveShapes,
        content = content
    )
}