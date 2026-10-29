package dev.breakmode.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import dev.breakmode.session.Wallpaper

/**
 * The app keeps its own quiet palette instead of following the system accent, so a break screen
 * looks the same on every phone. Light mode gets a softer paper palette for daytime breaks.
 */
private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FE3C8),
    onPrimary = Color(0xFF05231A),
    secondary = Color(0xFF9FD6FF),
    onSecondary = Color(0xFF05202E),
    tertiary = Color(0xFFD9C2FF),
    background = Color(0xFF0C1014),
    onBackground = Color(0xFFE7EDF3),
    surface = Color(0xFF141B22),
    onSurface = Color(0xFFE7EDF3),
    surfaceVariant = Color(0xFF1E2731),
    onSurfaceVariant = Color(0xFF9FB0BF),
    outline = Color(0xFF2B3844),
    error = Color(0xFFFFB4AB),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B6B55),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF22597A),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF5A4390),
    background = Color(0xFFF7F5F0),
    onBackground = Color(0xFF1A1D21),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1D21),
    surfaceVariant = Color(0xFFEDEAE3),
    onSurfaceVariant = Color(0xFF4C5560),
    outline = Color(0xFFC9C5BC),
)

@Composable
fun BreakModeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
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
