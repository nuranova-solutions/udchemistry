package com.udchemistry.mobile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AppPalette(
    val isDark: Boolean,
    val background: Color,
    val backgroundTop: Color,
    val backgroundMid: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val glass: Color,
    val glassStrong: Color,
    val text: Color,
    val mutedText: Color,
    val divider: Color,
    val blueGlow: Color,
    val purpleGlow: Color,
    val orangeGlow: Color,
)

internal val DarkAppPalette = AppPalette(
    isDark = true,
    background = Color(0xFF060814),
    backgroundTop = Color(0xFF080B15),
    backgroundMid = Color(0xFF0B1020),
    surface = Color(0xFF11172A),
    surfaceMuted = Color(0xFF161E35),
    glass = Color(0xD8182036),
    glassStrong = Color(0xE61D2742),
    text = Color(0xFFF4F7FF),
    mutedText = Color(0xFFB3BDD7),
    divider = Color(0x26FFFFFF),
    blueGlow = Color(0xFF2D7BFF),
    purpleGlow = Color(0xFF7C3AED),
    orangeGlow = Color(0xFFFF8C00),
)

internal val LightAppPalette = AppPalette(
    isDark = false,
    background = Color(0xFFF5F3FF),
    backgroundTop = Color(0xFFFDFBFF),
    backgroundMid = Color(0xFFF4EEFF),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF4F1FB),
    glass = Color(0xEEFFFFFF),
    glassStrong = Color(0xFFF8F6FF),
    text = Color(0xFF111827),
    mutedText = Color(0xFF667085),
    divider = Color(0x1F18243D),
    blueGlow = Color(0xFF60A5FA),
    purpleGlow = Color(0xFFA855F7),
    orangeGlow = Color(0xFFFFA347),
)

internal val LocalAppPalette = staticCompositionLocalOf { DarkAppPalette }

val AppPrimary = Color(0xFF8A2BE2)
val AppPrimaryHover = Color(0xFFA855F7)
val AppSecondary = Color(0xFF4A78FF)
val AppAccent = Color(0xFFFF8C00)
val AppAccentSoft = Color(0x33FF8C00)
val AppDanger = Color(0xFFFF5D73)
val AppSuccess = Color(0xFF00E38C)
val AppSuccessSoft = Color(0x3300E38C)

val AppBackground: Color
    @Composable get() = LocalAppPalette.current.background

val AppSurface: Color
    @Composable get() = LocalAppPalette.current.surface

val AppSurfaceMuted: Color
    @Composable get() = LocalAppPalette.current.surfaceMuted

val AppGlass: Color
    @Composable get() = LocalAppPalette.current.glass

val AppGlassStrong: Color
    @Composable get() = LocalAppPalette.current.glassStrong

val AppText: Color
    @Composable get() = LocalAppPalette.current.text

val AppMutedText: Color
    @Composable get() = LocalAppPalette.current.mutedText

val AppDivider: Color
    @Composable get() = LocalAppPalette.current.divider

val AppBlueGlow: Color
    @Composable get() = LocalAppPalette.current.blueGlow

val AppPurpleGlow: Color
    @Composable get() = LocalAppPalette.current.purpleGlow

val AppOrangeGlow: Color
    @Composable get() = LocalAppPalette.current.orangeGlow
