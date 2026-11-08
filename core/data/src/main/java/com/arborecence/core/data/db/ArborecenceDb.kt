package com.arborecence.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [SubjectEntity::class, TopicEntity::class, NoteEntity::class, HighlightEntity::class,
        KnowledgeCardEntity::class, CardEmbeddingEntity::class, ReviewItemEntity::class, LearningProgressEntity::class],
    version = 1, exportSchema = false,
)
@TypeConverters(DbConverters::class)
abstract class ArborecenceDb : RoomDatabase() {
    abstract fun subjects(): SubjectDao
    abstract fun topics(): TopicDao
    abstract fun notes(): NoteDao
    abstract fun highlights(): HighlightDao
    abstract fun knowledge(): KnowledgeDao
    abstract fun review(): ReviewDao
    abstract fun progress(): ProgressDao
}
