package com.arborecence.core.data

import com.arborecence.core.data.ai.CardDto
import com.arborecence.core.data.ai.QaDto
import com.arborecence.core.data.ai.rankBySimilarity
import com.arborecence.core.domain.model.Highlight
import com.arborecence.core.domain.model.HighlightColor
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class RagAndParsingTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun cosineSimilarityRanksClosest() {
        val q = floatArrayOf(1f, 0f)
        val a = "a" to floatArrayOf(1f, 0f).toBytes()
        val b = "b" to floatArrayOf(0f, 1f).toBytes()
        val c = "c" to floatArrayOf(0.8f, 0.2f).toBytes()
        val ranked = rankBySimilarity(q, listOf(a, b, c), 2)
        assertEquals(listOf("a", "c"), ranked)
    }

    @Test fun cosineHandlesEmptyAndMismatch() {
        assertEquals(0f, cosine(floatArrayOf(), floatArrayOf(1f)), 0.0001f)
        assertEquals(0f, cosine(floatArrayOf(1f), floatArrayOf(1f, 2f)), 0.0001f)
    }

    @Test fun parsesGeminiKnowledgeCardJson() {
        val raw = """
            {"area":"História","topico":"Brasil colonial","resumo_curto":"Período inicial.","explicacao":"Explicação factual.",
             "conceitos_chave":["capitanias"],"exemplos":["cana"],"nuances_e_excecoes":["contexto varia"],
             "erros_comuns":["generalizar"],"perguntas_e_respostas":[{"pergunta":"Q?","resposta":"R."}],"nivel_de_certeza":0.8}
        """.trimIndent()
        val dto = json.decodeFromString<CardDto>(raw)
        assertEquals("História", dto.area)
        assertEquals(0.8f, dto.nivel_de_certeza, 0.001f)
        assertEquals(1, dto.perguntas_e_respostas.size)
        assertEquals("Q?", dto.perguntas_e_respostas[0].pergunta)
    }

    @Test fun highlightRangeValidation() {
        val text = "Aprendi que atenção varia."
        val h = Highlight(noteId = "n", start = 0, end = 7, text = text.substring(0, 7), color = HighlightColor.LAVENDER)
        assertTrue(h.start >= 0 && h.end <= text.length && h.start < h.end)
        assertEquals("Aprendi", h.text)
    }

    @Test fun floatBytesRoundTrip() {
        val v = floatArrayOf(0.1f, -2f, 3.5f)
        assertArrayEquals(v, v.toBytes().toFloats(), 0.0001f)
    }
}
