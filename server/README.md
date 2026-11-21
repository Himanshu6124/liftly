# Liftly server

Ktor + PostgreSQL backend for sync. The app stays offline-first; this only stores and serves
changes. Wire format: `core/protocol`. Design notes: `DECISIONS.md` (Milestone 20).

## Run locally

No Docker needed: an embedded PostgreSQL 18 starts with the server and keeps its data in
`server/build/dev-db`.

```bash
./gradlew :server:runDev
```

The server listens on `http://localhost:8080` with **dev auth**: send `X-Liftly-Dev-User: <any id>`
instead of a token (real sign-in is Milestone 21). The iOS simulator reaches it at `localhost`,
the Android emulator at `10.0.2.2`.

With Docker:

```bash
./gradlew :server:installDist
docker compose -f server/docker-compose.yml up --build
```

## API

| Endpoint | Body / query | Returns |
|---|---|---|
| `GET /health` | | `ok` (checks the database) |
| `POST /sync/{table}/push` | `List<SyncRecord>`, at most 500 | `List<PushAck>` |
| `GET /sync/{table}/pull` | `cursor` (last version seen), `limit` (1–500) | `PullPage` |

`{table}` is one of `SyncProtocol.TABLES`. Errors are `ApiError` JSON (`{"code","message"}`).

```bash
curl -s localhost:8080/sync/routines/pull -H 'X-Liftly-Dev-User: me'
```

## Configuration (environment)

| Variable | Default | |
|---|---|---|
| `PORT` | `8080` | |
| `DATABASE_URL` | required | e.g. `jdbc:postgresql://db:5432/liftly` |
| `DATABASE_USER`, `DATABASE_PASSWORD` | required | |
| `LIFTLY_DEV_AUTH` | off | `true` trusts `X-Liftly-Dev-User`. **Never in production.** |

The schema migrates itself on start (Flyway, `src/main/resources/db/migration`).

## Tests

```bash
./gradlew :server:check
```

Runs against a real embedded PostgreSQL, including the app's own `SyncEngine` syncing two
in-memory devices through the server. `checkNoAppModules` fails the build if the server's runtime
ever depends on an app module (only `core/protocol` is allowed).
