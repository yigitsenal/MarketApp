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
 * Utility class to fix dark mode issues throughout the app
 * This provides a centralized way to handle theme-specific adjustments
 */
object DarkModeFixUtil {
    
    /**
     * Applies a theme-aware shadow to a Modifier
     */
    @Composable
    fun Modifier.fixedShadow(
        elevation: Dp,
        shape: Shape,
        clip: Boolean = false
    ): Modifier {
        val (ambientColor, spotColor) = if (isSystemInDarkTheme()) {
            // More visible shadows for dark mode
            Pair(
                Color.Black.copy(alpha = 0.25f),
                Color.Black.copy(alpha = 0.35f)
            )
        } else {
            // Subtle shadows for light mode
            Pair(
                PrimaryColor.copy(alpha = 0.04f),
                PrimaryColor.copy(alpha = 0.06f)
            )
        }
        
        // Adjust elevation based on theme
        val adjustedElevation = if (isSystemInDarkTheme()) {
            (elevation.value * 0.7f).dp  // Reduce elevation in dark mode
        } else {
            elevation
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
     * Returns a color that's appropriate for the current theme
     */
    @Composable
    fun getThemeAwareColor(lightColor: Color, darkColor: Color? = null): Color {
        val actualDarkColor = darkColor ?: when (lightColor) {
            Color.White -> MaterialTheme.colorScheme.onPrimary
            Color.Gray -> MaterialTheme.colorScheme.onSurfaceVariant
            PrimaryColor -> PrimaryLightColor
            SecondaryColor -> SecondaryLightColor
            else -> lightColor
        }
        
        return if (isSystemInDarkTheme()) actualDarkColor else lightColor
    }
    
    /**
     * Returns a theme-aware elevation value
     */
    @Composable
    fun getThemeAwareElevation(lightElevation: Dp): Dp {
        return if (isSystemInDarkTheme()) {
            (lightElevation.value * 0.5f).dp  // Half elevation in dark mode
        } else {
            lightElevation
        }
    }
    
    /**
     * Returns a theme-aware container color for cards
     */
    @Composable
    fun getCardContainerColor(alpha: Float = 1f): Color {
        return if (isSystemInDarkTheme()) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = alpha)
        }
    }
    
    /**
     * Returns a theme-aware text color
     */
    @Composable
    fun getTextColor(isSecondary: Boolean = false): Color {
        return if (isSecondary) {
            if (isSystemInDarkTheme()) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    }
}
