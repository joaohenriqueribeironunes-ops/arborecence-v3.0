package com.arborecence.core.data.repo

import com.arborecence.core.data.ai.EmbeddingEngine
import com.arborecence.core.data.ai.rankBySimilarity
import com.arborecence.core.data.db.ArborecenceDb
import com.arborecence.core.data.db.CardEmbeddingEntity
import com.arborecence.core.data.db.LearningProgressEntity
import com.arborecence.core.data.toBytes
import com.arborecence.core.data.toDomain
import com.arborecence.core.data.toEntity
import com.arborecence.core.domain.model.KnowledgeCard
import com.arborecence.core.domain.model.LearningProgress
import com.arborecence.core.domain.repository.KnowledgeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class KnowledgeRepositoryImpl @Inject constructor(
    private val db: ArborecenceDb,
    private val embedding: EmbeddingEngine,
) : KnowledgeRepository {
    override fun progress(): Flow<LearningProgress> = db.progress().get().map { it?.toDomain() ?: LearningProgress() }
    override fun cards(area: String?): Flow<List<KnowledgeCard>> = db.knowledge().cards(area).map { it.map { e -> e.toDomain() } }
    override suspend fun cardCount(): Int = db.knowledge().count()
    override suspend fun addCards(cards: List<KnowledgeCard>) {
        db.knowledge().insertAll(cards.map { it.toEntity() })
        for (c in cards) {
            val text = listOf(c.area, c.topico, c.resumoCurto, c.explicacao, c.conceitosChave.joinToString(", ")).joinToString("\n")
            val vec = embedding.embed(text) ?: continue
            db.knowledge().upsertEmbedding(CardEmbeddingEntity(c.id, vec.toBytes()))
        }
    }
    override suspend fun search(queryEmbedding: FloatArray, limit: Int): List<KnowledgeCard> {
        val ids = rankBySimilarity(queryEmbedding, db.knowledge().allEmbeddings().map { it.cardId to it.vector }, limit)
        if (ids.isEmpty()) return emptyList()
        val byId = db.knowledge().byIds(ids).associateBy { it.id }
        return ids.mapNotNull { byId[it]?.toDomain() }
    }
    override suspend fun updateProgress(p: LearningProgress) = db.progress().upsert(p.toEntity())
}
