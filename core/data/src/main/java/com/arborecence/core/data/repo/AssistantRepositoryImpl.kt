package com.arborecence.core.data.repo

import com.arborecence.core.data.assistant.LocalAssistantEngine
import com.arborecence.core.data.connectivity.ConnectivityObserver
import com.arborecence.core.domain.model.AssistantReply
import com.arborecence.core.domain.repository.AssistantRepository
import com.arborecence.core.common.GeminiConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AssistantRepositoryImpl @Inject constructor(
    private val local: LocalAssistantEngine,
    private val connectivity: ConnectivityObserver,
    private val config: GeminiConfig,
) : AssistantRepository {
    override fun isOnline(): Flow<Boolean> = connectivity.online()
    override fun isGeminiConfigured(): Boolean = config.apiKey.isNotBlank()
    override suspend fun askLocal(question: String): AssistantReply = local.answer(question)
    override suspend fun askGemini(question: String, personalConsent: Boolean): AssistantReply {
        if (!connectivity.online().first()) return AssistantReply("Sem internet no momento. Posso responder com a base local.", "Offline", 0f, true)
        val key = config.apiKey
        if (key.isBlank()) return AssistantReply("Gemini não configurado (chave ausente). Usando resposta local.", "Configuração", 0f, true)
        val prompt = "Responda em pt-BR, de forma clara, literal, neurodivergente-afirmativa, sem diagnóstico. Use 2-6 frases. Se não souber, diga com honestidade. Pergunta: $question"
        val models = listOf("gemini-1.5-flash", "gemini-1.5-flash-latest", "gemini-2.0-flash", "gemini-2.0-flash-lite")
        var lastErr: String? = null
        for (m in models) {
            val res = runCatching { GenerativeModel(m, key).generateContent(prompt).text?.trim().orEmpty() }
            res.onSuccess { t -> if (t.isNotBlank()) return AssistantReply(t, source = "Gemini ($m, online)", confidence = 0.85f, offline = false) }
            res.onFailure { lastErr = it.message }
        }
        val local = local.answer(question)
        return AssistantReply(
            local.text + "\n\n(Não consegui consultar o Gemini: ${lastErr?.take(120) ?: "resposta vazia"}. Respondi com a base local.)",
            source = "Base local (Gemini indisponível)", confidence = local.confidence, offline = true
        )
    }
}
