package com.arborecence.core.data.assistant

import android.content.Context
import android.os.BatteryManager
import com.arborecence.core.data.ai.EmbeddingEngine
import com.arborecence.core.domain.model.AssistantReply
import com.arborecence.core.domain.repository.KnowledgeRepository
import com.arborecence.core.domain.repository.NotesRepository
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAssistantEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val knowledge: KnowledgeRepository,
    private val notes: NotesRepository,
    private val embedding: EmbeddingEngine,
) {
    @Volatile private var llm: LlmInference? = null
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
        val contextText = buildString {
            if (cards.isNotEmpty()) {
                append("Fichas recuperadas:\n")
                cards.forEach { append("- [${it.area}/${it.topico}] ${it.resumoCurto} (certeza ${"%.2f".format(it.nivelDeCerteza)})\n") }
            }
            if (personal.isNotEmpty()) {
                append("Notas pessoais (resumo):\n")
                personal.forEach { append("- ${it.title}: ${it.content.take(120)}\n") }
            }
        }
        val lowConfidence = cards.isEmpty() || cards.all { it.nivelDeCerteza < 0.45f }
        if (batteryLow() || !modelReady()) {
            val base = if (cards.isNotEmpty()) cards.first().explicacao.take(320) else "Não encontrei fichas locais para essa pergunta."
            val msg = buildString {
                append(base)
                if (lowConfidence) append("\n\nMinha base local tem baixa confiança para isso. Posso tentar explicar de outro jeito, ou, se você quiser e houver internet, buscar uma resposta mais profunda.")
                if (!modelReady()) append("\n\n(O modelo de linguagem local ainda não está instalado. Use a tela de Configuração/Onboarding para baixar os modelos.)")
                if (batteryLow()) append("\n\n(Modelo local não carregado porque a bateria está abaixo de 15%.)")
            }
            return AssistantReply(msg.trim(), source = if (cards.isNotEmpty()) "Base local (fichas/notas)" else "Base local", confidence = if (cards.isEmpty()) 0.2f else cards.maxOf { it.nivelDeCerteza }, offline = true)
        }
        val prompt = """Você é um assistente de estudos, neurodivergente-afirmativo, sem julgamento e sem diagnóstico.
Responda em português do Brasil, de forma clara, literal e sem ironia. Seja breve (2 a 5 frases).
Use o contexto recuperado quando ajudar. Se a base for incerta, diga com honestidade.
Sobre intenções de outras pessoas, nunca afirme certeza: apresente hipóteses (pode ser X, Y ou Z).
Para sofrimento intenso ou risco à segurança, acolha e incentive contato com alguém de confiança ou o CVV 188 (Brasil).

Contexto:
$contextText

Pergunta: $question
Resposta:"""
        val text = runCatching {
            val engine = llm ?: LlmInference.createFromOptions(context, LlmInference.LlmInferenceOptions.builder().setModelPath(modelFile.absolutePath).setMaxTokens(384).build()).also { llm = it }
            lastUse = System.currentTimeMillis()
            engine.generateResponse(prompt)
        }.getOrElse { e ->
            "Tive um problema ao usar o modelo local (${e.message?.take(80)}). " + (cards.firstOrNull()?.explicacao?.take(240) ?: "Tente perguntar de outro jeito.")
        }
        val conf = if (lowConfidence) 0.35f else 0.75f
        return AssistantReply(text.trim(), source = "Modelo local + base local (RAG)", confidence = conf, offline = true)
    }

    fun release() { runCatching { llm?.close() }; llm = null }
}
