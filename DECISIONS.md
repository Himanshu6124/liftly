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

## Milestone 13 (Phase 2): plate calculator and warm-ups

- **Settings → Barbell:** bar weight and the plates you own, kept separately for kg and lb (like
  the weight increment). Plates are per side and assumed to come in pairs.
- **Plate calculator** shows under the weight field for barbell exercises in the log sheet
  ("Per side: 25 + 15 + 1.25"), live as you type. It uses an exact search (dynamic programming on
  the plates' common step), not greedy: with 15 kg and 10 kg plates, 20 kg per side is 10 + 10,
  which greedy misses. If the exact weight can't be made it shows the closest load below.
- **Warm-ups:** "Warm-ups 40 · 60 · 80 %" fills 3 sets (10 / 5 / 3 reps) in one tap, replacing
  any existing warm-ups; "Add warm-up set" still adds them one at a time. On a barbell the
  weights snap to loads your plates can make (nearest, ties lower, never below the bar);
  otherwise to the weight increment. Sets that collapse to the same weight are merged.
- Only exercises with equipment = Barbell get the plate maths; EZ bar, Smith machine and trap bar
  have different bar weights and are left for later.

## Milestone 14 (Phase 2): supersets

- **One rule, in one place** (`core/domain/.../Supersets.kt`): a superset is a run of 2+ *adjacent*
  exercises with the same group number. Everything else is "no superset", and groups are
  renumbered 1, 2, … in order (shown as A, B, …).
- **Each exercise is still logged on its own** (spec); supersets only group the cards: a
  "Superset A" label and a coloured rail running down the grouped cards.
- **Editing:** card menu → "Superset with next" (creates, extends or merges groups) and
  "Remove from superset" (taking out a middle exercise splits the group in two).
- **When the rule is applied:** on read, on reorder and on every superset edit, but *not* when an
  exercise is removed. Removing one exercise of a pair leaves its partner's stored group as is
  (it simply shows as ungrouped), so Undo brings the superset back intact.
- **Routines:** starting a routine keeps its supersets; "Save as routine" keeps the workout's.
  The routine editor doesn't edit supersets yet: it keeps them, and drops any that a reorder
  splits apart. No schema change: the `supersetGroup` columns existed since v1.

## Milestone 15 (Phase 2): body measurements

- **Where:** History has two tabs, Workouts | Body, instead of a 6th navigation item (the bottom
  bar stays at five). Body has a type picker (types you use come first), the latest value with
  the change since the first entry, the shared `LineChart`, and the entries with delete + Undo.
- **Types:** body weight, body fat %, waist, chest, hips, arms, thighs, neck.
- **Units:** stored as kg / cm / %; shown in kg + cm or lb + in following the weight-unit setting
  (one metric/imperial switch). Entries outside a plausible range are rejected to catch typos.
- **Sync:** new `body_measurements` table with the same `SyncMeta` as every other table and
  registered in `SyncTables`, so a backend picks it up with no extra work.
- **Schema v3** via Room auto-migration (new table). Verified on the simulator's v2 database with
  data: everything kept.

## Visual refresh ("Calibrated plates")

The owner found the UI dated. Three directions were mocked (Whiteboard, Calibrated plates,
Clean modern); the owner picked **Calibrated plates** and asked for a floating iOS-style tab bar.

- **Palette:** competition plate colors are the data language: red 25 / blue 20 / yellow 15 /
  green 10 / white 5 (`PlateColors`). Plate red is the action color, blue the secondary, green
  "done", yellow PRs. Grounds are cool grey (light) and graphite (dark); cards are flat tonal
  surfaces with no shadow. Dynamic color on Android 12+ still replaces the scheme when enabled;
  `PlateColors` stay fixed because they mean real plates.
- **Type:** system faces (no bundled font), heavier and tighter at display/headline sizes, and
  tabular figures (`tnum`) in every style so kg, reps and timers line up and don't jitter.
- **Shapes:** 12 / 20 / 24 / 28 dp; segmented controls and chips at 12–14 dp.
- **Floating tab bar** (`FloatingTabBar`) on compact widths only: a pill above the home
  indicator with a sliding indicator and outlined → filled icons. Rail and drawer layouts are
  unchanged. Top-level screens add `LocalFloatingBarInset` (lists) or use
  `floatingBarAwareInsets()` (Scaffolds with a FAB/snackbar) so nothing hides behind it. Tabs
  are a `selectableGroup` with `Role.Tab`, so screen readers announce position and state.
- **Plates on screen:** barbell cards in a workout show the per-side plate stack for the top
  working weight (logged, or last time when pending), and the log sheet draws it next to
  "Per side: 25 + 15". The stack has a spoken label on the card and is hidden from screen
  readers in the sheet, where the text already says it. Settings shows owned plates in their
  colors, with a check mark so selection never depends on color alone.
- **Home:** one graphite hero card ("Next up" / "In progress") with a yellow eyebrow; the
  weekly goal ring became a row of plates that load as you train (`WeekPlates`).
- **Shared components:** `LiftlyCard` (flat, springs to 97 % while pressed), `PillTabs`
  (segmented control with a sliding thumb, replaces tab rows and segmented buttons),
  `LiftlyChip`, `SearchFieldDefaults` (filled search), `PlateStack`, `WeekPlates`. The status
  check springs in when an exercise is logged.
- **Dates** in lists read "Sat, 21 Nov" (year only when it isn't this year) via `DateLabels`
  instead of ISO dates.

## Milestone 16 (Phase 3): ongoing workout notification and Live Activity

- **One interface, one publisher.** `OngoingWorkoutIndicator` (`show(OngoingWorkout)` / `end()`) lives
  in `core/platform`; `OngoingWorkoutPublisher` (`feature/workout`) observes the active workout in
  `AppScope` and drives it, started once from `initKoin`. `OngoingWorkout` uses epoch millis so Swift
  reads it without Kotlin time types. Desktop binds a no-op.
- **A rest that runs out is dropped on time by the publisher**, not when the app next clears
  `restEndsAt`, so the notification/Live Activity flips back to elapsed time even with the app closed.
- **Android:** a foreground service of type `specialUse` (none of the predefined types fits a
  user-started workout timer; `health` needs body sensors) with a low-importance ongoing
  notification: a system chronometer for elapsed time, a count-down chronometer while resting,
  "n of m logged · Next: X", tap opens the app. Started only when a workout appears (the app is in
  the foreground then); updates go straight to `NotificationManager`; a failure to start is logged.
  The notification text is English for now (core/platform has no string resources).
- **Rest alarms on time on Android:** `AndroidNotifier` posts "rest is over" from a main-thread timer
  backed by a partial wake lock (released when it fires or is cancelled). The process is kept alive
  by the foreground service. The AlarmManager alarm stays as the fallback for when the process is
  gone; the timer cancels it after posting and the notification uses `setOnlyAlertOnce`, so there is
  never a double alert. Still no `USE_EXACT_ALARM`.
- **iOS:** ActivityKit is Swift-only, so `LiveActivityIndicator` (Swift) implements the Kotlin
  protocol and is registered in `iOSApp.init` via `OngoingWorkoutIndicatorHolder` (Kotlin), which
  replays the last state if the delegate arrives late. `core/platform` is exported from the
  `ComposeApp` framework for this. Lock screen and Dynamic Island draw time with
  `Text(timerInterval:)` / `.timer`, so they tick without app updates.
- **Widget extension target** `LiftlyWidgets` (also for M18 widgets) was added with the Ruby
  `xcodeproj` gem (`tools/ios/add_widget_target.rb`, idempotent). Its deployment target is 16.1
  (first iOS with Live Activities); the app stays at 16.0 and guards the code with
  `@available(iOS 16.1, *)`. `WorkoutActivityAttributes` is compiled into both targets. The
  extension uses the same signing team as the app. `NSSupportsLiveActivities` is set.

## Milestone 17 (Phase 3): app shortcut and Quick Action

- **"Start next workout"** (`StartNextWorkoutUseCase`): resume the workout in progress, else start
  the next routine in rotation (`NextRoutine.pick`), else an empty workout, so the shortcut never
  dead-ends, even before the first routine exists. Either way it opens the workout screen.
- **One entry point for links:** `DeepLinks` (composeApp) turns `liftly://start-next-workout` and
  `liftly://workout` into an `AppAction` and holds it until the app shell consumes it, so a link that
  arrives before the first frame (cold start) isn't lost. Widgets (M18), the Android ongoing
  notification and the Live Activity use the same links. The shell consumes them in one long-lived
  collector: keying an effect on the pending value cancelled the action as soon as it was consumed.
- **Android:** a static shortcut (`res/xml/shortcuts.xml`) firing a `VIEW liftly://…` intent at
  `MainActivity` (now `singleTop`, handles `onNewIntent`). Static rather than dynamic: the label never
  changes, and it works before the app has ever run.
- **iOS:** a static Quick Action whose type is the link itself (`UIApplicationShortcutItems`), plus the
  `liftly` URL scheme. With the SwiftUI lifecycle these are delivered to a scene delegate
  (`AppDelegate.swift`), which forwards them to `DeepLinks`; once a custom scene delegate is set,
  SwiftUI's `onOpenURL` no longer fires, so URLs are handled there too.

## Milestone 18 (Phase 3): home-screen widgets

- **One widget, same on both platforms:** "THIS WEEK 2/3" with the week's plates, the weekly streak,
  and one button: "Start <next routine>" (`liftly://start-next-workout`) or "Resume workout"
  (`liftly://workout`) while one is running. It reuses the M17 deep links, so tapping it behaves
  exactly like the shortcut.
- **Data flow:** `HomeWidgetPublisher` (`feature/home`, started from `initKoin`) computes a
  `WidgetSnapshot` with the same `TrainingCalendar` and `NextRoutine` as Home and hands it to a
  `HomeWidgetBridge`. The snapshot is six plain lines of text, so Kotlin, Glance and Swift read it
  without a shared JSON model.
- **The widget rolls the week over on its own:** the snapshot carries its week's Monday; past it, a
  widget shows 0 this week and keeps the streak only if the old week met the goal (the same rule as
  `TrainingCalendar`). iOS adds a timeline entry at next Monday 00:00; Android re-reads every 3 hours
  (`updatePeriodMillis`).
- **Android:** Glance 1.2.0 (latest stable) in `androidApp`, since Glance needs the Compose
  compiler and lives with the app's receivers. Its bridge is bound in `LiftlyApplication`; the
  snapshot is kept in SharedPreferences and `updateAll` redraws.
- **iOS:** WidgetKit in the M16 `LiftlyWidgets` extension (small + medium). WidgetKit is Swift-only,
  so `AppGroupWidgetBridge` (Swift) implements the Kotlin bridge via `HomeWidgetBridgeHolder`, writes to
  the **App Group** `group.com.himanshu.liftly` and reloads the timeline. **The owner must enable this
  App Group for their team** (Signing & Capabilities, both targets) before installing on a real
  device; the simulator works without it.
- Widget text is English for now, like the ongoing notification.
- Desktop has no widgets (`NoHomeWidgets`).

## App icon

- **A loaded barbell in competition-plate colors** (red 25, blue 20, yellow 15 per side) on the
  graphite ground: the "Calibrated plates" palette, readable at small sizes.
- **One source:** `tools/icon/render_icon.swift` draws it with Core Graphics (no extra tools needed)
  and writes the iOS 1024 px icon (opaque, single-size asset catalog) and the Android
  adaptive-icon foreground + monochrome (themed icon) vectors, scaled into the 66 dp safe zone.
  Build and run it with `xcrun swiftc tools/icon/render_icon.swift -o /tmp/render_icon && /tmp/render_icon`
  from the repo root (the `swift` interpreter crashes on it).
- Desktop keeps the default window icon for now.

## Milestone 19 (backend): shared sync protocol module

- **Plan:** backend on Oracle Cloud (Ktor + PostgreSQL + Caddy in Docker on an Always Free A1 VM),
  app stays offline-first, sign-in optional. Milestones 19–26: protocol module, server, server
  auth, app network + sign-in, sync on, background sync, OCI deploy, store compliance.
- **`core/protocol`** holds the wire format: `SyncRecord`, `PushAck`, `PullPage`, auth DTOs
  (`SignInRequest`, `RefreshRequest`, `AuthTokens`, `Account`, `ApiError`) and
  `SyncProtocol.TABLES` / `MAX_BATCH`. It depends only on kotlinx-serialization (via the base
  convention), so the JVM server can use it without Room. Named `protocol`, not `sync-protocol`,
  because it covers auth too.
- `RemoteSyncDataSource` stays in `core/data`: it's the app's port; the server only needs the DTOs.
- **Wire format is pinned** by golden-JSON tests (`SyncProtocolTest`); a test in `SyncEngineTest`
  checks `SyncTables` matches `SyncProtocol.TABLES`, since the server will reject unknown tables.
- One refresh token per sign-in, single-use (rotated on refresh); access token expiry is sent as
  epoch millis so the app can refresh before it expires.

## Milestone 20 (backend): Ktor server running locally

- **`server/`** is a plain Kotlin/JVM module in this repo (Ktor 3 + Netty, HikariCP, Flyway,
  PostgreSQL JDBC). Its runtime may depend only on `core/protocol`; `checkNoAppModules` (part of
  `:server:check`) enforces it. App modules (`core/data`, Room) are allowed as **test**
  dependencies only, to drive the real `SyncEngine` against the server.
- **Plain JDBC, no ORM:** three SQL statements (upsert, current version, pull) don't need Exposed.
- **One generic `records` table** keyed by `(user_id, tbl, id)`, payload as `jsonb`. The server
  never reads the payload, so new app columns/tables need no server change (new table names
  still go in `SyncProtocol.TABLES`).
- **Same semantics as `FakeServer`:** last-write-wins on `updatedAt` (ties go to the incoming
  write); every accepted write gets `nextval('record_version_seq')`; an older write is acked with
  the current version so the client stops re-pushing and picks up the winner on its next pull.
- **Pull cursor = last version seen.** To keep that safe, a user's pushes are serialized with
  `pg_advisory_xact_lock(hashtextextended(user_id))`, so one user's versions commit in order and a
  pull can't advance past a lower version that commits later. Different users never block each other.
- **Validation:** table must be in `SyncProtocol.TABLES` (404), record table must match the URL,
  ids 1–128 chars, at most `MAX_BATCH` records per push, `limit` 1–`MAX_BATCH`, numeric cursor.
  Request body size is capped at the proxy (Caddy, Milestone 25).
- **Dev auth** (`LIFTLY_DEV_AUTH=true` + `X-Liftly-Dev-User` header) stands in for sign-in until
  Milestone 21; with it off, every sync call is 401. The server logs a warning when it's on.
- **Logging:** method, path, status only; never bodies (workout data) or auth headers.
- **No Docker on the dev Mac**, so local runs and tests use zonky embedded PostgreSQL 18 (real
  Postgres binaries, same major version as the `postgres:18` image): `./gradlew :server:runDev`.
  `Dockerfile` + `server/docker-compose.yml` are written but not yet run; they get exercised when
  Docker is installed or at deploy (Milestone 25).
- **The Docker image contains only the built distribution** (`installDist`), built on the host,
  because any Gradle build in this repo needs the Android SDK. Runs as a non-root user;
  `eclipse-temurin:21-jre` is multi-arch for the Ampere (arm64) VM.

## Milestone 21 (backend): sign-in on the server

- **Sign in with a provider ID token**, not passwords: the app gets an OpenID Connect ID token
  from Apple (iOS) or Google (Android) and posts it to `/auth/apple` or `/auth/google`. The server
  checks the RS256 signature against the provider's published keys (JWKS, cached a day,
  rate-limited fetches via `jwks-rsa`), issuer, audience (`APPLE_AUDIENCES` = bundle id,
  `GOOGLE_AUDIENCES` = OAuth client ids), expiry (60 s leeway), and for Apple the nonce
  (`sha256(rawNonce)` in the token, raw nonce in the request) so a captured token can't be replayed.
  A provider with no audiences configured is off (404 `provider_disabled`).
- **Accounts are keyed by the provider's `sub`.** Apple and Google sign-ins are separate accounts;
  emails are never used to link them (Apple's are often private relays, and linking by email is an
  account-takeover risk). Google emails are stored only when `email_verified`.
- **Our tokens:** 15-minute HS256 access JWT (`JWT_SECRET`, `iss=liftly`, `aud=liftly-app`) and a
  30-day opaque refresh token (32 random bytes), stored only as a SHA-256 hash. Refresh is
  single-use rotation. **Reuse detection with a 2-minute grace:** re-presenting a rotated token
  within 2 minutes gets a fresh token (a phone retrying after losing the response on bad gym
  signal must not be signed out); after that it's treated as theft and every session of the
  account is revoked. The app should still make refresh single-flight (Milestone 22).
- **Account deletion** deletes the `users` row; `records` and `refresh_tokens` go with it via
  `ON DELETE CASCADE`. It takes the same per-user advisory lock as push, and the `records → users`
  foreign key turns a push with a still-valid access token for a deleted account into a 401
  instead of orphaned data. V2 backfills owner rows for data written with dev auth.
- **Apple token revocation on deletion is still to do:** App Store rules require revoking the
  user's Apple tokens via Apple's REST API when they delete their account. That needs the
  authorization code from the app and a Sign in with Apple private key, so it lands with the app's
  sign-in (Milestone 22) and is checked again at release (Milestone 26).
- **Rate limit:** `/auth/*` 20 requests/minute per client IP (Ktor `RateLimit`, 429 `rate_limited`).
  Client IP comes from `X-Forwarded-For` (Caddy), so the server port must not be public.
- **Dev auth** now creates a `users` row for the header id (needed by the foreign key) and is
  tried after the Bearer token; it only exists when `LIFTLY_DEV_AUTH=true`.
- **Tests play Apple and Google** with a local RSA key (`TestAuth`): nonce, audience, expiry,
  forged-signature, unverified-email, rotation/grace/theft, logout, expiry and deletion cases run
  against real PostgreSQL with a controllable clock. No network calls to Apple or Google in tests.

## Milestone 22 (backend): sign-in in the app

- **Account is optional and lives at the top of Settings.** Signed out: a short explanation and
  the platform's sign-in button. Signed in: the email (or "Signed in"), Sign out, and Delete
  account behind a confirmation dialog. Signing in doesn't sync yet; that's Milestone 23.
- **iOS: Sign in with Apple** through a Swift bridge (`AppleSignIn.swift`, registered via
  `AppleSignInBridgeHolder` like the Live Activity). Swift generates the nonce (SecRandom) and
  hashes it (CryptoKit), returns the ID token + raw nonce. The button is Apple's own
  `ASAuthorizationAppleIDButton` embedded with `UIKitView` (its look is required), white on the
  dark theme, black on light. (Its corner radius doesn't render through Compose's UIKit interop, so it
  stays square, which Apple's guidelines allow.)
  Entitlement `com.apple.developer.applesignin` added; **the App ID needs the "Sign in with
  Apple" capability in the developer account before it works on a device.**
- **Android: Google via Credential Manager** (`GetSignInWithGoogleOption`), shown over the
  activity `MainActivity` registers. It needs our server's Google OAuth **Web** client id in the
  Gradle property `liftly.googleServerClientId` (→ `BuildConfig`); without it the Google button is
  hidden. The button is a plain outlined "Continue with Google"; Google's branded button is a
  release item (Milestone 26).
- **Tokens in platform secure storage** (`SecureStore`): iOS Keychain from Swift
  (`KeychainStore.swift`, `AfterFirstUnlockThisDeviceOnly`, so background sync can read it and it
  never moves to another device via backup); Android: AES-GCM key in the Android Keystore + private
  preferences (Jetpack `EncryptedSharedPreferences` is deprecated; undecryptable values, e.g.
  after a backup restore, read as signed out). Desktop keeps them in memory only.
- **`core/network`** (new): Ktor client (OkHttp / Darwin / Java engines), `AuthApi` and
  `AuthSession` (implements domain `AuthRepository`). Access tokens are refreshed 60 s before
  expiry and after a 401 (one retry). Refresh is **single-flight** behind a mutex, so concurrent
  requests never present the same refresh token twice. Only a 401 from the server ends the
  session (`SignedOut(expired = true)`, "You were signed out"); network errors never do. Sign-out
  clears tokens locally first, then revokes on the server best-effort.
- **Dev server URL** for now: `http://localhost:8080` (iOS simulator, desktop) and
  `http://10.0.2.2:8080` (Android emulator). Plain HTTP is allowed only there: iOS
  `NSAllowsLocalNetworking`, Android a debug-only network security config. The production URL
  comes with deploy (Milestone 25).
- **Tested end to end:** `AppSessionAgainstServerTest` runs the app's `AuthSession` against the
  real server (sign in with a test-signed Apple token, refresh, delete). On the simulator the
  Apple sheet opens from the button; a full sign-in needs an Apple Account signed in on the
  simulator and the capability above.
- **Still to do for Apple account deletion:** revoking Apple tokens (App Store rule) needs the
  authorization code and an Apple private key on the server; tracked for Milestone 26.

## Milestone 23 (backend): sync on

- **Sync runs only while signed in.** `AccountSyncController` replaces `BackgroundSyncController`
  (removed): signed out it reports `LocalOnly` and does nothing; signed in it runs the engine
  through `HttpSyncDataSource` (`core/network`, access token via `AuthSession.authorized`), 2 s
  after a write request, on sign-in / startup while signed in, when the app comes to the
  foreground (`LifecycleResumeEffect` in `App`), and from "Sync now" in Settings. Runs are
  sequential (one collector). Requests go through a conflated `Channel`, not a `SharedFlow`, so
  the sign-in request at startup isn't dropped before the collector subscribes.
- **Signing out keeps local data** (matches the account dialogs: "Workouts on this device stay").
  The plan said to wipe it; keeping it is safer: nothing logged offline is ever lost, and the app
  is fully usable signed out. Local changes keep collecting as `PENDING`.
- **Sync state belongs to an account**, recorded as a `_account` row in `sync_cursors` (no schema
  change). Before each run `SyncSql.bindAccount` checks it; if a *different* account (or none)
  owned it, local data is treated as never uploaded: tombstones are dropped, every user-changed
  row becomes `PENDING` with no server version, and cursors are cleared, so it goes to the new
  account as new rows and no cursor from another account is reused. The same account signing back
  in continues where it was. This also covers a deleted account (new user id on next sign-in) and
  is crash-safe (nothing to do at sign-out time). Trade-off: on a shared device, the next person's
  account receives this device's workouts; acceptable for a personal training log.
- **Library exercises merge on pull** (`TableSync.merge`, `mergeExercise`): for seeded rows only
  favorite, note, archived and rest time come from the server; the library text stays this
  device's, which may be from a newer seed.
- **Personal records need no recompute:** bests and the Records tab are computed from synced sets;
  PR rows are "you hit a PR" events created once, by the device that logged the set, so they never
  duplicate across devices.
- **Status in Settings** under the account: "Syncing…", "Backed up · last synced 14:05" (or the
  date), or "Couldn't sync. Liftly will try again." in the error color, plus "Sync now". Failures
  retry on the next write or foreground; timed backoff and background runs are Milestone 24.
- **Tests:** `AccountSyncControllerTest` (signed out → nothing; sign-in uploads offline changes;
  sign-out keeps data; account switch re-uploads; same account doesn't; seeded-row merge) runs in
  real time because Room does real I/O. `AppSessionAgainstServerTest` runs three phones through the
  real server: two on one account share a routine, the third account sees nothing.

## Milestone 24 (backend): background sync and retries

- **Retry with backoff in `AccountSyncController`:** a failed run schedules its own retry after
  30 s, doubling each time up to 30 min; any success resets it, and any new run cancels a pending
  retry. Retries stop by themselves when signed out (a run while signed out is a no-op), including
  when the server ends the session.
- **No connectivity monitor.** Backoff already covers "offline at the gym" (it catches up within
  the next retry once online, or immediately on the next write / foreground), and Android's
  WorkManager waits for a network itself. A reachability listener per platform wasn't worth it.
- **`SyncController.syncNow()`** (suspend, returns success) for background tasks; runs and
  requests share a mutex, so they never overlap. Default implementation just requests a sync, so
  fakes don't change.
- **Android:** `SyncWorker` (WorkManager `CoroutineWorker`), unique periodic work every 6 hours
  with a network constraint, scheduled (KEEP) from `LiftlyApplication`; `Result.retry()` on
  failure uses WorkManager's own backoff.
- **iOS:** `BGAppRefreshTask` `com.himanshu.liftly.sync` (Info.plist
  `BGTaskSchedulerPermittedIdentifiers` + `UIBackgroundModes: fetch`), registered in
  `AppDelegate.didFinishLaunching`, scheduled when the scene goes to the background and again from
  each run (earliest 6 hours later; iOS picks the time). The handler calls Kotlin
  `runBackgroundSync()` on the main thread and completes the task exactly once, whether the sync
  or the expiration comes first. Koin now starts through `IosStartup.ensureStarted()`, so a
  background launch with no UI can sync too.
- **Battery:** nothing runs while signed out except a no-op worker; signed in, one run is a few
  small requests. Every 6 hours is plenty for a training log that also syncs on open and on writes.

## Google sign-in on iOS

- **iOS offers both Apple and Google** (Apple first; App Store rules require Sign in with Apple
  whenever another social login is offered, at least as prominently).
- **No Google SDK:** `GoogleSignIn.swift` runs the standard OAuth flow for native apps
  (authorization code + PKCE, RFC 8252) in an `ASWebAuthenticationSession`, which handles the
  reversed-client-id redirect itself (no URL type in Info.plist), then exchanges the code at
  Google's token endpoint (iOS clients have no secret) and passes the **ID token** to our server,
  which verifies it like Android's. Keeps the Xcode project free of Swift packages.
- **Config:** the iOS OAuth client id goes in `GOOGLE_IOS_CLIENT_ID` (`Configuration/Config.xcconfig`
  → Info.plist `LiftlyGoogleClientID`); empty hides the button. The server must list it in
  `GOOGLE_AUDIENCES` too, since the token's audience is the iOS client id.
- The two iOS bridges share `SignInCallback` (raw nonce only for Apple). While a sign-in runs,
  only the tapped button shows progress.
- **Sign in with Apple is a build switch** (`SIGN_IN_WITH_APPLE` in `Config.xcconfig`, default
  `NO`). Personal (free) Apple developer teams can't create a provisioning profile with the
  capability, so with it always on the app no longer installed on the owner's iPhone. `NO` signs
  with `iosApp.entitlements` (no Apple sign-in) and hides the Apple button (Info.plist
  `LiftlySignInWithApple`); `YES` uses `iosAppSignInWithApple.entitlements` and shows it. Turn it
  on with a paid team, which App Store distribution needs anyway; the App Store rule "offer Apple
  when you offer Google" then holds for release builds.
