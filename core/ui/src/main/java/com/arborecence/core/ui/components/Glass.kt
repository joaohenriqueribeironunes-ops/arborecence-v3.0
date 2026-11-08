package com.arborecence.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CalmGradientBackground(modifier: Modifier = Modifier, dark: Boolean, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.background(MaterialTheme.colorScheme.background), content = content)
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    tonal: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    val fill = if (tonal) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    Box(
        modifier.clip(shape).background(fill),
        content = content,
    )
}

@Composable
fun GlassPill(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    GlassSurface(modifier = modifier, corner = 32.dp, tonal = true, content = content)
}
