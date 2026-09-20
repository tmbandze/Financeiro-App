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
    primary = EmeraldPrimaryDark,
    onPrimary = Color(0xFF003730),
    primaryContainer = EmeraldPrimaryContainerDark,
    onPrimaryContainer = Color(0xFFB2DFDB),
    secondary = TealSecondaryDark,
    onSecondary = Color(0xFF003731),
    tertiary = AmberTertiaryDark,
    background = NeutralBackgroundDark,
    onBackground = Color(0xFFE2E8F0),
    surface = NeutralSurfaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = NeutralSurfaceVariantDark,
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = Color(0xFFF87171)
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = Color(0xFF003730),
    secondary = TealSecondary,
    onSecondary = Color.White,
    tertiary = AmberTertiary,
    background = NeutralBackgroundLight,
    onBackground = Color(0xFF0F172A),
    surface = NeutralSurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = NeutralSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFDC2626)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep distinctive custom emerald palette
    content: @Composable () -> Unit
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
