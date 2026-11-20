package com.himanshu.liftly.core.domain.model

/** One finished workout in the History list. */
data class WorkoutHistoryItem(
    val workout: Workout,
    val exerciseCount: Int,
    val loggedCount: Int,
    val workingSets: Int,
    val volumeKg: Double,
    val prCount: Int,
)

/** Everything a deleted workout had, so Undo can put it back. */
data class DeletedWorkout(
    val workout: Workout,
    val exercises: List<WorkoutExercise>,
    val sets: List<WorkoutSet>,
    val personalRecords: List<PersonalRecord>,
)
