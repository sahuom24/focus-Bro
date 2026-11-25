package dev.breakmode.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import dev.breakmode.session.Wallpaper

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB6D9FF),
    onPrimary = Color(0xFF10283E),
    primaryContainer = Color(0xFF29445D),
    onPrimaryContainer = Color(0xFFDBEDFF),
    secondaryContainer = Color(0xFF304254),
    onSecondaryContainer = Color(0xFFDDEBFA),
    secondary = Color(0xFF9FD6FF),
    onSecondary = Color(0xFF05202E),
    tertiary = Color(0xFFD9C2FF),
    background = Color(0xFF0A0C10),
    onBackground = Color(0xFFF0F0F0),
    surface = Color(0xFF151B23),
    onSurface = Color(0xFFF0F0F0),
    surfaceVariant = Color(0xFF232E3A),
    onSurfaceVariant = Color(0xFFB8C4D1),
    outline = Color(0xFF56677B),
    error = Color(0xFFFFB4AB),
)

@Composable
fun BreakModeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides DarkColors.onBackground,
                content = content,
            )
        },
    )
}

/** Full-screen gradient used behind the break screen. */
fun wallpaperBrush(wallpaper: Wallpaper): Brush = when (wallpaper) {
    Wallpaper.AMOLED -> Brush.verticalGradient(
        listOf(Color(0xFF000000), Color(0xFF000000)),
    )

    Wallpaper.DUSK -> Brush.verticalGradient(
        listOf(Color(0xFF2A1E46), Color(0xFF151B2E), Color(0xFF0A0D12)),
    )

    Wallpaper.FOREST -> Brush.verticalGradient(
        listOf(Color(0xFF12332B), Color(0xFF0F2320), Color(0xFF0A0D12)),
    )

    Wallpaper.NIGHT -> Brush.verticalGradient(
        listOf(Color(0xFF102544), Color(0xFF0B1526), Color(0xFF0A0D12)),
    )
}

/** Small preview swatch for the wallpaper picker. */
fun wallpaperSwatch(wallpaper: Wallpaper): Brush = when (wallpaper) {
    Wallpaper.AMOLED -> Brush.linearGradient(listOf(Color(0xFF1F1F1F), Color(0xFF000000)))
    Wallpaper.DUSK -> Brush.linearGradient(listOf(Color(0xFF3B2B60), Color(0xFF151B2E)))
    Wallpaper.FOREST -> Brush.linearGradient(listOf(Color(0xFF1B4A3D), Color(0xFF0F2320)))
    Wallpaper.NIGHT -> Brush.linearGradient(listOf(Color(0xFF1B3A63), Color(0xFF0B1526)))
}
