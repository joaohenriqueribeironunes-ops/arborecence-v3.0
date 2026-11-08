package com.arborecence.core.data.repo

import com.arborecence.core.data.db.ArborecenceDb
import com.arborecence.core.data.toDomain
import com.arborecence.core.data.toEntity
import com.arborecence.core.domain.model.ReviewItem
import com.arborecence.core.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

class ReviewRepositoryImpl @Inject constructor(private val db: ArborecenceDb) : ReviewRepository {
    override fun dueItems(now: Long): Flow<List<ReviewItem>> = db.review().due(now).map { it.map { e -> e.toDomain() } }
    override suspend fun upsert(item: ReviewItem) = db.review().upsert(item.toEntity())
    override suspend fun grade(id: String, quality: Int) {
        val e = db.review().byId(id) ?: return
        val q = quality.coerceIn(0, 5)
        var ease = e.ease + (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f))
        ease = max(1.3f, ease)
        val (rep, interval) = if (q < 3) 0 to 1 else {
            val r = e.repetitions + 1
            val i = when (r) { 1 -> 1; 2 -> 6; else -> (e.intervalDays * ease).roundToInt().coerceAtLeast(1) }
            r to i
        }
        val due = System.currentTimeMillis() + interval * 86_400_000L
        db.review().upsert(e.copy(ease = ease, repetitions = rep, intervalDays = interval, dueAt = due))
    }
}
