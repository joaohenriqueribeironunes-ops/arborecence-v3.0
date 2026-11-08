package com.arborecence.core.domain.model

import java.util.UUID

enum class HighlightColor(val key: String, val defaultMeaning: String) {
    LAVENDER("lavender", "Ideia central"),
    MINT("mint", "Entendi bem"),
    AMBER("amber", "Revisar"),
    WATER("water", "Dúvida"),
}

data class Subject(val id: String = UUID.randomUUID().toString(), val name: String, val createdAt: Long = System.currentTimeMillis())
data class Topic(val id: String = UUID.randomUUID().toString(), val subjectId: String, val name: String, val createdAt: Long = System.currentTimeMillis())
data class Note(
    val id: String = UUID.randomUUID().toString(),
    val topicId: String? = null,
    val subjectId: String? = null,
    val title: String,
    val content: String,
    val source: NoteSource = NoteSource.MANUAL,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
enum class NoteSource { MANUAL, VOICE_DUMP, ASSISTANT }

data class Highlight(
    val id: String = UUID.randomUUID().toString(),
    val noteId: String,
    val start: Int,
    val end: Int,
    val text: String,
    val color: HighlightColor,
    val meaning: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

data class KnowledgeCard(
    val id: String = UUID.randomUUID().toString(),
    val area: String,
    val topico: String,
    val resumoCurto: String,
    val explicacao: String,
    val conceitosChave: List<String> = emptyList(),
    val exemplos: List<String> = emptyList(),
    val nuancesEExcecoes: List<String> = emptyList(),
    val errosComuns: List<String> = emptyList(),
    val perguntasERespostas: List<Qa> = emptyList(),
    val nivelDeCerteza: Float = 0f,
    val createdAt: Long = System.currentTimeMillis(),
)
data class Qa(val pergunta: String, val resposta: String)

data class LearningProgress(
    val day: Int = 0, // 0 = not started, 1-14
    val areasCovered: List<String> = emptyList(),
    val cardCount: Int = 0,
    val active: Boolean = false,
    val lastUpdated: Long = 0L,
)

data class ReviewItem(
    val id: String = UUID.randomUUID().toString(),
    val noteId: String? = null,
    val cardId: String? = null,
    val question: String,
    val answer: String,
    val dueAt: Long,
    val ease: Float = 2.5f,
    val intervalDays: Int = 1,
    val repetitions: Int = 0,
)

data class ModelAsset(
    val id: String,
    val name: String,
    val type: ModelType,
    val url: String? = null,
    val sha256: String? = null,
    val sizeBytes: Long = 0,
    val localPath: String? = null,
    val downloaded: Boolean = false,
)
enum class ModelType { LLM, EMBEDDING, STT, TTS }

data class AccessibilitySettings(
    val reduceMotion: Boolean = false,
    val reduceTransparency: Boolean = false,
    val fontScale: Float = 1.0f,
    val highContrast: Boolean = false,
)

data class AssistantReply(val text: String, val source: String, val confidence: Float = 1f, val offline: Boolean = true)
enum class ConnectivityState { OFFLINE, ONLINE }
