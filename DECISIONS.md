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

## Milestone 2: exercise library and form guide

- **Seed data:** 129 exercises built from free-exercise-db (public domain) by
  `tools/seed/build_seed.py`. Aliases, form cues, common mistakes and the finer muscle mapping
  (front/side/rear delts, upper/lower back) are written for Liftly in `tools/seed/curation.py`.
  Attribution: `core/resources/LICENSE-exercise-data.md` (also shown in Settings → About, M7).
- **All 129 exercises have bundled images** (2 per exercise, 360 px, ~6 MB), so the library works
  fully offline and Coil's network module isn't needed yet. `imagePaths` may also hold URLs; add
  `coil-network-ktor3` when the first URL-based image appears.
- **Resources live in a `core/resources` module** (seed JSON + images). `core/data` doesn't read
  them directly: it declares a `SeedSource` interface the app binds, so data stays free of
  Compose and the seeder is testable with plain strings.
- **Seeded rows** get ids `seed:<seedKey>`, start `SYNCED` with `updatedAt = 0`, so they are never
  pushed to a backend until the user changes them (favorite, note), and any real edit wins
  conflicts. Upgrades (seed version bump) replace library content only, matched by `seedKey`.
  Known gap: if another device's copy of a seeded row is pulled, its library text may be older
  than this device's seed; harmless now, and fixable by syncing only user fields for seed rows.
- **Filter chips use 6 muscle groups** (Chest, Back, Shoulders, Arms, Legs, Core) instead of 20
  muscles, plus equipment chips: fewer taps, and a group covers what people mean by "back day".
- **Favorite toggle is on every library row** (heart icon) so favoriting doesn't require opening
  the detail screen.
- **Form tab order:** demo → key cues → common mistakes → muscles → step-by-step → note.
  Cues come first because that's what people check between sets; the spec lists the same items
  in a different order.
- **Personal note autosaves** 0.5 s after typing stops and is flushed when leaving the screen,
  using an app-wide coroutine scope so the write isn't cancelled with the screen.
- **History / Records tabs** show empty states until workouts exist (Milestones 3–5). Creating
  custom exercises arrives with the exercise picker in Milestone 3 ("Create '<query>'").

## Milestone 3: active workout and exercise picker

- **Start = resume.** `StartWorkoutUseCase` enforces one in-progress workout by returning the
  existing one instead of failing, so "Start" never dead-ends and Home's big button becomes
  "Resume workout" while one is running.
- **Unnamed workouts** are stored with an empty name and labelled at display time from the start
  hour ("Morning/Afternoon/Evening/Night workout"), so the label follows the UI language.
- **Finishing a workout with zero exercises discards it** instead of saving an empty entry to
  history. Finishing with exercises still pending marks them SKIPPED (after one confirm sheet).
- **Auto-resume:** on launch, if a workout is in progress the app opens straight into it (once per
  launch, not again after rotation). Everything is read from Room, so it survives process death;
  the elapsed timer is computed from the stored `startedAt`.
- **Reordering** uses `sh.calvin.reorderable` (3.1.0, KMP, Apache-2.0): long-press + drag on a
  card. Hand-rolling drag-and-drop in a `LazyColumn` (auto-scroll, item offsets) is a lot of
  fiddly code for no benefit. The card menu also has Move up/down for accessibility.
- **Undo instead of confirm:** Skip and Remove show an Undo snackbar. Remove keeps a snapshot
  (exercise + sets) and restores it; if the server already knew the row, its tombstone is revived
  (keeps `serverVersion`) so the restore syncs as an update, not a duplicate.
- **Exercise picker and form preview live in `feature/exercises`** and are reused by the workout
  (and later routines), so `feature/workout` depends on `feature/exercises`. Simpler than a
  separate shared-UI module for two sheets.
- **"Create '<query>'"** is offered whenever there's a search query (not only on zero results),
  pre-filled from the query and active filters; the new exercise is selected automatically.
- **Until Milestone 4**, tapping a card opens the form preview; M4 changes tap to the Batch Log
  Sheet. The workout summary screen arrives in M7; finishing returns Home for now.
