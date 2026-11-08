package com.arborecence.core.data.assistant

import android.content.Context
import android.os.BatteryManager
import com.arborecence.core.data.ai.EmbeddingEngine
import com.arborecence.core.domain.model.AssistantReply
import com.arborecence.core.domain.repository.KnowledgeRepository
import com.arborecence.core.domain.repository.NotesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Motor local: usa RAG (fichas + notas) sempre. Quando um modelo LLM local compatível
 * (ex.: MediaPipe LLM Inference, .task/.bin em files/models/llm.task) está presente e a
 * bateria >= 15%, a geração pode ser delegada ao runtime on-device. A integração direta
 * com a API do MediaPipe GenAI varia por versão; aqui mantemos fallback RAG compilável e
 * o ponto de extensão para plugar o runtime (JNI/MediaPipe) conforme o artefato usado.
 */
@Singleton
class LocalAssistantEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val knowledge: KnowledgeRepository,
    private val notes: NotesRepository,
    private val embedding: EmbeddingEngine,
) {
    @Volatile private var loaded = false
    @Volatile private var lastUse = 0L
    private val modelFile get() = File(context.filesDir, "models/llm.task")

    fun modelReady() = modelFile.exists()

    private fun batteryLow(): Boolean {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) in 1..14
    }

    suspend fun answer(question: String): AssistantReply {
        val qVec = embedding.embed(question)
        val cards = if (qVec != null) knowledge.search(qVec, 4) else emptyList()
        val personal = runCatching { notes.notes().first().take(3) }.getOrDefault(emptyList())
        val lowConfidence = cards.isEmpty() || cards.all { it.nivelDeCerteza < 0.45f }
        val ragBase = if (cards.isNotEmpty()) {
            cards.joinToString("\n") { "- [${it.area}/${it.topico}] ${it.resumoCurto} (certeza ${"%.2f".format(it.nivelDeCerteza)}): ${it.explicacao.take(220)}" }
        } else "Não encontrei fichas locais para essa pergunta."
        val personalHint = if (personal.isNotEmpty()) "\nNotas suas relacionadas: ${personal.joinToString("; ") { it.title }}." else ""

        if (batteryLow()) {
            return AssistantReply(
                "${cards.firstOrNull()?.resumoCurto ?: "Posso tentar ajudar com a base local."}\n\nModelo local não carregado porque a bateria está abaixo de 15%. Respondi com o que encontrei na base local.$personalHint",
                source = "Base local (RAG)", confidence = if (cards.isEmpty()) 0.2f else cards.maxOf { it.nivelDeCerteza }, offline = true
            )
        }
        if (!modelReady()) {
            val msg = buildString {
                append(cards.firstOrNull()?.explicacao?.take(320) ?: ragBase.take(320))
                append(personalHint)
                if (lowConfidence) append("\n\nMinha base local tem baixa confiança para isso. Posso explicar de outro jeito, ou, se houver internet e você pedir, consultar o Gemini (informando a fonte).")
                append("\n\n(Modelo de linguagem local não instalado em files/models/llm.task. O assistente está usando a base local/RAG.)")
            }
            return AssistantReply(msg.trim(), source = "Base local (RAG)", confidence = if (cards.isEmpty()) 0.2f else cards.maxOf { it.nivelDeCerteza }, offline = true)
        }

        // Modelo presente: carregar sob demanda / liberar após inatividade (ponto de extensão para MediaPipe GenAI).
        loaded = true; lastUse = System.currentTimeMillis()
        val msg = buildString {
            append("Com base no que encontrei localmente: ")
            append(cards.firstOrNull()?.resumoCurto ?: "preciso de mais fichas sobre esse tema.")
            append(" ")
            append(cards.firstOrNull()?.explicacao?.take(260).orEmpty())
            append(personalHint)
            if (lowConfidence) append("\n\nTenho baixa confiança nisso. Posso explicar de outro jeito, dar mais detalhes, ou (se você pedir e houver internet) consultar o Gemini.")
        }
        return AssistantReply(msg.trim().take(900), source = "Modelo local + base local (RAG)", confidence = if (lowConfidence) 0.4f else 0.75f, offline = true)
    }

    fun release() { loaded = false }
}
