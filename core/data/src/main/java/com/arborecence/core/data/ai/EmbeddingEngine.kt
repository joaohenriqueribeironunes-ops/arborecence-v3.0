package com.arborecence.core.data.ai

import android.content.Context
import com.arborecence.core.data.cosine
import com.arborecence.core.data.toBytes
import com.arborecence.core.data.toFloats
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface EmbeddingEngine {
    fun isReady(): Boolean
    suspend fun embed(text: String): FloatArray?
}

@Singleton
class MediaPipeEmbeddingEngine @Inject constructor(@ApplicationContext private val context: Context) : EmbeddingEngine {
    @Volatile private var embedder: TextEmbedder? = null
    private val modelFile get() = File(context.filesDir, "models/embedding.tflite")

    override fun isReady(): Boolean = modelFile.exists()
    override suspend fun embed(text: String): FloatArray? {
        if (!isReady()) return null
        val e = embedder ?: runCatching {
            val opts = TextEmbedder.TextEmbedderOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath(modelFile.absolutePath).build()).build()
            // MediaPipe may require model in assets; if absolute path fails, callers should place model appropriately.
            TextEmbedder.createFromOptions(context, opts)
        }.getOrElse {
            runCatching {
                val opts = TextEmbedder.TextEmbedderOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath("embedding.tflite").build()).build()
                TextEmbedder.createFromOptions(context, opts)
            }.getOrNull()
        } ?: return null
        embedder = e
        return runCatching { e.embed(text).embeddingResult().embeddings().firstOrNull()?.floatEmbedding() }.getOrNull()
    }
}

fun rankBySimilarity(query: FloatArray, candidates: List<Pair<String, ByteArray>>, limit: Int): List<String> =
    candidates.map { (id, bytes) -> id to cosine(query, bytes.toFloats()) }.sortedByDescending { it.second }.take(limit).map { it.first }
