package com.arborecence.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

data class GlassPrefs(val reduceMotion: Boolean = false, val reduceTransparency: Boolean = false)
val LocalGlassPrefs = staticCompositionLocalOf { GlassPrefs() }

private val LightColors = lightColorScheme(
    primary = LavenderDeep, secondary = MintDeep, tertiary = WaterDeep,
    background = BgLightTop, surface = GlassLight, onPrimary = Color.White,
    onBackground = TextPrimaryLight, onSurface = TextPrimaryLight,
)
private val DarkColors = darkColorScheme(
    primary = Lavender, secondary = Mint, tertiary = Water,
    background = BgDarkTop, surface = GlassDark, onPrimary = Color(0xFF1B1E2B),
    onBackground = TextPrimaryDark, onSurface = TextPrimaryDark,
)

@Composable
fun ArborecenceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reduceMotion: Boolean = false,
    reduceTransparency: Boolean = false,
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalGlassPrefs provides GlassPrefs(reduceMotion, reduceTransparency),
        LocalDensity provides Density(density.density, fontScale.coerceIn(0.85f, 1.5f)),
    ) {
        MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
    }
}
