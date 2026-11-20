package com.himanshu.liftly.core.domain.repository

import com.himanshu.liftly.core.domain.model.ActiveWorkout
import com.himanshu.liftly.core.domain.model.EntityId
import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.core.domain.model.RemovedWorkoutExercise
import com.himanshu.liftly.core.domain.model.Workout
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    /** The single in-progress workout (endedAt == null), or null. */
    fun observeActiveWorkout(): Flow<ActiveWorkout?>

    suspend fun getInProgressWorkout(): Workout?

    /** Creates a workout with the given exercises (in order). Callers enforce the one-in-progress rule. */
    suspend fun createWorkout(name: String, routineId: EntityId?, exerciseIds: List<EntityId>): EntityId

    suspend fun addExercises(workoutId: EntityId, exerciseIds: List<EntityId>)

    suspend fun rename(workoutId: EntityId, name: String)

    /** Persists a new order; [orderedWorkoutExerciseIds] lists every exercise of the workout. */
    suspend fun reorder(workoutId: EntityId, orderedWorkoutExerciseIds: List<EntityId>)

    suspend fun setStatus(workoutExerciseId: EntityId, status: ExerciseStatus)

    suspend fun removeExercise(workoutExerciseId: EntityId): RemovedWorkoutExercise?

    suspend fun restoreExercise(removed: RemovedWorkoutExercise)

    /** Ends the workout; still-pending exercises become SKIPPED. */
    suspend fun finish(workoutId: EntityId)

    /** Deletes the workout and everything in it. */
    suspend fun discard(workoutId: EntityId)

    /** Ids of the most recently logged distinct exercises, newest first. */
    fun observeRecentExerciseIds(limit: Int): Flow<List<EntityId>>
}
