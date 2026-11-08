package com.arborecence.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arborecence.core.data.connectivity.ConnectivityObserver
import com.arborecence.core.domain.repository.NotesRepository
import com.arborecence.core.ui.components.GlassSurface
import com.arborecence.core.ui.components.PrimaryGlassButton
import com.arborecence.core.ui.components.SectionTitle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(notes: NotesRepository, connectivity: ConnectivityObserver) : ViewModel() {
    val subjects = notes.subjects().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val online = connectivity.online().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
}

@Composable
fun HomeRoute(onDump: () -> Unit, onAssistant: () -> Unit, onFocus: () -> Unit, onNotes: () -> Unit, onLearning: () -> Unit, vm: HomeViewModel = hiltViewModel()) {
    val subjects by vm.subjects.collectAsState()
    val online by vm.online.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Arborecence", style = MaterialTheme.typography.headlineMedium)
            Text(if (online) "Online" else "Offline, usando conhecimento local", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        }
        item { SectionTitle("Uma ação por vez") }
        item { PrimaryGlassButton("Descarregar (voz)", onDump) }
        item { PrimaryGlassButton("Assistente de voz", onAssistant) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onFocus, Modifier.weight(1f), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)) { Icon(Icons.Outlined.Timer, null); Spacer(Modifier.width(6.dp)); Text("Modo foco") }
                OutlinedButton(onClick = onLearning, Modifier.weight(1f), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)) { Icon(Icons.Outlined.School, null); Spacer(Modifier.width(6.dp)); Text("Aprendizado") }
            }
        }
        item { SectionTitle("Matérias") }
        if (subjects.isEmpty()) item { GlassSurface { Text("Nenhuma matéria ainda. Toque em Notas para criar sua árvore de estudos.", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge) } }
        items(subjects, key = { it.id }) { s ->
            GlassSurface(Modifier.fillMaxWidth()) {
                ListItem(headlineContent = { Text(s.name) }, leadingContent = { Icon(Icons.Outlined.AccountTree, null) }, modifier = Modifier.padding(4.dp),
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent))
            }
        }
        item { OutlinedButton(onClick = onNotes, Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)) { Text("Abrir árvore de notas e revisão") } }
        item { Spacer(Modifier.height(80.dp)) }
    }
}
