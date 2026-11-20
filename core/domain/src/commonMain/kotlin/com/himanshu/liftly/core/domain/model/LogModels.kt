package com.himanshu.liftly.core.domain.model

import kotlin.time.Instant

/** A set being edited in the log sheet; nothing is persisted until Save. */
data class DraftSet(
    val weightKg: Double? = null,
    val reps: Int? = null,
    val durationSec: Int? = null,
    val distanceM: Double? = null,
    val rpe: Double? = null,
    val setType: SetType = SetType.NORMAL,
)

/** The most recent time this exercise was logged in another workout. */
data class LastSession(
    val workoutId: EntityId,
    val performedAt: Instant,
    val inputMode: InputMode,
    val sets: List<WorkoutSet>,
)

data class RoutineTarget(val sets: Int, val reps: Int?, val weightKg: Double?)

enum class PrefillSource { CURRENT_WORKOUT, LAST_SESSION, ROUTINE_TARGET, DEFAULT }

data class LogSheetPrefill(
    val sets: List<DraftSet>,
    val inputMode: InputMode,
    val source: PrefillSource,
)

/** Everything the log sheet needs, loaded in one go. */
data class LogSheetData(
    val workoutExercise: WorkoutExercise,
    val exercise: Exercise,
    val currentSets: List<WorkoutSet>,
    val lastSession: LastSession?,
    val routineTarget: RoutineTarget?,
)

/** State of a workout exercise before a log, so a quick-log can be undone exactly. */
data class LogSnapshot(
    val workoutExerciseId: EntityId,
    val status: ExerciseStatus,
    val loggedAt: Instant?,
    val inputMode: InputMode,
    val sets: List<WorkoutSet>,
)
