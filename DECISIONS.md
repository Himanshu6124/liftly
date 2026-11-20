# Decisions

Choices made where the spec was ambiguous, where reality (library versions, platform limits)
forced a change, or where the offline-first + future-backend requirement shaped the design.
Newest at the bottom of each section.

## Offline-first, backend-ready data layer

**Requirement:** store everything locally for now, but make it possible to save all data to a
backend later with minimal change.

1. **Room is the single source of truth.** UI observes Room `Flow`s only. A backend never talks
   to the UI; it only reconciles with the local DB. The app is fully functional with no network.
2. **UUID string primary keys** instead of the spec's `Long autoGenerate`. Rows created offline on
   several devices can be merged on a server without id collisions or remapping. Seeded
   exercises get deterministic ids (`seed:<seedKey>`) so the same library row has the same id
   on every device.
3. **Sync bookkeeping on every user-data table** (`@Embedded SyncMeta`):
   `updatedAt`, `deletedAt` (tombstone), `syncStatus` (`SYNCED`/`PENDING`), `serverVersion`.
   Every local write sets `PENDING` + `updatedAt`. Added from schema v1 so there's no migration later.
4. **Deletes:** rows a server has never seen (`serverVersion IS NULL`) are hard-deleted, so
   local-only use never accumulates tombstones. Rows the server knows become tombstones, get
   pushed, then are purged after the server acks. All read queries filter `deletedAt IS NULL`.
5. **Generic sync engine** (`core/data/sync`): push pending rows table by table (parents first,
   FK-safe), then pull pages after a per-table server cursor, then purge acked tombstones.
   Conflict policy is **last-write-wins on `updatedAt`**; a newer unpushed local edit is kept.
   A push ack only marks a row synced if it wasn't edited again while the request was in flight.
6. **Adding a backend = implement one interface** (`RemoteSyncDataSource`: `push` + `pull`) and
   change one Koin binding from `LocalOnlySyncController` to `BackgroundSyncController`.
   Payloads are the entity's columns as JSON, so the server can be generic or mapped.
   No HTTP client is added yet (Ktor would be the natural choice) since nothing uses it.
7. **Settings (DataStore) are device-local and not synced** (unit, theme are per-device choices).
   `PersonalRecord` rows are synced but are derived data; after pulls from another device the
   app should recompute PRs for the affected exercises (to do with PR detection, Milestone 5).
8. **Not done yet, intentionally:** auth/user scoping (server-side concern; add an `ownerId`
   only if multi-account on one device is ever needed), background scheduling (WorkManager /
   BGTaskScheduler) — `BackgroundSyncController` runs while the app is open.

## Platform and tooling

- **AGP 9 project shape:** AGP 9 no longer allows the KMP plugin together with
  `com.android.application`. So shared modules use `com.android.kotlin.multiplatform.library`,
  and the Android entry point lives in a small `androidApp` module (Application + MainActivity)
  instead of `composeApp/androidMain`.
- **No `iosX64` target.** `androidx.sqlite:sqlite-bundled` 2.7 (needed by Room KMP) no longer
  publishes Intel iOS simulator binaries. Apple Silicon simulators (`iosSimulatorArm64`) and
  devices (`iosArm64`) are supported.
- **`kotlin.time.Instant`** instead of `kotlinx.datetime.Instant`: kotlinx-datetime 0.7+ moved
  `Instant`/`Clock` into the Kotlin stdlib. kotlinx-datetime is still used for calendar math.
- **Database builder lives in `core/data`** (per-platform `platformDatabaseModule`) rather than
  `core/platform`, because it needs the `AppDatabase` type; `core/platform` stays free of Room.
- **Notifier** is a logging no-op on all platforms until the rest timer (Phase 2) needs it.
- **detekt 1.23.8** (latest stable) runs without type resolution; detekt 2.x is still alpha.
- **Material icons** come from `material-icons-core` 1.7.3, the last version JetBrains publishes.
- **Navigation:** `NavigationSuiteScaffold` gives bottom bar / rail / drawer from the window
  size class automatically. Five top-level tabs: Home, Routines, Exercises, History, Settings.
- **Tests that need a real database run in `desktopTest`** (JVM, bundled SQLite, in-memory
  Room). Pure logic tests live in `commonTest` and run on every target.
- **AGP pinned to 9.2.1** (not the newest 9.5.x) because current stable Android Studio only
  opens projects up to AGP 9.2.1. Bump it once Studio supports newer versions.
