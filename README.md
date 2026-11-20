# Liftly

Offline-first strength training tracker for **Android, iOS and Desktop**, built with
Compose Multiplatform. You log **per exercise, not per set**: do all your sets, then open the
exercise and save them together in a sheet that is already filled in from last time.

> Status: **Milestone 7 (history, summary, settings) done.** See [Roadmap](#roadmap).

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
| `core/designsystem` | Theme (light/dark/dynamic), spacing, shared components (thumbnail, demo, muscle diagram) |
| `core/resources` | Bundled exercise seed JSON + images (Compose resources) |
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

## Exercise data

The bundled library (129 exercises with images) is generated from
[free-exercise-db](https://github.com/yuhonas/free-exercise-db) (public domain) plus Liftly's own
aliases, form cues and common mistakes in `tools/seed/curation.py`. To change it:

```bash
python3 tools/seed/build_seed.py path/to/free-exercise-db/dist/exercises.json --images
```

Then bump `SEED_VERSION` in `RoomExerciseSeeder.kt` (and `version` in the JSON) so existing
installs pick up the change. Favorites, notes and custom exercises are never overwritten.

## Roadmap

1. ✅ Setup: KMP targets, build-logic, Koin, navigation shell, theme, Room + DataStore, sync-ready schema, CI
2. ✅ Exercise library + form guide (129 exercises, fuzzy search, filters, favorites, form tab, muscle diagram, notes)
3. ✅ Active workout (start/resume/finish/discard, picker with suggestions + custom exercises, reorder, auto-resume)
4. ✅ Batch Log Sheet (pre-fill, quick reps parser, weight nudge, swipe quick-log/skip with undo, auto-advance)
5. ✅ Log sheet complete (per-set mode, set types, warm-ups, RPE, notes, time/distance, PR detection + badge, Records tab)
6. ✅ Routines (create/edit/delete with undo, targets, reorder, next-routine rotation and 1-tap start on Home, save/update from a workout)
7. ✅ History (weekly, edit, delete with undo), workout summary, settings
8. MVP hardening (accessibility, adaptive layouts, E2E UI test)
9. Phase 2 and Phase 3 features
