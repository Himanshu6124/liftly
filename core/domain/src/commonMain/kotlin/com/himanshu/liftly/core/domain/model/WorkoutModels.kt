package com.himanshu.liftly.core.domain.model

import kotlin.time.Instant

/** One exercise inside the active workout, with what the checklist card needs to show. */
data class ActiveWorkoutExercise(
    val workoutExercise: WorkoutExercise,
    val exercise: Exercise,
    val sets: List<WorkoutSet>,
    /** Records set by this exercise in this workout (drives the 🏆 badge). */
    val personalRecords: List<PersonalRecord> = emptyList(),
)

data class ActiveWorkout(
    val workout: Workout,
    val exercises: List<ActiveWorkoutExercise>,
) {
    val pendingCount: Int get() = exercises.count { it.workoutExercise.status == ExerciseStatus.PENDING }
}

/** Everything needed to put a removed exercise back exactly as it was (Undo). */
data class RemovedWorkoutExercise(
    val workoutExercise: WorkoutExercise,
    val sets: List<WorkoutSet>,
)

/** Input for creating a user-defined exercise. */
data class NewCustomExercise(
    val name: String,
    val primaryMuscle: Muscle,
    val equipment: Equipment,
    val trackingType: TrackingType,
)

/** Summary of the in-progress workout for the Home banner. */
data class InProgressSummary(val workoutId: EntityId, val name: String, val startedAt: Instant, val exerciseCount: Int)
