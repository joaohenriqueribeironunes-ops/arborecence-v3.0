package com.arborecence.feature.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arborecence.core.domain.model.*
import com.arborecence.core.domain.repository.NotesRepository
import com.arborecence.core.ui.components.GlassSurface
import com.arborecence.core.ui.components.PrimaryGlassButton
import com.arborecence.core.ui.components.SectionTitle
import com.arborecence.core.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel @Inject constructor(private val repo: NotesRepository) : ViewModel() {
    val subjects = repo.subjects().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allNotes = repo.notes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val selectedNote = MutableStateFlow<Note?>(null)
    val highlights = selectedNote.flatMapLatest { n -> if (n == null) flowOf(emptyList()) else repo.highlights(n.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun addSubject(name: String) = viewModelScope.launch { if (name.isNotBlank()) repo.upsertSubject(Subject(name = name.trim())) }
    fun addNote(title: String, content: String) = viewModelScope.launch { if (content.isNotBlank()) repo.upsertNote(Note(title = title.ifBlank { "Nota" }, content = content)) }
    fun select(n: Note?) { selectedNote.value = n }
}

fun HighlightColor.ui() = when (this) { HighlightColor.LAVENDER -> Lavender; HighlightColor.MINT -> Mint; HighlightColor.AMBER -> Amber; HighlightColor.WATER -> Water }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesRoute(onReview: () -> Unit, onBack: () -> Unit = {}, vm: NotesViewModel = hiltViewModel()) {
    val subjects by vm.subjects.collectAsState()
    val notes by vm.allNotes.collectAsState()
    val sel by vm.selectedNote.collectAsState()
    val hs by vm.highlights.collectAsState()
    var newSubject by remember { mutableStateOf("") }
    var filterColor by remember { mutableStateOf<HighlightColor?>(null) }
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, topBar = { TopAppBar(title = { Text("Matérias e notas") }, actions = { TextButton(onClick = onReview) { Text("Revisão") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)) }) { pad ->
        if (sel != null) {
            val n = sel!!
            val filtered = if (filterColor == null) hs else hs.filter { it.color == filterColor }
            val ann = buildAnnotatedString {
                append(n.content)
                hs.filter { it.end <= n.content.length && it.start in 0 until it.end }.forEach { h -> addStyle(SpanStyle(background = h.color.ui().copy(alpha = 0.45f)), h.start, h.end) }
            }
            LazyColumn(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { TextButton(onClick = { vm.select(null) }) { Text("← Voltar à árvore") } }
                item { Text(n.title, style = MaterialTheme.typography.headlineMedium) }
                item { SectionTitle("Filtrar por cor") }
                item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = filterColor == null, onClick = { filterColor = null }, label = { Text("Todas") })
                    HighlightColor.entries.forEach { c -> FilterChip(selected = filterColor == c, onClick = { filterColor = c }, label = { Text(c.defaultMeaning) }, leadingIcon = { Box(Modifier.size(10.dp).background(c.ui(), RoundedCornerShape(5.dp))) }) }
                } }
                item { GlassSurface { Column(Modifier.padding(16.dp)) { Text(ann, style = MaterialTheme.typography.bodyLarge) } } }
                if (filterColor != null) item { GlassSurface { Column(Modifier.padding(16.dp)) { Text("Resumo (${filterColor!!.defaultMeaning})", style = MaterialTheme.typography.titleMedium); Text(filtered.joinToString(" \n") { it.text }.ifBlank { "Nenhum trecho nessa cor." }) } } }
                item { Spacer(Modifier.height(80.dp)) }
            }
        } else {
            LazyColumn(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { SectionTitle("Matéria > tema > nota") }
                item { GlassSurface { Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(newSubject, { newSubject = it }, label = { Text("Nova matéria") }, modifier = Modifier.weight(1f))
                    FilledIconButton(onClick = { vm.addSubject(newSubject); newSubject = "" }) { Icon(Icons.Outlined.Add, "Adicionar") }
                } } }
                if (subjects.isEmpty()) item { GlassSurface { Text("Crie uma matéria para organizar seus estudos.", Modifier.padding(16.dp)) } }
                items(subjects, key = { it.id }) { s -> GlassSurface(Modifier.fillMaxWidth()) { ListItem(headlineContent = { Text(s.name) }, supportingContent = { Text("Matéria") }, colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)) } }
                item { SectionTitle("Notas") }
                if (notes.isEmpty()) item { GlassSurface { Text("Sem notas. Use 'Descarregar' na tela inicial para criar uma nota por voz.", Modifier.padding(16.dp)) } }
                items(notes, key = { it.id }) { n ->
                    GlassSurface(Modifier.fillMaxWidth()) { ListItem(headlineContent = { Text(n.title) }, supportingContent = { Text(n.content.take(90) + if (n.content.length > 90) "…" else "") }, modifier = Modifier.padding(2.dp), colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent), trailingContent = { TextButton(onClick = { vm.select(n) }) { Text("Abrir") } }) }
                }
                item { PrimaryGlassButton("Ir para revisão espaçada", onClick = onReview) }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
