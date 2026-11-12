package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 1. Classic Purple
private val PurpleDarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)
private val PurpleLightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// 2. 🌊 Sky Blue ColorScheme (Голубая тема)
private val SkyBlueDarkColorScheme = darkColorScheme(
    primary = SkyBluePrimaryDark,
    onPrimary = SkyBlueOnPrimaryDark,
    primaryContainer = SkyBluePrimaryContainerDark,
    onPrimaryContainer = SkyBlueOnPrimaryContainerDark,
    secondary = SkyBlueSecondaryDark,
    surface = SkyBlueSurfaceDark,
    surfaceContainer = SkyBlueSurfaceDark,
    surfaceContainerHigh = SkyBlueSurfaceVariantDark,
    surfaceVariant = SkyBlueSurfaceVariantDark,
    background = SkyBlueBackgroundDark,
    onSurface = Color(0xFFE1F5FE),
    onBackground = Color(0xFFE1F5FE)
)
private val SkyBlueLightColorScheme = lightColorScheme(
    primary = SkyBluePrimaryLight,
    onPrimary = SkyBlueOnPrimaryLight,
    primaryContainer = SkyBluePrimaryContainerLight,
    onPrimaryContainer = SkyBlueOnPrimaryContainerLight,
    secondary = SkyBlueSecondaryLight,
    surface = SkyBlueSurfaceLight,
    surfaceContainer = SkyBlueSurfaceLight,
    surfaceContainerHigh = SkyBlueSurfaceVariantLight,
    surfaceVariant = SkyBlueSurfaceVariantLight,
    background = SkyBlueBackgroundLight,
    onSurface = Color(0xFF0F1D26),
    onBackground = Color(0xFF0F1D26)
)

// 3. 🌌 Deep Ocean ColorScheme
private val OceanDarkColorScheme = darkColorScheme(
    primary = OceanPrimaryDark,
    onPrimary = Color(0xFF003062),
    primaryContainer = Color(0xFF00468B),
    onPrimaryContainer = Color(0xFFD4E3FF),
    secondary = Color(0xFF90CAF9),
    surface = OceanSurfaceDark,
    surfaceContainer = OceanSurfaceDark,
    surfaceContainerHigh = Color(0xFF1B2A3B),
    background = OceanBackgroundDark
)
private val OceanLightColorScheme = lightColorScheme(
    primary = OceanPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E3FF),
    onPrimaryContainer = Color(0xFF001C3B),
    secondary = Color(0xFF1976D2),
    surface = Color(0xFFF7F9FF),
    surfaceContainer = Color(0xFFF7F9FF),
    surfaceContainerHigh = Color(0xFFE2EAF7),
    background = Color(0xFFFAFBFD)
)

// 4. 🍃 Emerald Teal ColorScheme
private val EmeraldDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = Color(0xFF003730),
    primaryContainer = Color(0xFF005047),
    onPrimaryContainer = Color(0xFFBCEEE5),
    secondary = Color(0xFF80CBC4),
    surface = EmeraldSurfaceDark,
    surfaceContainer = EmeraldSurfaceDark,
    surfaceContainerHigh = Color(0xFF192C29),
    background = EmeraldBackgroundDark
)
private val EmeraldLightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBCEEE5),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF00796B),
    surface = Color(0xFFF5FAF8),
    surfaceContainer = Color(0xFFF5FAF8),
    surfaceContainerHigh = Color(0xFFDDEEEA),
    background = Color(0xFFFAFDFB)
)

// 5. ⚡ Cyberpunk Neon ColorScheme
private val NeonDarkColorScheme = darkColorScheme(
    primary = NeonPrimaryDark,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF8CF4FF),
    secondary = Color(0xFF26C6DA),
    surface = NeonSurfaceDark,
    surfaceContainer = NeonSurfaceDark,
    surfaceContainerHigh = Color(0xFF1F2230),
    background = NeonBackgroundDark
)
private val NeonLightColorScheme = lightColorScheme(
    primary = NeonPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCEF5F9),
    onPrimaryContainer = Color(0xFF002024),
    secondary = Color(0xFF00838F),
    surface = Color(0xFFF5FBFC),
    surfaceContainer = Color(0xFFF5FBFC),
    surfaceContainerHigh = Color(0xFFE0F4F7),
    background = Color(0xFFFAFEFE)
)

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorTheme: AppColorTheme = AppColorTheme.SKY_BLUE,
    darkTheme: Boolean = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    },
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme: ColorScheme = when (colorTheme) {
        AppColorTheme.DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) SkyBlueDarkColorScheme else SkyBlueLightColorScheme
            }
        }
        AppColorTheme.SKY_BLUE -> if (darkTheme) SkyBlueDarkColorScheme else SkyBlueLightColorScheme
        AppColorTheme.DEEP_OCEAN -> if (darkTheme) OceanDarkColorScheme else OceanLightColorScheme
        AppColorTheme.EMERALD -> if (darkTheme) EmeraldDarkColorScheme else EmeraldLightColorScheme
        AppColorTheme.CYBER_NEON -> if (darkTheme) NeonDarkColorScheme else NeonLightColorScheme
        AppColorTheme.PURPLE -> if (darkTheme) PurpleDarkColorScheme else PurpleLightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
