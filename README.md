# Arborecence v3.0

Central de estudos Android nativa (Kotlin, Jetpack Compose, Room, Hilt, WorkManager, Coroutines/Flow) para uma pessoa com TEA e TDAH, com assistente de voz que, após aprendizado e com modelos instalados, opera de forma local/offline.

## Requisitos do dispositivo
- **Alvo principal:** Redmi Note 11S (Snapdragon 680, 4–6 GB RAM), Android 11–13.
- **Build:** `minSdk 30`, `targetSdk 34`, `compileSdk 35`, Java 17.
- **MIUI (bateria agressiva):** o onboarding orienta ativar Autostart e desativar otimização de bateria para o app.

## Princípio offline-first
- Após configuração inicial e modelos instalados, as funções principais (descarregar por voz, marca-texto, notas, revisão, assistente de voz com base local) funcionam **sem internet**.
- **Configuração inicial (uma vez, com internet):** baixar e verificar por hash os modelos (1) LLM local quantizado, (2) embeddings, (3) STT pt-BR offline, (4) TTS pt-BR. A tela de onboarding/configurações orienta; o gerenciador de modelos baixa para `files/models/` quando URLs+SHA-256 são configurados.
- **Gemini é opcional** e nunca dependência do assistente. A chave vai em `local.properties`/`gradle.properties`/segredo CI (`GEMINI_API_KEY`) — nunca no código-fonte público.
- Estado mostrado na UI: **"Offline, usando conhecimento local"** ou **"Online"**. Configurações mostram espaço dos modelos e permitem remover.

## Módulos e funcionalidades
1. **Ambiente acessível (TEA+TDAH):** interface previsível, uma ação principal por tela, sem flashes, animações lentas/opcionais; modo foco (pomodoro flexível, tarefa única, pausas sensoriais, estacionamento de pensamentos); árvore matéria > tema > nota; reduzir movimento/transparência, fonte ajustável, contraste.
2. **Descarregamento cognitivo por voz:** botão "Descarregar", transcrição pt-BR (SpeechRecognizer com preferência offline; arquitetura preparada para Whisper/whisper.cpp), áudio descartado após transcrever, nota editável. **Marca-texto virtual (4 cores):** lavanda (ideia central), menta (entendi bem), âmbar (revisar), água (dúvida) — significados configuráveis; filtrar por cor e resumo dos trechos da cor.
3. **Aprendizado 14 dias com Gemini (base local, sem treinar pesos):** currículo em 27 áreas (filosofia… legislação brasileira, nuances sociais, TEA/TDAH etc.), fichas JSON com schema definido (resumo, explicação, conceitos, exemplos, nuances/exceções, erros comuns, Q&A, `nivel_de_certeza`). Execução **somente em primeiro plano** (`ForegroundService` tipo `dataSync`, notificação visível) e **somente com internet** (pausa/retoma), com backoff. Fichas no Room + embeddings no aparelho (MediaPipe Text Embedder) + índice de busca por similaridade (RAG). Após o dia 14, continuação opcional em baixa frequência, só com internet e app aberto.
4. **Assistente local de voz:** LLM local quantizado 4-bit via **MediaPipe LLM Inference** (ex.: Gemma 3 1B / Qwen 1.5B em `files/models/llm.task`), carregado sob demanda e liberado após inatividade; **não carrega com bateria < 15%**. Fluxo: voz > transcrição offline > RAG (fichas + notas) > resposta local (streaming quando suportado) > TTS pt-BR offline. Respostas curtas (2–5 frases), claras/literais/sem ironia; "explique de outro jeito" e "mais detalhes". Se confiança baixa, informa com honestidade. Gemini só se houver internet e o usuário pedir, informando a fonte. Memória pessoal local (notas/matérias/preferências). Uso por voz com tela desligada: `ForegroundService` tipo `microphone` iniciado por toque (Quick Settings Tile), microfone só após toque, ícone/notificação visíveis, encerra por silêncio ou comando "encerrar". Conselhos neurodivergente-afirmativos, sem diagnóstico; sofrimento intenso/risco: acolher e indicar apoio (CVV 188). Sobre intenções alheias: apenas hipóteses (X, Y ou Z).

## Integração com o painel de controle
- `TileService` "Arborecence": toque abre/inicia o assistente de voz. Onboarding guia adição do tile (Android 13: `requestAddTileService`; 11–12: passo a passo).

