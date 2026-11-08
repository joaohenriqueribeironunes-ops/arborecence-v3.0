package com.arborecence.core.data.ai

import com.arborecence.core.domain.model.KnowledgeCard
import com.arborecence.core.domain.model.Qa
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
data class CardDto(
    val area: String = "", val topico: String = "", val resumo_curto: String = "",
    val explicacao: String = "", val conceitos_chave: List<String> = emptyList(),
    val exemplos: List<String> = emptyList(), val nuances_e_excecoes: List<String> = emptyList(),
    val erros_comuns: List<String> = emptyList(),
    val perguntas_e_respostas: List<QaDto> = emptyList(), val nivel_de_certeza: Float = 0.5f,
)
@Serializable data class QaDto(val pergunta: String = "", val resposta: String = "")

class GeminiCurriculumClient @Inject constructor() {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun isConfigured(apiKey: String) = apiKey.isNotBlank()

    suspend fun fetchCard(apiKey: String, area: String, topic: String): Result<KnowledgeCard> = runCatching {
        val model = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey,
            generationConfig = generationConfig { responseMimeType = "application/json"; temperature = 0.2f },
        )
        val prompt = """
Você é um assistente factual. Gere UMA ficha de conhecimento em português do Brasil sobre o tópico abaixo.
Responda SOMENTE com JSON válido, sem markdown, seguindo exatamente este schema:
{"area":"","topico":"","resumo_curto":"","explicacao":"","conceitos_chave":[],"exemplos":[],"nuances_e_excecoes":[],"erros_comuns":[],"perguntas_e_respostas":[{"pergunta":"","resposta":""}],"nivel_de_certeza":0.0}
Regras: seja factual; indique incerteza em nivel_de_certeza (0-1); marque o que for controverso ou dependente de contexto.
Legislação brasileira: leis mudam; isso NÃO substitui advogado. Saúde/neurociência clínica: NÃO substitui profissional.
Nunca invente fatos; se não souber, reduza nivel_de_certeza e explique a limitação em nuances_e_excecoes.
Área: $area
Tópico: $topic
""".trim()
        val text = model.generateContent(content { text(prompt) }).text.orEmpty().trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val dto = json.decodeFromString<CardDto>(text)
        KnowledgeCard(
            area = dto.area.ifBlank { area }, topico = dto.topico.ifBlank { topic },
            resumoCurto = dto.resumo_curto, explicacao = dto.explicacao,
            conceitosChave = dto.conceitos_chave, exemplos = dto.exemplos,
            nuancesEExcecoes = dto.nuances_e_excecoes, errosComuns = dto.erros_comuns,
            perguntasERespostas = dto.perguntas_e_respostas.map { Qa(it.pergunta, it.resposta) },
            nivelDeCerteza = dto.nivel_de_certeza.coerceIn(0f, 1f),
        )
    }
}

object CurriculumPlan {
    val areas: List<Pair<String, List<String>>> = listOf(
        "Filosofia" to listOf("Ética básica", "Lógica", "Estoicismo", "Empirismo e racionalismo"),
        "História" to listOf("Brasil colonial", "República brasileira", "Guerras mundiais", "Direitos civis"),
        "Matemática" to listOf("Aritmética e porcentagem", "Álgebra básica", "Estatística intuitiva", "Raciocínio lógico"),
        "Língua portuguesa" to listOf("Interpretação de texto", "Coesão e coerência", "Concordância", "Comunicação clara"),
        "Geografia" to listOf("Regiões do Brasil", "Clima e ambiente", "Urbanização", "Geopolítica básica"),
        "Sociologia" to listOf("Instituições sociais", "Cultura e normas", "Desigualdade", "Cidadania"),
        "Física" to listOf("Movimento e força", "Energia", "Eletricidade básica", "Ondas e som"),
        "Química" to listOf("Matéria e átomos", "Ligações", "Reações cotidianas", "Água e pH"),
        "Neurociência" to listOf("Neurônios e sinapses", "Plasticidade", "Atenção", "Memória"),
        "Neurologia" to listOf("Sistema nervoso", "Sono", "Sinais de alerta (geral)", "Quando buscar profissional"),
        "Neurofisiologia" to listOf("Potencial de ação", "Neurotransmissores", "Regulação autonômica", "Estresse fisiológico"),
        "Anatomia" to listOf("Sistemas do corpo", "Cérebro visão geral", "Sentidos", "Saúde básica"),
        "Neuroanatomia" to listOf("Córtex", "Límbico", "Cerebelo e tronco", "Redes funcionais"),
        "Fisiologia" to listOf("Homeostase", "Metabolismo", "Resposta ao estresse", "Exercício e corpo"),
        "Psicologia social" to listOf("Influência social", "Normas", "Empatia", "Conflitos"),
        "Neurodivergências" to listOf("Conceito neurodiversidade", "Variação humana", "Acessibilidade", "Neurodivergente-afirmativo"),
        "TDAH e TEA" to listOf("TDAH visão geral", "TEA visão geral", "Apoios e estratégias", "Mitos e fatos (sem diagnóstico)"),
        "Relações humanas" to listOf("Comunicação", "Limites", "Confiança", "Resolução de conflitos"),
        "Amizade" to listOf("Reciprocidade", "Manutenção", "Desencontros", "Apoio"),
        "Relacionamentos" to listOf("Consentimento", "Expectativas", "Comunicação afetiva", "Sinais de respeito"),
        "Jogos sociais" to listOf("Regras implícitas", "Contexto", "Mal-entendidos comuns", "Alternativas claras"),
        "Nuances sociais" to listOf("Tom e contexto", "Linguagem indireta", "Pedir clarificação", "Interpretação cautelosa"),
        "Sensações entre pessoas" to listOf("Nomear emoções", "Regulação emocional", "Empatia sem adivinhar", "Incerteza sobre intenções"),
        "Como pessoas se afetam" to listOf("Contextos familiares", "Trabalho/estudo", "Conflito", "Cooperação"),
        "Visão de mundo" to listOf("Valores", "Perspectivas", "Viés", "Diálogo respeitoso"),
        "Legislação brasileira" to listOf("Constituição (noções)", "Direitos do consumidor", "Acessibilidade (noções)", "Aviso: não substitui advogado"),
        "Conhecimento geral 25 anos" to listOf("Vida prática", "Trabalho e finanças básicas", "Saúde e bem-estar geral", "Cidadania digital"),
    )
    fun topicsForDay(day: Int): List<Pair<String, String>> {
        val all = areas.flatMap { (a, ts) -> ts.map { a to it } }
        if (all.isEmpty()) return emptyList()
        val perDay = (all.size + 13) / 14
        val start = ((day - 1).coerceIn(0, 13)) * perDay
        return all.drop(start).take(perDay)
    }
}
