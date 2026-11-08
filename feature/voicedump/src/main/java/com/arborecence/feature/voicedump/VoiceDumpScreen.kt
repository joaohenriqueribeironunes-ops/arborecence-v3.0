package com.arborecence.feature.voicedump

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arborecence.core.domain.model.*
import com.arborecence.core.domain.repository.NotesRepository
import com.arborecence.core.domain.repository.SettingsRepository
import com.arborecence.core.ui.components.GlassSurface
import com.arborecence.core.ui.components.PrimaryGlassButton
import com.arborecence.core.ui.components.SectionTitle
import com.arborecence.core.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceDumpViewModel @Inject constructor(private val notes: NotesRepository, private val settings: SettingsRepository) : ViewModel() {
    fun saveAsNote(title: String, content: String, highlights: List<Highlight>) = viewModelScope.launch {
        val n = Note(title = title.ifBlank { "Descarregamento ${java.text.SimpleDateFormat("dd/MM HH:mm").format(java.util.Date())}" }, content = content, source = NoteSource.VOICE_DUMP)
        notes.upsertNote(n)
        highlights.forEach { notes.addHighlight(it.copy(noteId = n.id)) }
    }
}

fun HighlightColor.ui() = when (this) { HighlightColor.LAVENDER -> Lavender; HighlightColor.MINT -> Mint; HighlightColor.AMBER -> Amber; HighlightColor.WATER -> Water }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceDumpRoute(onBack: () -> Unit, vm: VoiceDumpViewModel = hiltViewModel()) {
    var tfv by remember { mutableStateOf(TextFieldValue("")) }
    var highlights by remember { mutableStateOf(listOf<Highlight>()) }
    var selectedColor by remember { mutableStateOf(HighlightColor.LAVENDER) }
    var saved by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val text = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (text.isNotBlank()) {
                val merged = (if (tfv.text.isBlank()) "" else tfv.text.trimEnd() + "\n\n") + text.trim()
                tfv = TextFieldValue(merged, TextRange(merged.length))
            }
        }
        // áudio não é salvo: RecognizerIntent gerencia captura; não persistimos áudio bruto.
    }
    fun startVoice() {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Explique o que você entendeu sobre o assunto")
        }
        launcher.launch(i)
    }
    fun applyHighlight() {
        val sel = tfv.selection
        if (sel.min < sel.max && sel.max <= tfv.text.length) {
            val t = tfv.text.substring(sel.min, sel.max)
            highlights = highlights + Highlight(noteId = "", start = sel.min, end = sel.max, text = t, color = selectedColor)
        }
    }
    val annotated = remember(tfv.text, highlights) {
        buildAnnotatedString {
            append(tfv.text)
            highlights.filter { it.end <= tfv.text.length && it.start >= 0 && it.start < it.end }.forEach { h ->
                addStyle(SpanStyle(background = h.color.ui().copy(alpha = 0.45f)), h.start, h.end)
            }
        }
    }
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, topBar = { TopAppBar(title = { Text("Descarregar por voz") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryGlassButton("Gravar e transcrever (offline)", onClick = { startVoice() })
            Text("O áudio é descartado após transcrever. Só o texto é salvo.", style = MaterialTheme.typography.bodyMedium)
            SectionTitle("Nota editável (parágrafos curtos)")
            GlassSurface { OutlinedTextField(value = tfv, onValueChange = { tfv = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp).padding(8.dp), placeholder = { Text("Sua explicação aparece aqui. Você pode editar.") }) }
            SectionTitle("Marca-texto virtual (4 cores)")
            Text("Selecione um trecho no texto e toque em uma cor.", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HighlightColor.entries.forEach { c ->
                    FilterChip(selected = selectedColor == c, onClick = { selectedColor = c; applyHighlight() },
                        label = { Text(c.defaultMeaning, maxLines = 1) },
                        leadingIcon = { Box(Modifier.size(12.dp).background(c.ui(), RoundedCornerShape(6.dp))) })
                }
            }
            GlassSurface { Column(Modifier.padding(12.dp)) {
                Text("Pré-visualização com marcas", style = MaterialTheme.typography.titleMedium)
                SelectionContainer { Text(annotated) }
            } }
            if (highlights.isNotEmpty()) {
                val sum = highlights.filter { it.color == selectedColor }.joinToString(" ") { it.text }
                if (sum.isNotBlank()) GlassSurface { Column(Modifier.padding(12.dp)) { Text("Resumo (${selectedColor.defaultMeaning})", style = MaterialTheme.typography.titleMedium); Text(sum) } }
            }
            if (saved) Text("Nota salva.", color = MaterialTheme.colorScheme.secondary)
            PrimaryGlassButton("Salvar como nota", onClick = { vm.saveAsNote("Descarregamento", tfv.text, highlights); saved = true }, enabled = tfv.text.isNotBlank())
            Spacer(Modifier.height(80.dp))
        }
    }
}
