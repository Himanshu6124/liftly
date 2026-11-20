package com.himanshu.liftly.core.domain.model

import kotlin.time.Instant

/**
 * All ids are client-generated UUID strings so records can be created offline and later
 * pushed to a backend without id collisions or remapping.
 */
typealias EntityId = String

data class Exercise(
    val id: EntityId,
    val seedKey: String?,
    val name: String,
    val aliases: List<String>,
    val primaryMuscle: Muscle,
    val secondaryMuscles: List<Muscle>,
    val equipment: Equipment,
    val trackingType: TrackingType,
    val mechanic: Mechanic?,
    val difficulty: Difficulty?,
    val imagePaths: List<String>,
    val videoUrl: String?,
    val instructions: List<String>,
    val formCues: List<String>,
    val commonMistakes: List<String>,
    val userNote: String?,
    val isFavorite: Boolean,
    val isCustom: Boolean,
    val isArchived: Boolean,
)

data class Routine(
    val id: EntityId,
    val name: String,
    val notes: String?,
    val orderIndex: Int,
    val lastPerformedAt: Instant?,
    val createdAt: Instant,
)

data class RoutineExercise(
    val id: EntityId,
    val routineId: EntityId,
    val exerciseId: EntityId,
    val orderIndex: Int,
    val supersetGroup: Int?,
    val targetSets: Int,
    val targetReps: Int?,
    val targetWeightKg: Double?,
)

data class Workout(
    val id: EntityId,
    val name: String,
    val routineId: EntityId?,
    val startedAt: Instant,
    val endedAt: Instant?,
    val notes: String?,
) {
    val isInProgress: Boolean get() = endedAt == null
}

data class WorkoutExercise(
    val id: EntityId,
    val workoutId: EntityId,
    val exerciseId: EntityId,
    val orderIndex: Int,
    val supersetGroup: Int?,
    val status: ExerciseStatus,
    val loggedAt: Instant?,
    val inputMode: InputMode,
    val notes: String?,
)

data class WorkoutSet(
    val id: EntityId,
    val workoutExerciseId: EntityId,
    val orderIndex: Int,
    val weightKg: Double?,
    val reps: Int?,
    val durationSec: Int?,
    val distanceM: Double?,
    val rpe: Double?,
    val setType: SetType,
)

data class PersonalRecord(
    val id: EntityId,
    val exerciseId: EntityId,
    val type: PrType,
    val value: Double,
    val workoutSetId: EntityId,
    val achievedAt: Instant,
)

data class UserSettings(
    val weightUnit: WeightUnit = WeightUnit.KG,
    val weightIncrementKg: Double = 2.5,
    val weightIncrementLb: Double = 5.0,
    val defaultSetCount: Int = 3,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val openNextExerciseAutomatically: Boolean = false,
    /** Workouts per week the user aims for (Home goal ring and weekly streak). */
    val weeklyGoal: Int = 3,
)
