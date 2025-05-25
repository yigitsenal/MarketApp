package com.yigitsenal.marketapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/**
 * Theme utility functions to help with dark mode compatibility
 */

/**
 * Returns appropriate shadow colors based on the current theme
 */
@Composable
fun shadowColors(
    lightAmbient: Color = PrimaryColor.copy(alpha = 0.04f),
    lightSpot: Color = PrimaryColor.copy(alpha = 0.06f),
    darkAmbient: Color = Color.Black.copy(alpha = 0.2f),
    darkSpot: Color = Color.Black.copy(alpha = 0.3f)
): Pair<Color, Color> {
    return if (isSystemInDarkTheme()) {
        Pair(darkAmbient, darkSpot)
    } else {
        Pair(lightAmbient, lightSpot)
    }
}

/**
 * Returns appropriate text color for cards based on the current theme
 */
@Composable
fun cardTextColor(
    lightColor: Color = Color.Gray,
    darkColor: Color = Color.LightGray
): Color {
    return if (isSystemInDarkTheme()) darkColor else lightColor
}

/**
 * Returns appropriate container color for cards based on the current theme
 */
@Composable
fun cardContainerColor(
    lightColor: Color = Color.White,
    darkColor: Color = Color(0xFF2D2D2D)
): Color {
    return if (isSystemInDarkTheme()) darkColor else lightColor
}

/**
 * Returns appropriate surface variant color with alpha based on the current theme
 */
@Composable
fun surfaceVariantWithAlpha(alpha: Float = 0.3f): Color {
    return MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
}

/**
 * Returns a color that is visible on both light and dark themes
 */
@Composable
fun adaptiveColor(
    lightThemeColor: Color,
    darkThemeColor: Color
): Color {
    return if (isSystemInDarkTheme()) darkThemeColor else lightThemeColor
}

/**
 * Returns a color with increased contrast for better visibility in dark mode
 */
@Composable
fun highContrastColor(baseColor: Color, contrastIncrease: Float = 0.3f): Color {
    return if (isSystemInDarkTheme()) {
        // Lighten colors in dark mode for better visibility
        Color.White.copy(alpha = contrastIncrease).compositeOver(baseColor)
    } else {
        // Darken colors in light mode for better visibility
        Color.Black.copy(alpha = contrastIncrease).compositeOver(baseColor)
    }
}
