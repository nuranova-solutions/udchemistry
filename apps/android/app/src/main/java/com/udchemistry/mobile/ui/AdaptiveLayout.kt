package com.udchemistry.mobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.udchemistry.mobile.ui.components.AppGradientBackground
import com.udchemistry.mobile.ui.screens.DashboardScreen
import com.udchemistry.mobile.ui.theme.ThemeMode
import com.udchemistry.mobile.ui.theme.UDChemistryTheme
import androidx.compose.foundation.layout.fillMaxSize
enum class AppWidthClass {
    Compact,
    Medium,
    Expanded,
}

@Immutable
data class AdaptiveLayout(
    val widthClass: AppWidthClass,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val pagePadding: Dp,
    val contentSpacing: Dp,
    val maxContentWidth: Dp,
) {
    val isCompact: Boolean = widthClass == AppWidthClass.Compact
    val isMedium: Boolean = widthClass == AppWidthClass.Medium
    val isExpanded: Boolean = widthClass == AppWidthClass.Expanded
    val showNavigationRail: Boolean = widthClass == AppWidthClass.Expanded
    val preferTwoColumns: Boolean = widthClass != AppWidthClass.Compact
}

@Composable
fun rememberAdaptiveLayout(): AdaptiveLayout {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    return remember(screenWidth, screenHeight) {
        when {
            screenWidth >= 960 -> AdaptiveLayout(
                widthClass = AppWidthClass.Expanded,
                screenWidthDp = screenWidth,
                screenHeightDp = screenHeight,
                pagePadding = 28.dp,
                contentSpacing = 22.dp,
                maxContentWidth = 1520.dp,
            )

            screenWidth >= 600 -> AdaptiveLayout(
                widthClass = AppWidthClass.Medium,
                screenWidthDp = screenWidth,
                screenHeightDp = screenHeight,
                pagePadding = 22.dp,
                contentSpacing = 20.dp,
                maxContentWidth = 1180.dp,
            )

            else -> AdaptiveLayout(
                widthClass = AppWidthClass.Compact,
                screenWidthDp = screenWidth,
                screenHeightDp = screenHeight,
                pagePadding = 16.dp,
                contentSpacing = 16.dp,
                maxContentWidth = 720.dp,
            )
        }
    }
}

@Preview(name = "Dashboard Preview", showBackground = true, backgroundColor = 0xFF060814)
@Composable
private fun AdaptiveLayoutDashboardPreview() {
    UDChemistryTheme(themeMode = ThemeMode.Dark) {
        AppGradientBackground {
            DashboardScreen(
                uiState = previewMainUiState(),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
