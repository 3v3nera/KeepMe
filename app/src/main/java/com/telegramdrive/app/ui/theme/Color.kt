package com.telegramdrive.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * KeepMe dark palette: a near-black neutral canvas, two raised surface levels, hairlines at
 * 8-10% white and a single emerald accent. Screens read the *semantic* names (OnCanvas*,
 * FillSoft, LineSoft, Surface*) so the whole app can be re-themed from this one file.
 */
object TDriveColors {
    // === CANVAS ===
    val Canvas =            Color(0xFF0B0C0F)  // app background, near-black neutral
    val Background =        Color(0xFF0B0C0F)
    val BackgroundAlt =     Color(0xFF0E1013)

    // === SURFACES (cards, sheets) ===
    val Surface =           Color(0xFF16181C)  // default card
    val SurfaceElevated =   Color(0xFF1B1E23)  // raised card / modal
    val SurfaceBright =     Color(0xFF262A31)  // chips, pressed states

    // === ACCENT (emerald, the only brand colour) ===
    val Accent =            Color(0xFF3EE0B0)
    val Primary =           Color(0xFF3EE0B0)
    val PrimaryVariant =    Color(0xFF2BC496)
    val PrimaryContainer =  Color(0xFF12352B)
    val Secondary =         Color(0xFF3EE0B0)
    val SecondaryContainer= Color(0xFF12352B)

    // === STATES ===
    val Error =             Color(0xFFFF6B7A)
    val ErrorContainer =    Color(0xFF3A1A20)
    val Warning =           Color(0xFFFFB74D)
    val WarningContainer =  Color(0xFF3A2B14)

    // === TEXT ON THE CANVAS / SURFACES ===
    val OnBackground =      Color(0xFFF2F3F5)  // primary
    val OnSurface =         Color(0xFFC9CDD4)  // secondary
    val OnSurfaceDim =      Color(0xFF8B9099)  // tertiary / hints

    val OnCanvas =          Color(0xFFF2F3F5)  // titles, body
    val OnCanvasMuted =     Color(0xFF8B9099)  // subtitles, labels
    val OnCanvasFaint =     Color(0xFF5A5F68)  // placeholders, disabled

    // === FILLS & HAIRLINES (white at low alpha, read on any dark surface) ===
    val FillSoft =          Color(0x0FFFFFFF)  // 6%  chip / field fill
    val LineSoft =          Color(0x1AFFFFFF)  // 10% hairline

    // === OUTLINES ===
    val Outline =           Color(0xFF262A31)
    val OutlineVariant =    Color(0xFF30353D)
}