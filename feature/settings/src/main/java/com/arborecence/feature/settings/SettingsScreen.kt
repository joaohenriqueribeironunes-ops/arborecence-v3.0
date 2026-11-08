package com.arborecence.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arborecence.core.data.connectivity.ConnectivityObserver
import com.arborecence.core.domain.model.AccessibilitySettings
import com.arborecence.core.domain.repository.SettingsRepository
import com.arborecence.core.ui.components.GlassSurface
import com.arborecence.core.ui.components.IosStyleSwitch
import com.arborecence.core.ui.components.SectionTitle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repo: SettingsRepository, connectivity: ConnectivityObserver) : ViewModel() {
    val acc = repo.accessibility().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccessibilitySettings())
    val online = connectivity.online().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    fun set(acc: AccessibilitySettings) = viewModelScope.launch { repo.setAccessibility(acc) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val ctx = LocalContext.current
    val acc by vm.acc.collectAsState()
    val online by vm.online.collectAsState()
    val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
    val ignoring = pm.isIgnoringBatteryOptimizations(ctx.packageName)
    val modelsDir = File(ctx.filesDir, "models")
    val modelBytes = modelsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, topBar = { TopAppBar(title = { Text("Configurações") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassSurface { Column(Modifier.padding(16.dp)) { Text(if (online) "Online" else "Offline, usando conhecimento local", style = MaterialTheme.typography.titleMedium) } }
            SectionTitle("Acessibilidade (TEA + TDAH)")
            GlassSurface { Column(Modifier.padding(16.dp)) {
                IosStyleSwitch(acc.reduceMotion, { vm.set(acc.copy(reduceMotion = it)) }, "Reduzir movimento (animações lentas/opcionais)")
                IosStyleSwitch(acc.reduceTransparency, { vm.set(acc.copy(reduceTransparency = it)) }, "Reduzir transparência (vidro sólido)")
                IosStyleSwitch(acc.highContrast, { vm.set(acc.copy(highContrast = it)) }, "Contraste reforçado")
                Text("Fonte: ${"%.2f".format(acc.fontScale)}x", Modifier.padding(top = 8.dp))
                Slider(acc.fontScale, { vm.set(acc.copy(fontScale = it)) }, valueRange = 0.85f..1.5f)
            } }
            SectionTitle("Bateria (MIUI)")
            GlassSurface { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (ignoring) "Otimização de bateria desativada para o app." else "MIUI pode limitar o app em segundo plano. Recomendo desativar otimização de bateria e ativar Autostart.")
                if (!ignoring) OutlinedButton(onClick = { ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))) }) { Text("Desativar otimização de bateria") }
                OutlinedButton(onClick = { runCatching { ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))) } }) { Text("Abrir configurações do app (Autostart)") }
            } }
            SectionTitle("Modelos locais e armazenamento")
            GlassSurface { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Espaço usado pelos modelos: ${modelBytes / 1024 / 1024} MB")
                Text("Na configuração inicial (uma vez, com internet), baixe: LLM local quantizado, embeddings, STT pt-BR offline e voz TTS pt-BR. O app funciona offline depois disso.")
                OutlinedButton(onClick = { runCatching { ctx.startActivity(Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, Uri.parse("package:${ctx.packageName}"))) } }) { Text("Gerenciar voz do sistema (TTS pt-BR)") }
                OutlinedButton(onClick = { if (modelsDir.exists()) modelsDir.deleteRecursively() }, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Remover modelos baixados") }
            } }
            SectionTitle("Privacidade")
            GlassSurface { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tudo local por padrão (Room). Ao Gemini vai só o necessário para gerar fichas; notas pessoais só com consentimento explícito; nunca áudio bruto. Áudio de descarregamento é descartado após transcrição.")
                Text("Chave da API Gemini: mantida fora do APK (BuildConfig/local.properties/CI). Para produção, use proxy ou Keystore.")
                OutlinedButton(onClick = { /* export hook: implement SAF document export */ }, enabled = false) { Text("Exportar todos os dados (em breve)") }
                OutlinedButton(onClick = { ctx.deleteDatabase("arborecence.db"); ctx.filesDir.deleteRecursively() }, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Apagar todos os dados") }
            } }
            Spacer(Modifier.height(80.dp))
        }
    }
}
