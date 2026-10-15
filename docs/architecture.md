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
- room-service (salas, jogadores e autoria das musicas)
- youtube-client (metadados publicos de videos)
- lobby-service (estado da sala)
- game-service (rounds e votos)
- scoring-service (pontuacao)
- realtime-gateway (WebSocket)

No inicio, todos estes contextos ficam em um unico backend Spring Boot modular.
