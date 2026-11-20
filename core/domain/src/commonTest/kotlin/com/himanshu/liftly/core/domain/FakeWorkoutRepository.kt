package com.himanshu.liftly.core.domain

import com.himanshu.liftly.core.domain.model.ActiveWorkout
import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.core.domain.model.RemovedWorkoutExercise
import com.himanshu.liftly.core.domain.model.Workout
import com.himanshu.liftly.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Instant

class FakeWorkoutRepository : WorkoutRepository {
    val created = mutableListOf<Workout>()
    override fun observeActiveWorkout(): Flow<ActiveWorkout?> = flowOf(null)
    override suspend fun getInProgressWorkout(): Workout? = created.lastOrNull { it.endedAt == null }
    override suspend fun createWorkout(name: String, routineId: String?, exerciseIds: List<String>): String {
        val w = Workout("w${created.size + 1}", name, routineId, Instant.fromEpochMilliseconds(0), null, null)
        created += w
        return w.id
    }
    override suspend fun addExercises(workoutId: String, exerciseIds: List<String>) = Unit
    override suspend fun rename(workoutId: String, name: String) = Unit
    override suspend fun reorder(workoutId: String, orderedWorkoutExerciseIds: List<String>) = Unit
    override suspend fun setStatus(workoutExerciseId: String, status: ExerciseStatus) = Unit
    override suspend fun removeExercise(workoutExerciseId: String): RemovedWorkoutExercise? = null
    override suspend fun restoreExercise(removed: RemovedWorkoutExercise) = Unit
    override suspend fun finish(workoutId: String) = Unit
    override suspend fun discard(workoutId: String) = Unit
    override fun observeRecentExerciseIds(limit: Int): Flow<List<String>> = flowOf(emptyList())
}
