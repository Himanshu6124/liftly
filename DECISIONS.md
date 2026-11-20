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

## Milestone 4: Batch Log Sheet

- **Pre-fill** (`GetLogSheetPrefillUseCase`, pure): this workout's saved sets → last session of
  the exercise (other workouts, LOGGED only) → routine target → `defaultSetCount` empty sets.
  The input mode is remembered per exercise via the last session's `WorkoutExercise.inputMode`.
- **UNIFORM mode shows the heaviest working weight** of the pre-fill when the last session had
  varying weights; warm-up sets from the pre-fill are kept untouched and shown as "+ n warm-up
  sets kept". Full per-set editing (PER_SET mode) arrives in Milestone 5.
- **Reps field keyboard:** the phone keypad (`KeyboardType.Phone`) is the only numeric keyboard
  available on both Android and iOS that has a "multiply" key (`*`). The parser accepts `x × *`
  for "reps × sets" and space, comma, `+`, `#`, `.` as set separators, so `8*4` and `8+8+7` work
  on every phone keypad, and `8 8 7 6` / `8x4` work on full keyboards.
- **Text fields in sheets use `TextFieldState`** (the field owns its text). With the classic
  `value/onValueChange` pattern, fast typing inside a `ModalBottomSheet` on iOS dropped all but
  the last keystroke, because the sheet is a separate window that only recomposes after input
  settles. Pre-fill, nudges and set-count changes write into the `TextFieldState`.
- **Weight nudge** rounds to the increment (2.5 kg / 5 lb from settings): 81 − 2.5 → 77.5.
- **Save = one transaction** (`logSets`): soft-deletes old sets, inserts new ones, sets
  LOGGED + `loggedAt` + input mode, and returns a snapshot so the change can be undone exactly.
- **Swipe right (quick-log)** logs exactly the pre-fill. If there's nothing to pre-fill from
  (first time ever), it opens the sheet instead of logging empty sets. Swipe left skips. Both
  keep the card in place (status changes) and show an Undo snackbar. Screen-reader users get
  "Log same as last time" / "Skip" / "Remove" as accessibility actions on the card.
- **Auto-advance:** after a save or quick-log, the next pending card (wrapping around to earlier
  ones) is scrolled into view and highlighted for 1.5 s; with "Open next exercise automatically"
  enabled, its sheet opens instead. A light haptic confirms each log.
- **Validation:** reps 1–100, weight 0–1000 kg, at most 20 sets; Save stays disabled while
  invalid. Empty sets are dropped on save.

## Milestone 5: complete log sheet and personal records

- **PR rules** (`DetectPersonalRecordsUseCase`, pure): est. 1RM (Epley, reps ≤ 12), heaviest weight,
  most reps in a set, best session volume. Working sets only (warm-ups excluded on both sides);
  a tie is not a PR; the very first session of an exercise sets no PRs (nothing to beat).
  Detection runs against every *other* session, so editing a session down removes its PRs.
- **PR rows are written in the same transaction as the sets** and deleted with them (edit,
  remove exercise, discard workout, undo restores them). They drive the 🏆 badge on cards and
  a "success" haptic instead of the light one.
- **The Records tab is computed from logged sets**, not from PR rows, so it shows correct bests
  even after the first session and after edits/deletions. PR rows remain the "when was it
  broken" log for history/charts later.
- **Per-set mode** ("Vary weight per set") converts both ways: rows ← one weight + reps list,
  and back to the heaviest weight + reps list. New rows copy the previous row. Working set types
  cycle Normal → Drop → Failure by tapping the set number.
- **Warm-ups are separate rows** above working sets in both modes (Add warm-up set suggests
  40/60/80 % of the working weight × 10/5/3, rounded to the increment). They're saved as
  `WARMUP` and excluded from summaries, volume and PRs.
- **RPE:** one value for all sets or one per set ("8 8.5 9") in uniform mode; an RPE column per
  row in per-set mode. Shown under "More" with warm-ups and the exercise note.
