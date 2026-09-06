package com.telegramdrive.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun TelegramDriveTheme(
    forceLightMode: Boolean = false, // Enforce dark mode per guidelines
    content: @Composable () -> Unit
) {
    val darkColorScheme = darkColorScheme(
        background = TDriveColors.Background,
        surface = TDriveColors.Surface,
        surfaceVariant = TDriveColors.SurfaceElevated,
        primary = TDriveColors.Primary,
        onPrimary = Color.White,
        secondary = TDriveColors.Secondary,
        error = TDriveColors.Error,
        onBackground = TDriveColors.OnBackground,
        onSurface = TDriveColors.OnSurface,
        outline = TDriveColors.Outline
    )

    val lightColorScheme = lightColorScheme(
        background = TDriveColors.BackgroundAlt, // Just a fallback, will use gradient
        surface = TDriveColors.Surface,
        surfaceVariant = TDriveColors.SurfaceElevated,
        primary = TDriveColors.Primary,
        onPrimary = Color.Black,
        secondary = TDriveColors.Secondary,
        error = TDriveColors.Error,
        onBackground = TDriveColors.OnBackground,
        onSurface = TDriveColors.OnSurface,
        outline = Color.LightGray
    )

    MaterialTheme(
        colorScheme = if (forceLightMode) lightColorScheme else darkColorScheme,
        typography = TDriveTypography,
        shapes = TDriveShapes,
        content = content
    )
}