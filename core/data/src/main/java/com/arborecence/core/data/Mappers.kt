package com.arborecence.core.data

import com.arborecence.core.data.db.*
import com.arborecence.core.domain.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.nio.ByteBuffer

private val json = Json { ignoreUnknownKeys = true }
private fun List<String>.pack() = json.encodeToString(ListSerializer(String.serializer()), this)
private fun String.unpackStrings(): List<String> = runCatching { json.decodeFromString(ListSerializer(String.serializer()), this) }.getOrDefault(emptyList())
private fun List<Qa>.packQa() = json.encodeToString(this.map { mapOf("p" to it.pergunta, "r" to it.resposta) })
private fun String.unpackQa(): List<Qa> = runCatching {
    json.decodeFromString<List<Map<String, String>>>(this).map { Qa(it["p"].orEmpty(), it["r"].orEmpty()) }
}.getOrDefault(emptyList())

fun SubjectEntity.toDomain() = Subject(id, name, createdAt)
fun Subject.toEntity() = SubjectEntity(id, name, createdAt)
fun TopicEntity.toDomain() = Topic(id, subjectId, name, createdAt)
fun Topic.toEntity() = TopicEntity(id, subjectId, name, createdAt)
fun NoteEntity.toDomain() = Note(id, topicId, subjectId, title, content, source, createdAt, updatedAt)
fun Note.toEntity() = NoteEntity(id, topicId, subjectId, title, content, source, createdAt, updatedAt)
fun HighlightEntity.toDomain() = Highlight(id, noteId, start, end, text, color, meaning, createdAt)
fun Highlight.toEntity() = HighlightEntity(id, noteId, start, end, text, color, meaning, createdAt)
fun KnowledgeCardEntity.toDomain() = KnowledgeCard(id, area, topico, resumoCurto, explicacao, conceitosChave.unpackStrings(), exemplos.unpackStrings(), nuancesEExcecoes.unpackStrings(), errosComuns.unpackStrings(), perguntasERespostas.unpackQa(), nivelDeCerteza, createdAt)
fun KnowledgeCard.toEntity() = KnowledgeCardEntity(id, area, topico, resumoCurto, explicacao, conceitosChave.pack(), exemplos.pack(), nuancesEExcecoes.pack(), errosComuns.pack(), perguntasERespostas.packQa(), nivelDeCerteza, createdAt)
fun ReviewItemEntity.toDomain() = ReviewItem(id, noteId, cardId, question, answer, dueAt, ease, intervalDays, repetitions)
fun ReviewItem.toEntity() = ReviewItemEntity(id, noteId, cardId, question, answer, dueAt, ease, intervalDays, repetitions)
fun LearningProgressEntity.toDomain() = LearningProgress(day, areasCovered.unpackStrings(), cardCount, active, lastUpdated)
fun LearningProgress.toEntity() = LearningProgressEntity(0, day, areasCovered.pack(), cardCount, active, lastUpdated)

fun FloatArray.toBytes(): ByteArray = ByteBuffer.allocate(size * 4).apply { for (f in this@toBytes) putFloat(f) }.array()
fun ByteArray.toFloats(): FloatArray { val b = ByteBuffer.wrap(this); return FloatArray(remaining() / 4) { b.float } }
fun cosine(a: FloatArray, b: FloatArray): Float {
    if (a.isEmpty() || b.isEmpty() || a.size != b.size) return 0f
    var dot = 0f; var na = 0f; var nb = 0f
    for (i in a.indices) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i] }
    val d = kotlin.math.sqrt(na) * kotlin.math.sqrt(nb)
    return if (d == 0f) 0f else dot / d
}
