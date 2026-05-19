# Guess The Playlist

Monorepo inicial para o jogo de deducao social com playlist colaborativa.

## Stack inicial
- Frontend: Angular + TypeScript
- Backend: Java 21 + Spring Boot
- Realtime/eventos: Kafka (pronto para evolucao), WebSocket (fase seguinte)
- Banco: PostgreSQL
- Infra local: Docker Compose

## Estrutura
- frontend/: app Angular (MVP UI)
- backend/: API Spring Boot (MVP playlist -> tracks -> added_by)
- infra/: docker-compose para Postgres e Kafka
- docs/: arquitetura e roadmap

## MVP tecnico (fase 1)
1. Receber link de playlist publica
2. Extrair playlist_id
3. Buscar dados no Spotify (app token)
4. Exibir tabela musica -> added_by

## Proximos passos
1. Subir infra com docker compose
2. Configurar variaveis SPOTIFY_CLIENT_ID e SPOTIFY_CLIENT_SECRET
3. Rodar backend
4. Gerar frontend Angular e conectar no endpoint de importacao
