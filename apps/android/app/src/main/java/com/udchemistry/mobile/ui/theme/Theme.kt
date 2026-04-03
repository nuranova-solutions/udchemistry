package com.udchemistry.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AppTypography = Typography(
    displayLarge = TextStyle(fontSize = 56.sp, lineHeight = 60.sp, fontWeight = FontWeight.ExtraBold),
    displayMedium = TextStyle(fontSize = 46.sp, lineHeight = 50.sp, fontWeight = FontWeight.ExtraBold),
    displaySmall = TextStyle(fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.ExtraBold),
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.ExtraBold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun shouldUseDarkTheme(themeMode: ThemeMode): Boolean {
    return when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
    }
}

@Composable
fun UDChemistryTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val useDarkTheme = shouldUseDarkTheme(themeMode)
    val palette = if (useDarkTheme) DarkAppPalette else LightAppPalette
    val colorScheme = if (useDarkTheme) {
        darkColorScheme(
            primary = AppPrimary,
            secondary = AppSecondary,
            tertiary = AppPrimaryHover,
            background = palette.background,
            surface = palette.glass,
            surfaceVariant = palette.surfaceMuted,
            onPrimary = palette.text,
            onSecondary = palette.text,
            onTertiary = palette.text,
            onBackground = palette.text,
            onSurface = palette.text,
            onSurfaceVariant = palette.mutedText,
            error = AppDanger,
            outline = palette.divider,
        )
    } else {
        lightColorScheme(
            primary = AppPrimary,
            secondary = AppSecondary,
            tertiary = AppPrimaryHover,
            background = palette.background,
            surface = palette.glass,
            surfaceVariant = palette.surfaceMuted,
            onPrimary = palette.text,
            onSecondary = palette.text,
            onTertiary = palette.text,
            onBackground = palette.text,
            onSurface = palette.text,
            onSurfaceVariant = palette.mutedText,
            error = AppDanger,
            outline = palette.divider,
        )
    }

    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
