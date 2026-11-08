package com.arborecence.feature.assistant

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arborecence.core.data.assistant.TtsHelper
import com.arborecence.core.domain.model.AssistantReply
import com.arborecence.core.domain.repository.AssistantRepository
import com.arborecence.core.ui.components.GlassSurface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMsg(val fromUser: Boolean, val text: String, val source: String? = null, val offline: Boolean = true)

@HiltViewModel
class AssistantViewModel @Inject constructor(private val repo: AssistantRepository, private val tts: TtsHelper) : ViewModel() {
    val online = repo.isOnline().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val geminiConfigured = repo.isGeminiConfigured()
    val messages = mutableStateListOf<ChatMsg>()
    var busy = mutableStateOf(false)
        private set
    var lastQuestion = mutableStateOf("")
        private set

    private suspend fun route(prompt: String): AssistantReply {
        // Funciona sem fichas: com internet + Gemini configurado, responde via Gemini.
        return if (online.value && geminiConfigured) repo.askGemini(prompt, personalConsent = true) else repo.askLocal(prompt)
    }

    fun ask(q: String, mode: String = "normal") = viewModelScope.launch {
        if (q.isBlank()) return@launch
        lastQuestion.value = q
        val prompt = when (mode) {
            "other" -> "Explique de outro jeito, de forma ainda mais simples e clara: $q"
            "more" -> "Dê mais detalhes, com cuidado e clareza: $q"
            else -> q
        }
        if (mode == "normal") messages += ChatMsg(true, q)
        busy.value = true
        val r = route(prompt)
        messages += ChatMsg(false, r.text, r.source, r.offline)
        tts.speak(r.text.take(500))
        busy.value = false
    }
    fun askGemini(q: String) = viewModelScope.launch {
        if (q.isBlank()) return@launch
        busy.value = true
        val r = repo.askGemini(q, personalConsent = true)
        messages += ChatMsg(false, r.text, r.source, r.offline)
        tts.speak(r.text.take(500))
        busy.value = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantRoute(onBack: () -> Unit, startVoice: Boolean = false, onStartMic: () -> Unit = {}, onStopMic: () -> Unit = {}, vm: AssistantViewModel = hiltViewModel()) {
    val online by vm.online.collectAsState()
    var input by remember { mutableStateOf("") }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val text = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (text.isNotBlank()) {
                if (text.trim().equals("encerrar", true)) onStopMic() else vm.ask(text)
            }
        }
    }
    fun launchVoice() {
        onStartMic()
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Pode falar. Diga \"encerrar\" para parar.")
        }
        voiceLauncher.launch(i)
    }
    LaunchedEffect(startVoice) { if (startVoice) launchVoice() }
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { TopAppBar(title = { Text("Assistente") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)) },
        bottomBar = {
            GlassSurface(Modifier.padding(8.dp), corner = 24.dp) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { launchVoice() }) { Icon(Icons.Outlined.Mic, "Voz") }
                    OutlinedTextField(input, { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Pergunte...") }, maxLines = 3)
                    IconButton(onClick = { vm.ask(input); input = "" }, enabled = !vm.busy.value) { Icon(Icons.Outlined.Send, "Enviar") }
                }
            }
        }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Text(
                when {
                    online && vm.geminiConfigured -> "Online — respondendo com Gemini (funciona sem fichas)"
                    online -> "Online"
                    else -> "Offline, usando conhecimento local"
                },
                Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary
            )
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { vm.ask(vm.lastQuestion.value, "other") }, label = { Text("Explique de outro jeito") }, enabled = vm.lastQuestion.value.isNotBlank() && !vm.busy.value)
                AssistChip(onClick = { vm.ask(vm.lastQuestion.value, "more") }, label = { Text("Mais detalhes") }, enabled = vm.lastQuestion.value.isNotBlank() && !vm.busy.value)
            }
            if (online) TextButton(onClick = { vm.askGemini(vm.lastQuestion.value.ifBlank { input }) }, Modifier.padding(horizontal = 8.dp), enabled = !vm.busy.value) { Text("Resposta mais profunda via Gemini (com consentimento, informa a fonte)") }
            if (vm.busy.value) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            LazyColumn(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                if (vm.messages.isEmpty()) item { GlassSurface { Text("Posso ajudar com seus estudos e com orientações claras. Respostas curtas (2–5 frases), sem ironia. Sobre intenções de outras pessoas, apresento hipóteses, nunca certeza. Se houver sofrimento intenso ou risco, procure alguém de confiança ou o CVV 188.", Modifier.padding(16.dp)) } }
                items(vm.messages) { m ->
                    GlassSurface(Modifier.fillMaxWidth(), corner = 20.dp) {
                        Column(Modifier.padding(12.dp)) {
                            Text(if (m.fromUser) "Você" else "Arborecence", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(m.text, style = MaterialTheme.typography.bodyLarge)
                            if (!m.fromUser && m.source != null) Text("Fonte: ${m.source}${if (m.offline) " • offline" else ""}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}
