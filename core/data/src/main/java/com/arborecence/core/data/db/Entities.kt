package com.arborecence.core.data.db

import androidx.room.*
import com.arborecence.core.domain.model.HighlightColor
import com.arborecence.core.domain.model.NoteSource

@Entity(tableName = "subjects")
data class SubjectEntity(@PrimaryKey val id: String, val name: String, val createdAt: Long)
@Entity(tableName = "topics", indices = [Index("subjectId")])
data class TopicEntity(@PrimaryKey val id: String, val subjectId: String, val name: String, val createdAt: Long)
@Entity(tableName = "notes", indices = [Index("topicId"), Index("subjectId")])
data class NoteEntity(
    @PrimaryKey val id: String,
    val topicId: String?, val subjectId: String?,
    val title: String, val content: String,
    val source: NoteSource, val createdAt: Long, val updatedAt: Long,
)
@Entity(tableName = "highlights", indices = [Index("noteId"), Index("color")])
data class HighlightEntity(
    @PrimaryKey val id: String,
    val noteId: String, val start: Int, val end: Int, val text: String,
    val color: HighlightColor, val meaning: String?, val createdAt: Long,
)
@Entity(tableName = "knowledge_cards", indices = [Index("area")])
data class KnowledgeCardEntity(
    @PrimaryKey val id: String,
    val area: String, val topico: String, val resumoCurto: String, val explicacao: String,
    val conceitosChave: String, val exemplos: String, val nuancesEExcecoes: String,
    val errosComuns: String, val perguntasERespostas: String,
    val nivelDeCerteza: Float, val createdAt: Long,
)
@Entity(tableName = "card_embeddings")
data class CardEmbeddingEntity(@PrimaryKey val cardId: String, val vector: ByteArray)
@Entity(tableName = "review_items")
data class ReviewItemEntity(
    @PrimaryKey val id: String,
    val noteId: String?, val cardId: String?, val question: String, val answer: String,
    val dueAt: Long, val ease: Float, val intervalDays: Int, val repetitions: Int,
)
@Entity(tableName = "learning_progress")
data class LearningProgressEntity(
    @PrimaryKey val singleton: Int = 0,
    val day: Int, val areasCovered: String, val cardCount: Int, val active: Boolean, val lastUpdated: Long,
)

class DbConverters {
    @TypeConverter fun fromColor(c: HighlightColor) = c.name
    @TypeConverter fun toColor(s: String) = HighlightColor.valueOf(s)
    @TypeConverter fun fromSource(s: NoteSource) = s.name
    @TypeConverter fun toSource(s: String) = NoteSource.valueOf(s)
}
