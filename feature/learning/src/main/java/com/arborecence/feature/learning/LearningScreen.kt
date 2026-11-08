package com.arborecence.feature.learning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arborecence.core.data.connectivity.ConnectivityObserver
import com.arborecence.core.domain.repository.KnowledgeRepository
import com.arborecence.core.ui.components.GlassSurface
import com.arborecence.core.ui.components.PrimaryGlassButton
import com.arborecence.core.ui.components.SectionTitle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LearningViewModel @Inject constructor(knowledge: KnowledgeRepository, connectivity: ConnectivityObserver) : ViewModel() {
    val progress = knowledge.progress().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.arborecence.core.domain.model.LearningProgress())
    val cards = knowledge.cards().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val online = connectivity.online().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningRoute(onStart: () -> Unit, onStop: () -> Unit, vm: LearningViewModel = hiltViewModel()) {
    val p by vm.progress.collectAsState()
    val cards by vm.cards.collectAsState()
    val online by vm.online.collectAsState()
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, topBar = { TopAppBar(title = { Text("Aprendizado de 14 dias") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (online) "Online" else "Offline — o aprendizado pausa e retoma quando houver internet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            GlassSurface { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Dia ${if (p.day == 0) "—" else p.day.toString()} de 14", style = MaterialTheme.typography.titleLarge)
                LinearProgressIndicator(progress = { (p.day.coerceIn(0,14) / 14f) }, Modifier.fillMaxWidth())
                Text("Áreas cobertas: ${p.areasCovered.size}")
                Text("Fichas na base local: ${cards.size}")
                Text("Sem pressão: você pode pausar quando quiser. O aprendizado roda em primeiro plano, com notificação visível, e só com internet.", style = MaterialTheme.typography.bodyMedium)
            } }
            PrimaryGlassButton(if (p.active) "Continuar (serviço em primeiro plano)" else "Iniciar / retomar aprendizado", onClick = onStart)
            OutlinedButton(onClick = onStop, Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)) { Text("Pausar aprendizado") }
            SectionTitle("Base de conhecimento local")
            GlassSurface { Column(Modifier.padding(16.dp)) {
                if (cards.isEmpty()) Text("Ainda sem fichas. Inicie o aprendizado (com internet e app aberto).")
                else Text("Áreas: ${cards.map { it.area }.distinct().joinToString(", ").ifBlank { "—" }}")
            } }
            Spacer(Modifier.height(80.dp))
        }
    }
}
