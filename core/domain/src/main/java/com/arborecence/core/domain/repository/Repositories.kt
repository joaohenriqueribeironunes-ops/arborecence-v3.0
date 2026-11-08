package com.arborecence.core.domain.repository

import com.arborecence.core.domain.model.*
import kotlinx.coroutines.flow.Flow

interface NotesRepository {
    fun subjects(): Flow<List<Subject>>
    fun topics(subjectId: String): Flow<List<Topic>>
    fun notes(topicId: String? = null, subjectId: String? = null): Flow<List<Note>>
    fun note(id: String): Flow<Note?>
    fun highlights(noteId: String): Flow<List<Highlight>>
    fun highlightsByColor(color: HighlightColor): Flow<List<Highlight>>
    suspend fun upsertSubject(subject: Subject)
    suspend fun upsertTopic(topic: Topic)
    suspend fun upsertNote(note: Note): String
    suspend fun addHighlight(h: Highlight)
    suspend fun deleteHighlight(id: String)
    suspend fun deleteNote(id: String)
}

interface KnowledgeRepository {
    fun progress(): Flow<LearningProgress>
    fun cards(area: String? = null): Flow<List<KnowledgeCard>>
    suspend fun cardCount(): Int
    suspend fun addCards(cards: List<KnowledgeCard>)
    suspend fun search(queryEmbedding: FloatArray, limit: Int = 5): List<KnowledgeCard>
    suspend fun updateProgress(p: LearningProgress)
}

interface ReviewRepository {
    fun dueItems(now: Long = System.currentTimeMillis()): Flow<List<ReviewItem>>
    suspend fun upsert(item: ReviewItem)
    suspend fun grade(id: String, quality: Int) // 0-5 SM-2 style
}

interface AssistantRepository {
    fun isOnline(): Flow<Boolean>
    fun isGeminiConfigured(): Boolean
    suspend fun askLocal(question: String): AssistantReply
    suspend fun askGemini(question: String, personalConsent: Boolean): AssistantReply
}

interface SettingsRepository {
    fun accessibility(): Flow<AccessibilitySettings>
    suspend fun setAccessibility(s: AccessibilitySettings)
    fun highlightMeaning(color: HighlightColor): Flow<String>
    suspend fun setHighlightMeaning(color: HighlightColor, meaning: String)
}
