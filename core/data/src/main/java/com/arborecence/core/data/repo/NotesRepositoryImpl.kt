package com.arborecence.core.data.repo

import com.arborecence.core.data.db.ArborecenceDb
import com.arborecence.core.data.toDomain
import com.arborecence.core.data.toEntity
import com.arborecence.core.domain.model.*
import com.arborecence.core.domain.repository.NotesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NotesRepositoryImpl @Inject constructor(private val db: ArborecenceDb) : NotesRepository {
    override fun subjects(): Flow<List<Subject>> = db.subjects().all().map { it.map { e -> e.toDomain() } }
    override fun topics(subjectId: String): Flow<List<Topic>> = db.topics().bySubject(subjectId).map { it.map { e -> e.toDomain() } }
    override fun notes(topicId: String?, subjectId: String?): Flow<List<Note>> = db.notes().list(topicId, subjectId).map { it.map { e -> e.toDomain() } }
    override fun note(id: String): Flow<Note?> = db.notes().byId(id).map { it?.toDomain() }
    override fun highlights(noteId: String): Flow<List<Highlight>> = db.highlights().byNote(noteId).map { it.map { e -> e.toDomain() } }
    override fun highlightsByColor(color: HighlightColor): Flow<List<Highlight>> = db.highlights().byColor(color).map { it.map { e -> e.toDomain() } }
    override suspend fun upsertSubject(subject: Subject) = db.subjects().upsert(subject.toEntity())
    override suspend fun upsertTopic(topic: Topic) = db.topics().upsert(topic.toEntity())
    override suspend fun upsertNote(note: Note): String { db.notes().upsert(note.toEntity()); return note.id }
    override suspend fun addHighlight(h: Highlight) = db.highlights().upsert(h.toEntity())
    override suspend fun deleteHighlight(id: String) = db.highlights().delete(id)
    override suspend fun deleteNote(id: String) = db.notes().delete(id)
}
