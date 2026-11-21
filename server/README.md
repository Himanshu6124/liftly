# Liftly server

Ktor + PostgreSQL backend for sync. The app stays offline-first; this only stores and serves
changes. Wire format: `core/protocol`. Design notes: `DECISIONS.md` (Milestone 20).

## Run locally

No Docker needed: an embedded PostgreSQL 18 starts with the server and keeps its data in
`server/build/dev-db`.

```bash
./gradlew :server:runDev
```

The server listens on `http://localhost:8080` with **dev auth** on: besides real tokens, it accepts
`X-Liftly-Dev-User: <any id>` with no sign-in. Apple sign-in accepts the iOS bundle id; set
`GOOGLE_AUDIENCES` before `runDev` to enable Google. The iOS simulator reaches it at `localhost`,
the Android emulator at `10.0.2.2`.

With Docker:

```bash
./gradlew :server:installDist
docker compose -f server/docker-compose.yml up --build
```

## API

| Endpoint | Auth | Body / query | Returns |
|---|---|---|---|
| `GET /health` | | | `ok` (checks the database) |
| `POST /auth/apple` | | `SignInRequest(idToken, nonce)` | `AuthTokens` |
| `POST /auth/google` | | `SignInRequest(idToken)` | `AuthTokens` |
| `POST /auth/refresh` | | `RefreshRequest` | `AuthTokens` (new refresh token) |
| `POST /auth/logout` | | `RefreshRequest` | 204 |
| `GET /account` | Bearer | | `Account` |
| `DELETE /account` | Bearer | | 204, deletes the user and all their data |
| `POST /sync/{table}/push` | Bearer | `List<SyncRecord>`, at most 500 | `List<PushAck>` |
| `GET /sync/{table}/pull` | Bearer | `cursor` (last version seen), `limit` (1–500) | `PullPage` |

`{table}` is one of `SyncProtocol.TABLES`. Errors are `ApiError` JSON (`{"code","message"}`); a
401 with `session_expired` means sign in again. `/auth/*` is limited to 20 requests a minute per IP.

**Tokens:** access tokens are 15-minute JWTs (`Authorization: Bearer ...`). Refresh tokens last
30 days and are single-use: each refresh returns a new one. Re-using an old one within 2 minutes
(a retry after a lost response) still works; later, it signs out every session of that account.

```bash
curl -s localhost:8080/sync/routines/pull -H 'X-Liftly-Dev-User: me'
```

## Configuration (environment)

| Variable | Default | |
|---|---|---|
| `PORT` | `8080` | |
| `DATABASE_URL` | required | e.g. `jdbc:postgresql://db:5432/liftly` |
| `DATABASE_USER`, `DATABASE_PASSWORD` | required | |
| `JWT_SECRET` | required | 32+ random characters, e.g. `openssl rand -base64 48` |
| `APPLE_AUDIENCES` | `com.himanshu.liftly` | Comma-separated; empty disables Apple sign-in |
| `GOOGLE_AUDIENCES` | empty (off) | Comma-separated Google OAuth client ids |
| `LIFTLY_DEV_AUTH` | off | `true` trusts `X-Liftly-Dev-User`. **Never in production.** |

The server expects to sit behind a reverse proxy (Caddy) and trusts `X-Forwarded-For` for rate
limiting, so its port must never be reachable directly.

The schema migrates itself on start (Flyway, `src/main/resources/db/migration`).

## Tests

```bash
./gradlew :server:check
```

Runs against a real embedded PostgreSQL, including the app's own `SyncEngine` syncing two
in-memory devices through the server. `checkNoAppModules` fails the build if the server's runtime
ever depends on an app module (only `core/protocol` is allowed).
