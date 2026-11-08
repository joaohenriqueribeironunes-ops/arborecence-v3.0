package com.arborecence.core.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arborecence.core.ui.theme.GlassBorderDark
import com.arborecence.core.ui.theme.GlassBorderLight
import com.arborecence.core.ui.theme.GlassDark
import com.arborecence.core.ui.theme.GlassLight
import com.arborecence.core.ui.theme.LocalGlassPrefs

@Composable
fun CalmGradientBackground(modifier: Modifier = Modifier, dark: Boolean, content: @Composable BoxScope.() -> Unit) {
    val brush = if (dark) Brush.verticalGradient(listOf(com.arborecence.core.ui.theme.BgDarkTop, com.arborecence.core.ui.theme.BgDarkBottom))
    else Brush.verticalGradient(listOf(com.arborecence.core.ui.theme.BgLightTop, com.arborecence.core.ui.theme.BgLightBottom))
    Box(modifier.background(brush), content = content)
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    corner: Dp = 28.dp,
    tonal: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val prefs = LocalGlassPrefs.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.2f
    val shape = RoundedCornerShape(corner)
    val fill = when {
        prefs.reduceTransparency && dark -> Color(0xFF2A3040)
        prefs.reduceTransparency -> Color(0xFFF7F8FC)
        dark -> GlassDark
        else -> GlassLight
    }
    val border = Brush.linearGradient(listOf(if (dark) GlassBorderDark else GlassBorderLight, Color.Transparent, if (dark) GlassBorderDark else GlassBorderLight))
    val blurMod = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !prefs.reduceTransparency) {
        Modifier.graphicsLayer { renderEffect = RenderEffect.createBlurEffect(18f, 18f, Shader.TileMode.CLAMP).asComposeRenderEffect(); alpha = 0.99f }
    } else Modifier
    Box(
        modifier
            .shadow(if (prefs.reduceTransparency) 0.dp else 10.dp, shape, clip = false, ambientColor = Color(0x22000000), spotColor = Color(0x22000000))
            .clip(shape)
            .then(blurMod)
            .background(fill)
            .border(1.dp, border, shape),
        content = content,
    )
}

@Composable
fun GlassPill(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    GlassSurface(modifier = modifier, corner = 32.dp, content = content)
}

fun Color.luminance(): Float = (0.299f * red + 0.587f * green + 0.114f * blue)
