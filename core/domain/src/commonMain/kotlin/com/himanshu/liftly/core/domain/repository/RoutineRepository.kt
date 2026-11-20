package com.himanshu.liftly.core.domain.repository

import com.himanshu.liftly.core.domain.model.EntityId
import com.himanshu.liftly.core.domain.model.RoutineExerciseDraft
import com.himanshu.liftly.core.domain.model.RoutineWithExercises
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    /** All routines in the user's order, with their exercises in order. */
    fun observeRoutines(): Flow<List<RoutineWithExercises>>

    fun observeRoutine(id: EntityId): Flow<RoutineWithExercises?>

    /** Creates (id == null) or replaces a routine's name and exercise list. Returns its id. */
    suspend fun saveRoutine(id: EntityId?, name: String, notes: String?, exercises: List<RoutineExerciseDraft>): EntityId

    /** Soft delete; returns a token for [restoreRoutine] (Undo). */
    suspend fun deleteRoutine(id: EntityId): RoutineWithExercises?

    suspend fun restoreRoutine(snapshot: RoutineWithExercises)

    suspend fun reorderRoutines(orderedIds: List<EntityId>)

    /** Creates a routine from a workout's exercises with today's numbers as targets. */
    suspend fun saveWorkoutAsRoutine(workoutId: EntityId, name: String): EntityId

    /** Overwrites the targets of [routineId] with what was done in [workoutId] (exercises in both). */
    suspend fun updateTargetsFromWorkout(routineId: EntityId, workoutId: EntityId)
}
