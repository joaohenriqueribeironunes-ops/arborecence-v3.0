package com.arborecence.di

import com.arborecence.BuildConfig
import com.arborecence.core.common.GeminiConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun geminiConfig() = GeminiConfig(BuildConfig.GEMINI_API_KEY)
}
