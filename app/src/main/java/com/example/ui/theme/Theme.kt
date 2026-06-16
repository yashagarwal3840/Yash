package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GymVolt,
    onPrimary = TextDark,
    primaryContainer = GymVoltDim,
    onPrimaryContainer = TextDark,
    secondary = GymTeal,
    onSecondary = TextDark,
    background = DarkBg,
    onBackground = TextLight,
    surface = DarkSurface,
    onSurface = TextLight,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextLight,
    outline = TextGray
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6B8A00), // Darker volt/olive for readability on light background
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4FA75),
    onPrimaryContainer = Color(0xFF1E2800),
    secondary = Color(0xFF007A8F), // Darker teal
    onSecondary = Color.White,
    background = LightBg,
    onBackground = TextDark,
    surface = LightSurface,
    onSurface = TextDark,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextDark,
    outline = Color(0xFF74777F)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamicColor option but default to custom athletic theme for high design aesthetic
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