- **TIME exercises** take one duration per set ("1:00 0:45", "60x3", "1.30" for keypads without
  ':'). **DISTANCE exercises** take metres + optional time as one entry. Metres avoid a km/mi
  setting for now; summaries read "5000 m · 25:00".

## Milestone 6: routines

- **Next routine rotation** (`NextRoutine.pick`): never-done routines first (in the user's order),
  then the one with the oldest `lastPerformedAt`. Empty routines are never offered. Finishing a
  workout started from a routine stamps the routine's `lastPerformedAt` with the workout start.
- **Home's primary card** is "Resume workout" while one runs, otherwise "Next up: Start <routine>"
  (1 tap). "Start empty workout" is the secondary button. On Home, tapping a routine *starts* it;
  on the Routines tab, tapping a routine *edits* it. Both have a ▶ button that starts it, and ⋮ menus.
- **Routine targets are optional.** Pre-fill uses the last session first; targets only matter for
  exercises never done before. "Update routine targets" (workout ⋮ menu, routine workouts only)
  copies today's working-set count, most common reps and top weight; unlogged exercises keep their
  old targets.
- **Save as routine** is in the workout ⋮ menu now (name pre-filled from the workout); the summary
  screen (Milestone 7) will offer it too. Unlogged exercises get 3 sets and no target weight.
- **Editing a routine keeps line ids** for unchanged exercises (so sync sees updates, not
  delete + insert). Deleting a routine has Undo; workouts that referenced it keep their history.
- **Reordering routines:** long-press + drag on the Routines tab; the editor uses ▲/▼ buttons
  (simpler inside a scrolling form with text fields).

## Milestone 7: history, summary, settings

- **Finishing a workout opens the Summary** (duration, volume, working sets, logged/skipped, new
  PRs, muscle map, Save as routine / Update routine). "Done" returns Home; the back stack is
  Home → Summary so Back also lands on Home. Discarding still goes straight Home.
- **History stats are computed in SQL** (one query with sub-selects) so the list stays fast and
  reacts to edits of past workouts. Volume and set counts exclude warm-ups everywhere.
- **Weeks start on Monday** (ISO); headers read "This week", "Last week", then "Week of <date>".
- **Editing a past workout** reuses the Batch Log Sheet unchanged (tap an exercise in the detail).
  PR detection runs against all other sessions, including later ones.
- **Deleting a workout** (detail ⋮ menu) closes the detail and the History list shows the Undo
  snackbar (a small shared `HistoryUndo` holder bridges the two screens). Undo revives every
  row, including PRs, keeping server versions for sync.
- **Settings are device-local** (DataStore): unit, increment (choices follow the unit: 1/1.25/2.5/5 kg
  or 2.5/5/10 lb, each unit remembers its own), default sets (1–10), theme, wallpaper colors (only
  shown where supported: Android 12+), open-next-automatically. Changing the unit converts
  every displayed weight; storage stays in kg.

## Milestone 8: MVP hardening

- **E2E test on Desktop (JVM)** with the Compose UI test API: it runs the real `App()` with
  real ViewModels and Room (in memory) on every CI run, without an emulator. Clicks use the
  semantics `OnClick` action so buttons below the small test window still work; the swipe
  quick-log is exercised through its screen-reader custom action.
- Writing it found three real bugs, now fixed and covered:
  - A `fillMaxHeight` divider inside the filter `LazyRow` stretched the row over the whole
    sheet on wide screens (the exercise list disappeared). It now has a fixed height.
  - The Batch Log Sheet started loading from `init` inside `remember` (a side effect during
    composition); the loaded values could miss recomposition. Loading now starts from a
    `LaunchedEffect`.
  - Finishing a workout raced two navigations: the workout disappearing (→ Home) and the
    Closed effect (→ Summary). While finishing or discarding, the ViewModel now ignores the
    workout vanishing, so only the effect navigates.
- **Compose stability config** (`config/compose-stability.conf`) marks domain models and
  date/time types stable: they're immutable but live in a module without the Compose
  compiler, so screens taking them could not skip recomposition.
- **List-detail only for the exercise library.** It's the one screen people browse; history
  and routines keep full-screen detail (their detail screens have their own actions and undo).

## Milestone 9 (Phase 2): exercise progress charts

- **Charts are a small Canvas composable** (`core/designsystem/.../LineChart.kt`), not a chart
  library: one line, dots, three labelled grid lines, tap-to-select. It is reused later for body
  measurements. Screen readers get a one-sentence summary (first and latest value) instead of
  the drawing.
- **Metrics per session, working sets only:** est. 1RM (Epley, reps ≤ 12), best weight, volume,
  most reps, total reps, longest set, distance. Only metrics that have data are offered, so a
  plank shows "Longest set" and a pull-up "Most reps".
- **Ranges 1M / 3M / 1Y / All**, counted back from today; default All.
- Values stay in kg in state and are converted when formatted, like everywhere else.
- The History tab lists every session (date, workout name, sets) under the chart. Sessions of
  the workout in progress are included once that exercise is logged.

## Milestone 10 (Phase 2): consistency on Home

- **Weekly streak, not daily.** Lifters rest between sessions, so a day streak would break on
  every rest day. The streak counts consecutive Monday-weeks that reached the weekly goal,
  ending last week, plus this week as soon as its goal is met (so it never drops to 0
  mid-week just because the week isn't over).
- **Weekly goal** (workouts per week, 1–7, default 3) lives in Settings and drives the ring.
- **Heatmap** shows the last 16 weeks (Monday-first columns, ending today), shaded by working
  sets that day: rest / under 10 / under 20 / 20+. Drawn on one Canvas (112 cells) instead of
  112 composables. Screen readers get "Trained on N of the last M days".
- The card only appears once there's a finished workout. Home now waits for its first data
  before showing empty-state hints, so "Create a routine" no longer flashes on launch.

## Milestone 11 (Phase 2): weekly sets per muscle

- Shown at the top of History (that's where you look back), browsable week by week.
- **Counting:** each logged working set counts 1 for the exercise's primary muscle and ½ for each
  secondary muscle (warm-ups excluded). This is the common way to estimate weekly volume per
  muscle; bench press then credits chest 1 and triceps/front delts ½ each.
- One SQL query groups working sets per exercise for the week; the per-muscle split is a pure
  domain function (`MuscleSets`), so it's unit-tested without a database.
- The body diagram reuses `MuscleDiagram` as a heatmap (intensity relative to the week's top
  muscle, with a floor so light work stays visible). `MuscleDiagram` now always fits inside the
  box it's given instead of trusting its aspect ratio, which overdrew when a caller fixed the
  height and the width separately.

## Milestone 12 (Phase 2): rest timer

- **Manual, one floating button** next to "Add exercise": tap → presets 1:00 / 1:30 / 2:00 / 3:00;
  while running it shows the countdown, and tap → +30 s / −30 s / Stop.
- **Stored as `workouts.restEndsAt`**, an absolute time, never a counter: it survives the app being
  backgrounded or killed, and the countdown is always `restEndsAt − now`.
- **Remembered per exercise** (`exercises.restSeconds`, synced like the personal note). The rest
  belongs to the next exercise to do (first not-logged card), since with batch logging you rest
  between sets of the exercise you haven't logged yet. Its last time is marked in the menu.
- **End of rest:** a local notification scheduled for `restEndsAt` (works with the app closed)
  plus a success haptic when the screen is open. iOS doesn't show notifications for the app in
  the foreground, so the haptic covers that case. Permission is requested the first time a rest
  starts, never at launch.
- **Android alarms:** exact when the system allows (`canScheduleExactAlarms`), otherwise inexact,
  which can be late by a few minutes in Doze. We don't ask for `USE_EXACT_ALARM` (Play reserves it
  for alarm/calendar apps). A foreground-service countdown is Phase 3 (ongoing notification).
- **Schema v2** via Room auto-migration (two nullable columns). Verified on a v1 database with
  data: version 2, all rows kept.
