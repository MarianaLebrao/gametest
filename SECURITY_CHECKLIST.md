# Security checklist before push

1. Never commit real credentials in files.
2. Keep secrets only in environment variables or secret manager.
3. Validate with a local grep scan before every push.

Quick scan command:
rg -n --hidden --glob "!**/.git/**" --glob "!**/target/**" --glob "!**/node_modules/**" "(SPOTIFY_CLIENT_SECRET|client_secret|access_token|refresh_token|AKIA|BEGIN PRIVATE KEY|password\\s*=|api[_-]?key|secret)" .
