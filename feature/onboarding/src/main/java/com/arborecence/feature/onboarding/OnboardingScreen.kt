package com.arborecence.feature.onboarding

import android.Manifest
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arborecence.core.ui.components.GlassSurface
import com.arborecence.core.ui.components.PrimaryGlassButton
import com.arborecence.core.ui.components.SectionTitle
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingRoute(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
    var micOk by remember { mutableStateOf(false) }
    var notifOk by remember { mutableStateOf(true) }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { micOk = it }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifOk = it }
    val modelsDir = File(ctx.filesDir, "models")
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, topBar = { TopAppBar(title = { Text("Configuração inicial") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Uma configuração guiada. Depois disso, o app funciona offline.", style = MaterialTheme.typography.bodyLarge)
            SectionTitle("1. Permissões")
            GlassSurface { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }) { Text(if (micOk) "Microfone: concedido" else "Permitir microfone (para voz)") }
                if (Build.VERSION.SDK_INT >= 33) OutlinedButton(onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Permitir notificações (serviços em primeiro plano)") }
            } }
            SectionTitle("2. Bateria e MIUI")
            GlassSurface { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("No Redmi/MIUI, ative Autostart e desative a otimização de bateria para o Arborecence funcionar de forma confiável.")
                OutlinedButton(onClick = { ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))) }) { Text("Desativar otimização de bateria") }
                OutlinedButton(onClick = { runCatching { ctx.startActivity(Intent("miui.intent.action.APP_PERM_EDITOR").apply { putExtra("extra_pkgname", ctx.packageName); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }.onFailure { ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))) } }) { Text("Abrir Autostart / detalhes do app") }
            } }
            SectionTitle("3. Atalho no painel de controle")
            GlassSurface { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (Build.VERSION.SDK_INT >= 33) {
                    OutlinedButton(onClick = {
                        val sbm = ctx.getSystemService(StatusBarManager::class.java)
                        sbm.requestAddTileService(ComponentName(ctx, "com.arborecence.service.ArborecenceTileService"), "Arborecence", Icon.createWithResource(ctx, android.R.drawable.ic_btn_speak_now), Executors.newSingleThreadExecutor()) {}
                    }) { Text("Adicionar tile 'Arborecence'") }
                } else {
                    Text("Android 11–12: abra o painel de configurações rápidas, toque em editar e arraste o tile 'Arborecence' para os atalhos.")
                }
            } }
            SectionTitle("4. Modelos offline (download único)")
            GlassSurface { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Com internet, baixe e verifique por hash: (1) LLM local quantizado, (2) embeddings, (3) STT pt-BR offline, (4) voz TTS pt-BR.")
                Text("Status local: pasta models ${if (modelsDir.exists()) "criada" else "não criada"} (${modelsDir.walkTopDown().filter{it.isFile}.count()} arquivos).")
                Text("Para STT offline completo (Whisper), adicione o modelo e binários whisper.cpp em files/models. O app usa reconhecimento do sistema como base e funciona com o pacote de voz pt-BR offline.")
                OutlinedButton(onClick = { runCatching { ctx.startActivity(Intent(Settings.ACTION_SETTINGS)) } }) { Text("Abrir configurações do sistema (voz/TTS pt-BR)") }
                Text("Download/verificação automática com hash: configure URLs e SHA-256 em Configurações > Modelos (ou forneça os arquivos em files/models). Gerenciador completo implementado na camada de dados (ModelManager).")
            } }
            PrimaryGlassButton("Concluir e ir para o início", onDone)
            Spacer(Modifier.height(40.dp))
        }
    }
}
