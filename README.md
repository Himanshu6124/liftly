# Liftly

Offline-first strength training tracker for **Android, iOS and Desktop**, built with
Compose Multiplatform. You log **per exercise, not per set**: do all your sets, then open the
exercise and save them together in a sheet that is already filled in from last time.

> Status: **Milestone 1 (setup) done.** See [Roadmap](#roadmap).

## Requirements

| Tool | Version |
|---|---|
| JDK | 21 |
| Android SDK | compileSdk 37 |
| Android Studio | latest stable (supports AGP 9.2.1) + Kotlin Multiplatform plugin |
| Xcode | 26+ (Apple Silicon; no Intel simulator, see `DECISIONS.md`) |

## Run

```bash
# Android (device or emulator connected)
./gradlew :androidApp:installDebug

# Desktop
./gradlew :composeApp:run

# iOS: open in Xcode and Run, or from the command line:
open iosApp/iosApp.xcodeproj
```

To sign for a physical iPhone, set `TEAM_ID` in `iosApp/Configuration/Config.xcconfig`.

## Test and lint

```bash
./gradlew desktopTest testAndroidHostTest iosSimulatorArm64Test
./gradlew ktlintCheck detekt
```

CI (`.github/workflows/ci.yml`) runs lint, tests and Android + Desktop builds on Linux, and
iOS tests + the Xcode build on macOS.

## Architecture

Clean Architecture with MVI-style ViewModels:

```
Composable ──Intent──▶ ViewModel ──▶ UseCase ──▶ Repository (interface, core/domain)
    ▲                     │                          │
    └──── UiState ◀───────┘          Room / DataStore / platform APIs (core/data, ...)
          + one-off Effects (Channel) for navigation and snackbars
```

| Module | Contents |
|---|---|
| `androidApp` | Android `Application` + `MainActivity` (AGP 9 needs a separate app module) |
| `composeApp` | `App()`, navigation shell, Koin start, iOS `MainViewController`, Desktop `main` |
| `iosApp` | Xcode project hosting the Compose UI in SwiftUI |
| `build-logic` | Convention plugins: `liftly.kmp.library`, `liftly.kmp.compose`, `liftly.kmp.feature`, `liftly.room` |
| `core/domain` | Pure Kotlin models, repository interfaces, use cases |
| `core/data` | Room database, DAOs, sync engine, repository implementations |
| `core/datastore` | `SettingsRepository` on DataStore Preferences |
| `core/platform` | `Haptics`, `Notifier`, `UrlLauncher`, `ShareService` per platform |
| `core/designsystem` | Theme (light/dark/dynamic), spacing, shared components |
| `feature/*` | One module per feature: screen, ViewModel, navigation entry, strings |

Conventions: each screen has a stateful `XxxScreen` (gets the ViewModel from Koin) and a
stateless `XxxContent`. ViewModels expose `state: StateFlow`, `effects: Flow` and
`onIntent(...)`. Weight is stored in kg; timestamps as epoch millis.

## Offline-first and backend sync

All data lives in the local Room database, which is the only thing the UI reads. The schema
is ready for a backend from version 1:

- Client-generated **UUID ids**, so records created offline never collide on a server.
- Every table has `updatedAt`, `deletedAt` (tombstone), `syncStatus`, `serverVersion`.
  Every local write is marked `PENDING`.
- A generic **sync engine** (`core/data/sync/SyncEngine.kt`) pushes pending rows, pulls
  server changes by cursor, and resolves conflicts with last-write-wins. It is covered by
  tests against an in-memory fake server.

**To connect a backend:**

1. Implement `RemoteSyncDataSource` (two methods: `push(table, records)` and
   `pull(table, cursor, limit)`), for example with Ktor against a REST API, or Supabase.
2. In `core/data/.../di/DataModule.kt`, bind it and replace `LocalOnlySyncController` with
   `BackgroundSyncController`.

Nothing else changes: screens, use cases and repositories stay as they are. Details and
trade-offs are in [`DECISIONS.md`](DECISIONS.md).

## Roadmap

1. ✅ Setup: KMP targets, build-logic, Koin, navigation shell, theme, Room + DataStore, sync-ready schema, CI
2. Exercise library + form guide (seed data, search, Exercise Detail, muscle diagram)
3. Active workout (start/resume/finish, exercise picker, checklist cards)
4. Batch Log Sheet (pre-fill, quick reps parser, swipe quick-log, undo)
5. Log sheet complete (per-set mode, warm-ups, RPE, tracking types, PRs)
6. Routines (CRUD, next-routine rotation, 1-tap start)
7. History, Summary, Settings
8. MVP hardening (accessibility, adaptive layouts, E2E UI test)
9. Phase 2 and Phase 3 features
