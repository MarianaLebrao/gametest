# Arquitetura inicial

Este arquivo resume a arquitetura enviada no briefing.

## Dominios
- Lobby
- Playlist Import
- Player Identity
- Round
- Vote
- Scoring

## Bounded contexts (futuro microservicos)
- playlist-service (Spotify integration + ETL)
- lobby-service (estado da sala)
- game-service (rounds e votos)
- scoring-service (pontuacao)
- realtime-gateway (WebSocket)

No inicio, todos estes contextos ficam em um unico backend Spring Boot modular.
