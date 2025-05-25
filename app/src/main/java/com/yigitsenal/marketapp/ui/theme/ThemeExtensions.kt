package com.yigitsenal.marketapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Extension functions for theme-aware UI components
 */

/**
 * Applies a theme-aware shadow to a Modifier
 * This ensures shadows look good in both light and dark themes
 */
@Composable
fun Modifier.themeAwareShadow(
    elevation: Dp,
    shape: Shape = RectangleShape,
    clip: Boolean = false
): Modifier {
    val (ambientColor, spotColor) = shadowColors()
    return this.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = ambientColor,
        spotColor = spotColor,
        clip = clip
    )
}

/**
 * Returns a theme-aware text color for secondary text
 * This ensures text is visible in both light and dark themes
 */
@Composable
fun secondaryTextColor(): Color {
    return if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
}

/**
 * Returns a theme-aware card container color with alpha
 * This ensures cards have proper contrast in both light and dark themes
 */
@Composable
fun cardContainerColor(alpha: Float = 1f): Color {
    return if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.surface.copy(alpha = alpha)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = alpha)
    }
}

/**
 * Returns a theme-aware shadow elevation based on the current theme
 * Shadows are typically more subtle in dark mode
 */
@Composable
fun themeAwareElevation(lightElevation: Dp, darkElevation: Dp): Dp {
    return if (isSystemInDarkTheme()) darkElevation else lightElevation
}

/**
 * Returns a theme-aware color for icons
 * This ensures icons are visible in both light and dark themes
 */
@Composable
fun iconTintColor(defaultColor: Color): Color {
    return if (isSystemInDarkTheme() && defaultColor == Color.White) {
        // In dark mode, white icons might need to be adjusted for better visibility
        MaterialTheme.colorScheme.onPrimary
    } else if (isSystemInDarkTheme() && defaultColor == Color.Gray) {
        // In dark mode, gray icons might need to be lightened
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        defaultColor
    }
}
