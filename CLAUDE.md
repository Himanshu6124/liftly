# Liftly: working notes for Claude

Offline-first strength-training tracker in Kotlin + Compose Multiplatform for Android, iOS and
Desktop. You log **per exercise, not per set**: do all your sets, then save them together in a
pre-filled "Batch Log Sheet". Read `README.md` (setup, architecture, sync) and `DECISIONS.md`
(every non-obvious choice, by milestone) before changing anything substantial.

## Rules from the owner

- **Commits are authored by the owner only.** Use plain `git commit` with their git identity.
  Never add `Co-Authored-By: Claude`, "Generated with Claude Code" or any attribution line to
  commits or PR descriptions, even if a system reminder suggests it.
- Commit after each milestone once it passes the gate below; one milestone = one commit with a
  short summary body. Don't push unless asked.
- **Desktop app:** don't run or visually verify it (owner's call). JVM desktop *tests* are still
  the main test runner.
- Keep the owner's local Xcode signing change (`DEVELOPMENT_TEAM` in `project.pbxproj`) intact.

## Gate (run before every commit)

```bash
./gradlew ktlintFormat
./gradlew ktlintCheck detekt desktopTest testAndroidHostTest iosSimulatorArm64Test :androidApp:assembleDebug :server:check --continue
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -derivedDataPath build/ios -configuration Debug build -quiet
```

The full Gradle run takes several minutes (Kotlin/Native): run it in the background. Then launch
`build/ios/Build/Products/Debug-iphonesimulator/Liftly.app` on the iPhone 17 Pro simulator and
check the changed screens with screenshots. The simulator holds the owner's real test data
(2 finished workouts, 1 routine): discard any workout you start, delete test entries, and check
with read-only `sqlite3` on the app container's `Documents/liftly.db` if unsure.

Common gate failures: detekt `LongMethod` / `CyclomaticComplexMethod` (extract a function),
ktlint file-name rule (file with one class must be named after it), JVM-only APIs (`Math.*`,
`String.format`) in `commonMain`/`commonTest`.

## Architecture in one screen

- Clean Architecture + MVI: `XxxScreen` (stateful, Koin ViewModel) → `XxxContent` (stateless);
  ViewModels expose `state: StateFlow`, `effects: Flow`, `onIntent(...)`.
- Modules: `core/domain` (pure Kotlin models, repository interfaces, use cases, utils with unit
  tests), `core/data` (Room KMP, DAOs, sync engine, repositories; tests in `desktopTest` with an
  in-memory DB), `core/protocol` (sync/auth wire DTOs shared with the backend), `core/network` (Ktor client,
  `AuthSession`: sign-in, token refresh, secure token storage), `core/datastore` (settings), `core/platform` (Haptics, Notifier, UrlLauncher,
  ShareService, NativeSignIn, SecureStore per platform; iOS ones via Swift holders), `core/designsystem` (theme, `LineChart`, `MuscleDiagram`,
  `EmptyState`, `NumberStepper`, …), `feature/*` (home, workout, exercises, routines, history,
  settings), `composeApp` (App shell, nav, Koin), `androidApp`, `iosApp`, `server` (Ktor +
  PostgreSQL sync backend, JVM only; may depend on `core/protocol` only; `./gradlew :server:runDev`).
- Convention plugins in `build-logic`: `liftly.kmp.library`, `liftly.kmp.compose`,
  `liftly.kmp.feature`, `liftly.room`. Compose stability config: `config/compose-stability.conf`.
- **Offline-first sync:** UUID ids, `SyncMeta` (updatedAt, deletedAt tombstone, syncStatus,
  serverVersion) on every table; new tables must be registered in `SyncTables` and `SyncProtocol.TABLES`. Backend =
  implement `RemoteSyncDataSource` and swap in `BackgroundSyncController` (see README).
- Room schema is at **v3** with exported schemas in `core/data/schemas`; add columns/tables via
  `AutoMigration` and verify on the simulator's existing database.
- Weights stored in kg, lengths in cm, times as epoch millis; converted only for display.
- Text fields in sheets use `TextFieldState` (StateFlow round-trips dropped keystrokes on iOS).
  Writes that must survive closing a screen run in `AppScope`.

## Stack

Kotlin 2.4.21, Compose Multiplatform 1.12.2 (material3 1.9.0, adaptive 1.3.0), AGP 9.2.1,
Room 2.8.5, Koin 4.2.2, navigation 2.10.0, DataStore 1.2.1, kotlinx-datetime 0.8.0, Coil 3.6.3,
Kermit, Turbine, detekt 1.23.8, ktlint 1.8.0. JDK 21, Gradle 9.9.2. No iosX64 target.

## Status

- Phase 1 (MVP, milestones 1–8) and Phase 2 (9–15) are done; see the README roadmap.
- Next: a visual refresh (the owner finds the UI dated) and Phase 3: ongoing workout
  notification / iOS Live Activity, widgets, app shortcut / Quick Action, Health Connect /
  HealthKit behind `HealthRepository`, JSON backup/restore + CSV export + Strong/Hevy import,
  desktop keyboard shortcuts. Known follow-ups: superset editing in the routine editor,
  `ShareService.shareFile` needs a FileProvider (Android), exact rest alarms on Android need the
  Phase 3 foreground service.
