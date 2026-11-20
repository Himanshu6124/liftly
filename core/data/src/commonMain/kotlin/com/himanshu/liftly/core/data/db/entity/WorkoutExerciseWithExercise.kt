package com.himanshu.liftly.core.data.db.entity

import androidx.room.Embedded
import androidx.room.Relation

data class WorkoutWithStats(
    @Embedded val workout: WorkoutEntity,
    val exerciseCount: Int,
    val loggedCount: Int,
    val workingSets: Int,
    val volumeKg: Double,
    val prCount: Int,
)

data class SetWithDate(
    @Embedded val set: WorkoutSetEntity,
    val performedAt: Long,
    val workoutId: String,
    val workoutName: String,
)

data class PersonalRecordWithWorkoutExercise(
    @Embedded val record: PersonalRecordEntity,
    val workoutExerciseId: String,
)

data class WorkoutExerciseWithExercise(
    @Embedded val workoutExercise: WorkoutExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity?,
)

data class ExerciseSetCountRow(
    val primaryMuscle: com.himanshu.liftly.core.domain.model.Muscle,
    val secondaryMuscles: List<com.himanshu.liftly.core.domain.model.Muscle>,
    val workingSets: Int,
)
