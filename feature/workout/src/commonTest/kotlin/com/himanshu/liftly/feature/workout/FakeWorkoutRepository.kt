package com.himanshu.liftly.feature.workout

import com.himanshu.liftly.core.domain.model.ActiveWorkout
import com.himanshu.liftly.core.domain.model.ActiveWorkoutExercise
import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.Exercise
import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.core.domain.model.InputMode
import com.himanshu.liftly.core.domain.model.Muscle
import com.himanshu.liftly.core.domain.model.RemovedWorkoutExercise
import com.himanshu.liftly.core.domain.model.TrackingType
import com.himanshu.liftly.core.domain.model.Workout
import com.himanshu.liftly.core.domain.model.WorkoutExercise
import com.himanshu.liftly.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlin.time.Instant

/** Minimal in-memory workout store driving the ViewModel through the same flow Room would. */
class FakeWorkoutRepository(exerciseNames: List<String>) : WorkoutRepository {
    private val workout = Workout("w1", "", null, Instant.fromEpochMilliseconds(0), null, null)
    val active = MutableStateFlow<ActiveWorkout?>(
        ActiveWorkout(workout, exerciseNames.mapIndexed { i, name -> item("we$i", name, i) }),
    )
    var finished = false
    var discarded = false

    private fun item(id: String, name: String, order: Int) = ActiveWorkoutExercise(
        workoutExercise = WorkoutExercise(id, "w1", "ex-$name", order, null, ExerciseStatus.PENDING, null, InputMode.UNIFORM, null),
        exercise = Exercise(
            "ex-$name", null, name, emptyList(), Muscle.CHEST, emptyList(), Equipment.BARBELL, TrackingType.WEIGHT_REPS,
            null, null, emptyList(), null, emptyList(), emptyList(), emptyList(), null, false, false, false,
        ),
        sets = emptyList(),
    )

    private fun edit(transform: (List<ActiveWorkoutExercise>) -> List<ActiveWorkoutExercise>) =
        active.update { it?.copy(exercises = transform(it.exercises)) }

    fun names() = active.value?.exercises?.map { it.exercise.name }
    fun status(id: String) = active.value?.exercises?.first { it.workoutExercise.id == id }?.workoutExercise?.status

    override fun observeActiveWorkout(): Flow<ActiveWorkout?> = active
    override suspend fun getInProgressWorkout() = active.value?.workout
    override suspend fun createWorkout(name: String, routineId: String?, exerciseIds: List<String>) = "w1"
    override suspend fun addExercises(workoutId: String, exerciseIds: List<String>) =
        edit { list -> list + exerciseIds.mapIndexed { i, id -> item("new$i", id, list.size + i) } }
    override suspend fun rename(workoutId: String, name: String) = active.update { it?.copy(workout = it.workout.copy(name = name)) }
    override suspend fun reorder(workoutId: String, orderedWorkoutExerciseIds: List<String>) =
        edit { list -> orderedWorkoutExerciseIds.map { id -> list.first { it.workoutExercise.id == id } } }
    override suspend fun setStatus(workoutExerciseId: String, status: ExerciseStatus) = edit { list ->
        list.map { e ->
            if (e.workoutExercise.id == workoutExerciseId) e.copy(workoutExercise = e.workoutExercise.copy(status = status)) else e
        }
    }
    private val removed = mutableMapOf<String, Pair<Int, ActiveWorkoutExercise>>()
    override suspend fun removeExercise(workoutExerciseId: String): RemovedWorkoutExercise? {
        val list = active.value?.exercises ?: return null
        val index = list.indexOfFirst { it.workoutExercise.id == workoutExerciseId }
        removed[workoutExerciseId] = index to list[index]
        edit { it.filterNot { e -> e.workoutExercise.id == workoutExerciseId } }
        return RemovedWorkoutExercise(list[index].workoutExercise, emptyList())
    }
    override suspend fun restoreExercise(removed: RemovedWorkoutExercise) {
        val (index, item) = this.removed.getValue(removed.workoutExercise.id)
        edit { it.toMutableList().apply { add(index, item) } }
    }
    override suspend fun finish(workoutId: String) {
        finished = true
        active.value = null
    }
    override suspend fun discard(workoutId: String) {
        discarded = true
        active.value = null
    }
    override fun observeRecentExerciseIds(limit: Int): Flow<List<String>> = flowOf(emptyList())
}
