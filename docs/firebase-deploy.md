# Google Cloud deployment

O SpotGuess usa um unico projeto Google Cloud/Firebase: `spotguess-20260312`.

- Firebase Hosting publica o frontend Angular.
- Cloud Run executa o backend Java/Spring Boot no servico `spotguess-api`.
- Firestore persiste salas, jogadores e musicas.
- Secret Manager entrega `YOUTUBE_API_KEY` ao Cloud Run.
- Firebase Hosting encaminha `/api/**` para o Cloud Run.

## Recursos necessarios

```powershell
gcloud config set project spotguess-20260312
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com firestore.googleapis.com secretmanager.googleapis.com
```

Crie o banco Firestore em modo Native na regiao escolhida para o projeto. A
identidade do servico Cloud Run precisa dos papeis de usuario do Firestore e
acesso ao segredo da API do YouTube.

## Publicar o backend

O `backend/Dockerfile` e usado automaticamente no deploy por codigo-fonte:

```powershell
gcloud run deploy spotguess-api --source backend --region southamerica-east1 --allow-unauthenticated --set-env-vars ROOM_STORE=firestore,GOOGLE_CLOUD_PROJECT=spotguess-20260312 --set-secrets YOUTUBE_API_KEY=spotguess-youtube-api-key:latest
```

## Publicar o frontend

Na raiz do repositorio:

```powershell
firebase deploy --only hosting --project spotguess-20260312
```

O predeploy compila o Angular e publica `frontend/dist/frontend/browser`. Nunca
coloque a chave do YouTube no Angular ou em arquivos versionados.
