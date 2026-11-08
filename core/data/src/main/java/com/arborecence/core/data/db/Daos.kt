package com.arborecence.core.data.db

import androidx.room.*
import com.arborecence.core.domain.model.HighlightColor
import kotlinx.coroutines.flow.Flow

@Dao interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY createdAt DESC") fun all(): Flow<List<SubjectEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(e: SubjectEntity)
}
@Dao interface TopicDao {
    @Query("SELECT * FROM topics WHERE subjectId=:subjectId ORDER BY createdAt DESC") fun bySubject(subjectId: String): Flow<List<TopicEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(e: TopicEntity)
}
@Dao interface NoteDao {
    @Query("SELECT * FROM notes WHERE (:topicId IS NULL OR topicId=:topicId) AND (:subjectId IS NULL OR subjectId=:subjectId) ORDER BY updatedAt DESC")
    fun list(topicId: String?, subjectId: String?): Flow<List<NoteEntity>>
    @Query("SELECT * FROM notes WHERE id=:id") fun byId(id: String): Flow<NoteEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(e: NoteEntity)
    @Query("DELETE FROM notes WHERE id=:id") suspend fun delete(id: String)
}
@Dao interface HighlightDao {
    @Query("SELECT * FROM highlights WHERE noteId=:noteId ORDER BY start ASC") fun byNote(noteId: String): Flow<List<HighlightEntity>>
    @Query("SELECT * FROM highlights WHERE color=:color ORDER BY createdAt DESC") fun byColor(color: HighlightColor): Flow<List<HighlightEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(e: HighlightEntity)
    @Query("DELETE FROM highlights WHERE id=:id") suspend fun delete(id: String)
}
@Dao interface KnowledgeDao {
    @Query("SELECT * FROM knowledge_cards WHERE (:area IS NULL OR area=:area) ORDER BY createdAt DESC") fun cards(area: String?): Flow<List<KnowledgeCardEntity>>
    @Query("SELECT COUNT(*) FROM knowledge_cards") suspend fun count(): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(cards: List<KnowledgeCardEntity>)
    @Query("SELECT * FROM card_embeddings") suspend fun allEmbeddings(): List<CardEmbeddingEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertEmbedding(e: CardEmbeddingEntity)
    @Query("SELECT * FROM knowledge_cards WHERE id IN (:ids)") suspend fun byIds(ids: List<String>): List<KnowledgeCardEntity>
}
@Dao interface ReviewDao {
    @Query("SELECT * FROM review_items WHERE dueAt <= :now ORDER BY dueAt ASC") fun due(now: Long): Flow<List<ReviewItemEntity>>
    @Query("SELECT * FROM review_items WHERE id=:id") suspend fun byId(id: String): ReviewItemEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(e: ReviewItemEntity)
}
@Dao interface ProgressDao {
    @Query("SELECT * FROM learning_progress WHERE singleton=0") fun get(): Flow<LearningProgressEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(e: LearningProgressEntity)
}
