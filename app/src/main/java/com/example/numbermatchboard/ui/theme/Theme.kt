package com.example.numbermatchboard.ui.theme

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
import com.example.numbermatchboard.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = LightPlayer,
    onPrimary = Color.White,
    primaryContainer = LightPlayerContainer,
    onPrimaryContainer = LightOnPlayerContainer,
    secondary = LightCpu,
    onSecondary = Color.White,
    secondaryContainer = LightCpuContainer,
    onSecondaryContainer = LightOnCpuContainer,
    tertiary = LightTertiary,
    tertiaryContainer = LightTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError
)

private val DarkColors = darkColorScheme(
    primary = DarkPlayer,
    onPrimary = Color(0xFF00201B),
    primaryContainer = DarkPlayerContainer,
    onPrimaryContainer = DarkOnPlayerContainer,
    secondary = DarkCpu,
    onSecondary = Color(0xFF2C1503),
    secondaryContainer = DarkCpuContainer,
    onSecondaryContainer = DarkOnCpuContainer,
    tertiary = DarkTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError
)

@Composable
fun isAppInDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun NumberMatchBoardTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = isAppInDarkTheme(themeMode)
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val dynamic = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            // Dynamic secondary is usually the same hue as primary; use tertiary so the
            // CPU accent stays clearly distinct from the player's.
            dynamic.copy(
                secondary = dynamic.tertiary,
                onSecondary = dynamic.onTertiary,
                secondaryContainer = dynamic.tertiaryContainer,
                onSecondaryContainer = dynamic.onTertiaryContainer
            )
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
