package com.yigitsenal.marketapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

// Base colors
val PrimaryColor = Color(0xFF2196F3)
val PrimaryLightColor = Color(0xFF6EC6FF)
val PrimaryDarkColor = Color(0xFF0069C0)
val SecondaryColor = Color(0xFFFF6F00)
val SecondaryLightColor = Color(0xFFFFAA42)
val SecondaryDarkColor = Color(0xFFC43E00)

// Dark theme specific colors
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2D2D2D)
val DarkOnSurfaceVariant = Color(0xFFD0D0D0)

// Light theme specific colors
val LightBackground = Color.White
val LightSurface = Color.White
val LightSurfaceVariant = Color(0xFFF5F5F5)
val LightOnSurfaceVariant = Color(0xFF666666)

// Shadow colors
val LightShadowAmbient = PrimaryColor.copy(alpha = 0.04f)
val LightShadowSpot = PrimaryColor.copy(alpha = 0.06f)
val DarkShadowAmbient = Color.Black.copy(alpha = 0.2f)
val DarkShadowSpot = Color.Black.copy(alpha = 0.3f)

// Material 3 palette
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

/**
 * Returns appropriate shadow colors based on the current theme
 */
@Composable
fun shadowColors(): Pair<Color, Color> {
    return if (isSystemInDarkTheme()) {
        Pair(DarkShadowAmbient, DarkShadowSpot)
    } else {
        Pair(LightShadowAmbient, LightShadowSpot)
    }
}

/**
 * Returns a color that is visible on both light and dark themes
 */
@Composable
fun adaptiveGray(): Color {
    return if (isSystemInDarkTheme()) {
        DarkOnSurfaceVariant
    } else {
        LightOnSurfaceVariant
    }
}

/**
 * Returns appropriate surface color with alpha based on the current theme
 */
@Composable
fun adaptiveSurfaceVariant(alpha: Float = 0.3f): Color {
    return if (isSystemInDarkTheme()) {
        DarkSurfaceVariant.copy(alpha = alpha)
    } else {
        LightSurfaceVariant.copy(alpha = alpha)
    }
}