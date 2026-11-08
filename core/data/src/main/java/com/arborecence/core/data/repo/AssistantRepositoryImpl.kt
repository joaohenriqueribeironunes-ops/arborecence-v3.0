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
    override suspend fun askLocal(question: String): AssistantReply = local.answer(question)
    override suspend fun askGemini(question: String, personalConsent: Boolean): AssistantReply {
        if (!connectivity.online().first()) return AssistantReply("Sem internet no momento. Posso responder com a base local.", "Offline", 0f, true)
        val key = config.apiKey
        if (key.isBlank()) return AssistantReply("Gemini não configurado (chave ausente). Usando resposta local.", "Configuração", 0f, true)
        val text = runCatching {
            GenerativeModel("gemini-1.5-flash", key).generateContent(
                "Responda em pt-BR, de forma clara, literal, neurodivergente-afirmativa, sem diagnóstico. 2-6 frases. Pergunta: $question"
            ).text.orEmpty()
        }.getOrElse { "Não consegui consultar o Gemini agora: ${it.message?.take(80)}" }
        return AssistantReply(text, source = "Gemini (online, com consentimento)", confidence = 0.8f, offline = false)
    }
}
