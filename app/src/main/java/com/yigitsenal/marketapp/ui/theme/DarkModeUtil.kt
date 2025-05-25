package com.yigitsenal.marketapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dark mode utility functions to ensure proper visibility in dark mode
 */

/**
 * Returns a theme-aware shadow elevation
 * Shadows are typically more subtle in dark mode
 */
@Composable
fun adaptiveElevation(lightModeElevation: Dp): Dp {
    return if (isSystemInDarkTheme()) {
        // Reduce elevation in dark mode to avoid harsh shadows
        (lightModeElevation.value * 0.6f).dp
    } else {
        lightModeElevation
    }
}

/**
 * Applies a theme-aware shadow to a modifier
 */
@Composable
fun Modifier.adaptiveShadow(
    elevation: Dp,
    shape: Shape,
    clip: Boolean = false
): Modifier {
    val adjustedElevation = adaptiveElevation(elevation)
    
    // Use different shadow colors based on theme
    val (ambientColor, spotColor) = if (isSystemInDarkTheme()) {
        // Darker, more subtle shadows for dark mode
        Pair(
            Color.Black.copy(alpha = 0.2f),
            Color.Black.copy(alpha = 0.3f)
        )
    } else {
        // Lighter shadows for light mode
        Pair(
            PrimaryColor.copy(alpha = 0.04f),
            PrimaryColor.copy(alpha = 0.06f)
        )
    }
    
    return this.shadow(
        elevation = adjustedElevation,
        shape = shape,
        ambientColor = ambientColor,
        spotColor = spotColor,
        clip = clip
    )
}

/**
 * Returns a theme-aware text color for better visibility in both themes
 */
@Composable
fun adaptiveTextColor(lightModeColor: Color): Color {
    return if (isSystemInDarkTheme()) {
        when (lightModeColor) {
            Color.White -> MaterialTheme.colorScheme.onPrimary
            Color.Gray -> MaterialTheme.colorScheme.onSurfaceVariant
            Color.Black -> MaterialTheme.colorScheme.onSurface
            else -> lightModeColor
        }
    } else {
        lightModeColor
    }
}

/**
 * Returns a theme-aware container color for cards
 */
@Composable
fun adaptiveContainerColor(alpha: Float = 1f): Color {
    return if (isSystemInDarkTheme()) {
        DarkSurfaceVariant.copy(alpha = alpha)
    } else {
        LightSurfaceVariant.copy(alpha = alpha)
    }
}

/**
 * Returns a theme-aware primary color
 */
@Composable
fun adaptivePrimaryColor(): Color {
    return if (isSystemInDarkTheme()) {
        PrimaryLightColor
    } else {
        PrimaryColor
    }
}

/**
 * Returns a theme-aware secondary color
 */
@Composable
fun adaptiveSecondaryColor(): Color {
    return if (isSystemInDarkTheme()) {
        SecondaryLightColor
    } else {
        SecondaryColor
    }
}
