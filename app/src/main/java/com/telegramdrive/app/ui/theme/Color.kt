package com.telegramdrive.app.ui.theme

import androidx.compose.ui.graphics.Color

object TDriveColors {
    // === CANVAS (70% — Backgrounds) ===
    val Background =        Color(0xFF080C18)  // Deep space navy (L=5%, S=12%, H=225°)
    val BackgroundAlt =     Color(0xFF0A0F1E)  // Slightly lighter variant

    // === SURFACE (25% — Cards, Sheets, Dialogs) ===
    val Surface =           Color(0xFF111827)  // Card surface (L=10%, S=11%)
    val SurfaceElevated =   Color(0xFF1A2238)  // Modals, bottom sheets (L=15%)
    val SurfaceBright =     Color(0xFF243050)  // Hover states, chips (L=20%)

    // === PRIMARY (5% — Actions, FAB, Links) ===
    val Primary =           Color(0xFF6C8FFF)  // Electric periwinkle blue
    val PrimaryVariant =    Color(0xFF4A6FE5)  // Pressed state
    val PrimaryContainer =  Color(0xFF1A2D5A)  // Muted container background

    // === SECONDARY (Accent — Success states) ===
    val Secondary =         Color(0xFF00E5A0)  // Emerald mint (upload complete, online)
    val SecondaryContainer= Color(0xFF0A2E24)  // Muted green container

    // === ERROR / DANGER ===
    val Error =             Color(0xFFFF6B8A)  // Soft coral-pink
    val ErrorContainer =    Color(0xFF3D1520)  // Muted red container

    // === WARNING ===
    val Warning =           Color(0xFFFFB74D)  // Warm amber
    val WarningContainer =  Color(0xFF3D2E14)

    // === TEXT (NEVER pure white — OLED glare rule) ===
    val OnBackground =      Color(0xFFE8ECF4)  // Primary text (rgba 255,255,255,0.92)
    val OnSurface =         Color(0xFFB0B8CC)  // Secondary text (rgba 255,255,255,0.72)
    val OnSurfaceDim =      Color(0xFF6B7394)  // Tertiary/disabled text (rgba 255,255,255,0.45)

    // === OUTLINE / DIVIDERS ===
    val Outline =           Color(0xFF1E2740)  // Subtle borders (NOT white/gray lines)
    val OutlineVariant =    Color(0xFF2A3456)  // Interactive borders

    // === LIGHT THEME (Phase 5 Reference) ===
    val LightBackgroundGradientStart = Color(0xFFFFDAB9) // Soft peach/pink
    val LightBackgroundGradientEnd = Color(0xFFF7F7F7)   // Off-white/light gray
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceDark = Color(0xFF2C2C2E) // For storage card
    val LightPrimary = Color(0xFFC4E265) // Light green for chips/progress
    val LightOnBackground = Color(0xFF1C1C1E)
    val LightOnSurface = Color(0xFF1C1C1E)
    val LightOnSurfaceVariant = Color(0xFF8E8E93)
}