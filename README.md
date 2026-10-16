# WhoAddedThat?

Jogo social para adivinhar quem escolheu cada musica. Cada participante entra
com um apelido e envia links de videos do YouTube; nenhum login Google ou
Spotify e necessario.

## Stack

- Frontend: Angular 17 + TypeScript, publicado no Firebase Hosting.
- Backend: Java 21 + Spring Boot, publicado no Render.
- Persistencia: MongoDB Atlas.
- Metadados de video: YouTube Data API v3.
- Segredos: Google Secret Manager.

## Fluxo do MVP

1. O host cria uma sala e recebe um codigo/link de convite.
2. Os jogadores entram com um apelido.
3. Cada jogador envia pelo menos um link publico do YouTube.
4. O backend registra quem enviou cada musica.
5. Na rodada, os participantes tentam adivinhar a autoria.

## Desenvolvimento local

Backend, usando armazenamento temporario em memoria:

```powershell
cd backend
$env:YOUTUBE_API_KEY="sua-chave-local"
mvn spring-boot:run
```

Frontend em outro terminal:

```powershell
cd frontend
npm start
```

Em producao, o Angular chama a API no Render e o backend usa
`ROOM_STORE=mongodb`.
