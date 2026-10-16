# Free deployment

O SpotGuess usa servicos com faixas gratuitas e nao exige faturamento no Google Cloud:

- Firebase Hosting publica o frontend Angular.
- Render executa o backend Java/Spring Boot.
- MongoDB Atlas M0 persiste salas, jogadores e musicas.
- YouTube Data API v3 fornece os metadados dos videos.

## Backend no Render

O servico usa o diretorio `backend`, runtime Docker e o `backend/Dockerfile`.
Configure estas variaveis sem coloca-las no Git:

```text
ROOM_STORE=mongodb
MONGODB_URI=mongodb+srv://...
MONGODB_REPOSITORIES_ENABLED=true
YOUTUBE_API_KEY=...
CORS_ALLOWED_ORIGINS=http://localhost:4200,https://spotguess-20260312.web.app
```

O plano gratuito pode suspender o backend sem trafego. A primeira requisicao
apos um periodo ocioso pode levar mais tempo.

## Frontend no Firebase

Na raiz do repositorio:

```powershell
firebase deploy --only hosting --project spotguess-20260312
```

O predeploy compila o Angular e publica `frontend/dist/frontend/browser`.
Nenhuma credencial deve ser colocada no frontend ou em arquivos versionados.
