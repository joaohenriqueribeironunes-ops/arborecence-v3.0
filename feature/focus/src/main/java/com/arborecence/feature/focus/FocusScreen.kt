package com.arborecence.feature.focus

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arborecence.core.ui.components.GlassSurface
import com.arborecence.core.ui.components.PrimaryGlassButton
import com.arborecence.core.ui.theme.LocalGlassPrefs
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusRoute(onBack: () -> Unit) {
    var task by remember { mutableStateOf("") }
    var minutes by remember { mutableFloatStateOf(15f) }
    var remaining by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var breakSuggested by remember { mutableStateOf(false) }
    var parked by remember { mutableStateOf(listOf<String>()) }
    var parkText by remember { mutableStateOf("") }
    val prefs = LocalGlassPrefs.current
    LaunchedEffect(running, remaining) {
        if (running && remaining > 0) { delay(1000); remaining -= 1 }
        if (running && remaining == 0 && !breakSuggested) { running = false; breakSuggested = true }
    }
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, topBar = { TopAppBar(title = { Text("Modo foco") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            GlassSurface { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tarefa única", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(task, { task = it }, label = { Text("O que você vai fazer agora?") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            } }
            GlassSurface { Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val show = if (remaining > 0) remaining else (minutes.toInt() * 60)
                Text("%02d:%02d".format(show / 60, show % 60), style = MaterialTheme.typography.headlineMedium)
                Text("Pomodoro flexível — sem pressão", style = MaterialTheme.typography.bodyMedium)
                Slider(minutes, { minutes = it; if (!running) remaining = 0 }, valueRange = 5f..45f, steps = 7)
                Text("${minutes.toInt()} minutos")
            } }
            if (breakSuggested) GlassSurface { Column(Modifier.padding(16.dp)) { Text("Pausa sensorial sugerida", style = MaterialTheme.typography.titleMedium); Text("Alongue, beba água, respire devagar ou descanse os olhos por alguns instantes — só se quiser."); TextButton(onClick = { breakSuggested = false }) { Text("Ok") } } }
            PrimaryGlassButton(if (running) "Pausar" else "Iniciar") {
                if (!running && remaining == 0) remaining = minutes.toInt() * 60
                running = !running
            }
            OutlinedButton(onClick = { running = false; remaining = 0; breakSuggested = false }, shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)) { Text("Reiniciar") }
            GlassSurface { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Estacionamento de pensamentos", style = MaterialTheme.typography.titleMedium)
                Text("Anote uma distração sem sair da tarefa.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(parkText, { parkText = it }, label = { Text("Pensamento") }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { if (parkText.isNotBlank()) { parked = parked + parkText.trim(); parkText = "" } }) { Text("Estacionar") }
                parked.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            } }
            Spacer(Modifier.height(80.dp))
        }
    }
}
