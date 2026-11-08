package com.arborecence.core.data.di

import android.content.Context
import androidx.room.Room
import com.arborecence.core.data.ai.EmbeddingEngine
import com.arborecence.core.data.ai.MediaPipeEmbeddingEngine
import com.arborecence.core.data.db.ArborecenceDb
import com.arborecence.core.data.repo.*
import com.arborecence.core.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindsModule {
    @Binds @Singleton abstract fun notes(impl: NotesRepositoryImpl): NotesRepository
    @Binds @Singleton abstract fun knowledge(impl: KnowledgeRepositoryImpl): KnowledgeRepository
    @Binds @Singleton abstract fun review(impl: ReviewRepositoryImpl): ReviewRepository
    @Binds @Singleton abstract fun settings(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds @Singleton abstract fun assistant(impl: AssistantRepositoryImpl): AssistantRepository
    @Binds @Singleton abstract fun embedding(impl: MediaPipeEmbeddingEngine): EmbeddingEngine
}

@Module
@InstallIn(SingletonComponent::class)
object DbModule {
    @Provides @Singleton
    fun db(@ApplicationContext ctx: Context): ArborecenceDb =
        Room.databaseBuilder(ctx, ArborecenceDb::class.java, "arborecence.db")
            // For production hardening, integrate SQLCipher SupportFactory with a Keystore-managed key.
            .fallbackToDestructiveMigration()
            .build()
}
