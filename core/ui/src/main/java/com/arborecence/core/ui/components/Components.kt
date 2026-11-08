package com.arborecence.core.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.arborecence.core.ui.theme.LocalGlassPrefs
import com.arborecence.core.ui.theme.ToggleGreen

@Composable
fun PrimaryGlassButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val prefs = LocalGlassPrefs.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && !prefs.reduceMotion) 0.97f else 1f, if (prefs.reduceMotion) spring(stiffness = Spring.StiffnessHigh) else spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow), label = "s")
    GlassSurface(modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
        Button(onClick = onClick, enabled = enabled, interactionSource = interaction,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
            modifier = Modifier.fillMaxWidth().height(54.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)) {
            Text(text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun IosStyleSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = ToggleGreen, checkedThumbColor = androidx.compose.ui.graphics.Color.White))
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(horizontal = 4.dp, vertical = 8.dp), style = MaterialTheme.typography.titleLarge)
}
