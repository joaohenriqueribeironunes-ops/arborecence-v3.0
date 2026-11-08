Pacotes integrados no APK (assets/models). Coloque aqui os arquivos antes do build:
- llm.task            : LLM local quantizado (ex.: Gemma 3 1B / Qwen 1.5B, MediaPipe LLM Inference)
- embedding.tflite    : modelo de embeddings (MediaPipe Text Embedder)
- stt/ (opcional)     : modelo/arquivos STT pt-BR (whisper.cpp), se integrado
- tts/ (informativo)  : TTS pt-BR é provido pelo sistema; guie instalação do pacote de voz

O app extrai assets/models/* para files/models/ no primeiro uso e usa fallback para files/models/ (download).
Verifique SHA-256 dos modelos antes de distribuir. Modelos possuem licenças próprias.