## Design (Liquid Glass)
- Vidro translúcido com blur (`RenderEffect`, API 31+) e fallback semitransparente/gradiente; bordas luminosas, cantos 24–32dp, sombras suaves; gradientes calmos (lavanda, menta, âmbar, água); barra inferior em pílula de vidro; sheets com spring (respeitando reduzir movimento). Vidro sólido com "reduzir transparência". Tipografia estilo SF (SansSerif; pronto para Inter em `res/font`), ícones de traço fino, toggles verdes, claro/escuro.

## Privacidade
- Tudo local por padrão (Room; hook para SQLCipher/SupportFactory com chave no Keystore documentado em `core/data`). Ao Gemini vai só o necessário para fichas; notas pessoais só com consentimento explícito; **nunca áudio bruto**. Chave de API fora do APK. Exportar/apagar dados (apagar implementado; exportar via SAF planejado).

## Telas
1. Onboarding (permissões, bateria MIUI, tile, modelos/TTS)
2. Início (matérias, foco, Descarregar, assistente)
3. Descarregamento por voz + editor com marca-texto
4. Matéria/árvore de notas e revisão espaçada
5. Progresso do aprendizado 14 dias e base de conhecimento
6. Assistente (voz/texto, fonte e estado online/offline)
7. Configurações (acessibilidade, bateria, privacidade, modelos, apagar)

## Estrutura do projeto (MVVM + Clean, modular)
- `:app` — Activity, navegação, DI app (`GeminiConfig`), serviços (`dataSync`/`microphone`), `TileService`, manifest.
- `:core:common` — utilitários (`Result`, config).
- `:core:domain` — modelos e interfaces de repositório.
- `:core:data` — Room/DAOs, repos, Gemini client + currículo, RAG/embeddings (MediaPipe), conectividade, assistente local (MediaPipe GenAI), TTS.
- `:core:ui` — tema Liquid Glass e componentes.
- `:feature:*` — onboarding, home, voice-dump, notes, focus, review, learning, assistant, settings.

## Build
Pré-requisitos: JDK 17, Android SDK (`platforms;android-35` ou 34, `build-tools`).

```bash
# Windows PowerShell (na raiz do projeto)
echo sdk.dir=$env:LOCALAPPDATA\Android\Sdk > local.properties
# opcional: GEMINI_API_KEY=... em local.properties ou gradle.properties
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease   # requer signingConfig (RELEASE_*) ou usa assinatura de debug se não configurado
```

APK gerado em `app/build/outputs/apk/`.

## Testes
```bash
.\gradlew.bat :core:data:testDebugUnitTest
```
Cobertura unitária incluída: **RAG/cosseno + ranking**, **parsing do JSON de ficha Gemini (schema)**, **validação de intervalo de marca-texto/highlight**, round-trip de vetores.

## Checklist de verificação (Redmi Note 11S)
- **Bateria/MIUI:** Autostart ativo, otimização de bateria desativada; aprendizado roda só em primeiro plano com notificação; assistente/mic só após toque.
- **RAM:** LLM local carregado sob demanda e liberado após inatividade; **não carregar com bateria < 15%**.
- **Latência:** respostas curtas (2–5 frases) no modo local; RAG limita contexto recuperado.
- **Modo avião (offline):** com modelos instalados e base preenchida, validar descarregar/transcrever (se pacote STT offline presente), marca-texto, notas, revisão e assistente local/RAG sem internet.

## Etapas de entrega (implementadas nesta base)
1. Estrutura MVVM/Clean multi-módulo + UI base Liquid Glass.
2. Notas, descarregamento por voz (STT offline-first), marca-texto 4 cores com filtro/resumo.
3. Modo foco (pomodoro flexível, estacionamento de pensamentos) e revisão espaçada (SM-2).
4. Integração Gemini (ficha JSON + currículo 14 dias) + base Room + RAG/embeddings (MediaPipe).
5. Motor local (MediaPipe GenAI) + assistente de voz/TTS offline + Quick Settings Tile + Foreground Services.
6. Testes unitários (RAG, parsing JSON, marca-texto) e checklist de bateria/RAM/latência/modo avião.

## Avisos de segurança
- Legislação: informativa, leis mudam — **não substitui advogado**. Saúde: **não substitui profissional**. Sem diagnóstico. Risco/sofrimento intenso: acolher e incentivar contato com alguém de confiança ou **CVV 188**.
- Conteúdo gerado por modelos pode conter erros; o app indica incerteza/fonte e usa base local factual por fichas.
