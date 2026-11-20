package com.himanshu.liftly.core.domain.repository

import com.himanshu.liftly.core.domain.model.ActiveWorkout
import com.himanshu.liftly.core.domain.model.DraftSet
import com.himanshu.liftly.core.domain.model.EntityId
import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.core.domain.model.InputMode
import com.himanshu.liftly.core.domain.model.LastSession
import com.himanshu.liftly.core.domain.model.LogSheetData
import com.himanshu.liftly.core.domain.model.LogSnapshot
import com.himanshu.liftly.core.domain.model.PersonalRecord
import com.himanshu.liftly.core.domain.model.RemovedWorkoutExercise
import com.himanshu.liftly.core.domain.model.Workout
import com.himanshu.liftly.core.domain.model.WorkoutSet
import com.himanshu.liftly.core.domain.model.WorkoutSummaryLine
import com.himanshu.liftly.core.domain.usecase.PrHit
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

    /** Everything the Batch Log Sheet needs for one workout exercise, or null if it no longer exists. */
    suspend fun getLogSheetData(workoutExerciseId: EntityId): LogSheetData?

    /** Most recent logged session of [exerciseId] outside [excludeWorkoutId]. */
    suspend fun getLastSession(exerciseId: EntityId, excludeWorkoutId: EntityId): LastSession?

    /**
     * In one transaction: replaces the sets, marks the exercise LOGGED (+ loggedAt), remembers the input mode.
     * Returns the previous state so the change can be undone.
     */
    suspend fun logSets(
        workoutExerciseId: EntityId,
        sets: List<DraftSet>,
        inputMode: InputMode,
        notes: String? = null,
        personalRecords: List<PrHit> = emptyList(),
    ): LogSnapshot

    /** Every logged session of [exerciseId] except [excludeWorkoutExerciseId], one list of sets per session. */
    suspend fun getSessionHistory(exerciseId: EntityId, excludeWorkoutExerciseId: EntityId): List<List<WorkoutSet>>

    /** Current best per record type for one exercise. */
    fun observeRecords(exerciseId: EntityId): Flow<List<PersonalRecord>>

    /** Puts a workout exercise back exactly as captured by [logSets]. */
    suspend fun restoreLog(snapshot: LogSnapshot)

    /** The most recently finished workout, for Home. */
    fun observeLastFinished(): Flow<WorkoutSummaryLine?>

    /** Ids of the most recently logged distinct exercises, newest first. */
    fun observeRecentExerciseIds(limit: Int): Flow<List<EntityId>>
}
