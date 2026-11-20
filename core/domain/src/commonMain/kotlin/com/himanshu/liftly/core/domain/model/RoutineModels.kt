package com.himanshu.liftly.core.domain.model

data class RoutineItem(val routineExercise: RoutineExercise, val exercise: Exercise)

data class RoutineWithExercises(val routine: Routine, val items: List<RoutineItem>)

/** An exercise line in the routine editor (id null = not saved yet). */
data class RoutineExerciseDraft(
    val id: EntityId?,
    val exerciseId: EntityId,
    val targetSets: Int,
    val targetReps: Int?,
    val targetWeightKg: Double?,
)

/** Last finished workout, for the Home summary card. */
data class WorkoutSummaryLine(
    val workout: Workout,
    val exerciseCount: Int,
    val loggedCount: Int,
    val volumeKg: Double,
)
