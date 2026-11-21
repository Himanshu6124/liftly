package com.himanshu.liftly.core.domain.usecase

import com.himanshu.liftly.core.domain.model.EntityId
import com.himanshu.liftly.core.domain.model.Exercise
import com.himanshu.liftly.core.domain.repository.WorkoutRepository

/**
 * Starts a workout, enforcing that only one can be in progress. If one already is, it is
 * returned instead of creating a second one, so "Start" doubles as "Resume" (fewer taps).
 */
class StartWorkoutUseCase(private val workouts: WorkoutRepository) {
    sealed interface Result {
        val workoutId: EntityId

        data class Started(override val workoutId: EntityId) : Result
        data class Resumed(override val workoutId: EntityId) : Result
    }

    suspend operator fun invoke(
        name: String = "",
        routineId: EntityId? = null,
        exerciseIds: List<EntityId> = emptyList(),
        supersetGroups: List<Int?> = emptyList(),
    ): Result {
        workouts.getInProgressWorkout()?.let { return Result.Resumed(it.id) }
        return Result.Started(workouts.createWorkout(name.trim(), routineId, exerciseIds, supersetGroups))
    }
}

/**
 * Picker suggestions: exercises that train the same primary muscles as what's already in the
 * workout, excluding ones already added. Favorites first, then by name.
 */
object SuggestExercises {
    fun suggest(library: List<Exercise>, inWorkout: Collection<EntityId>, limit: Int = 8): List<Exercise> {
        val inWorkoutSet = inWorkout.toSet()
        val muscles = library.filter { it.id in inWorkoutSet }.map { it.primaryMuscle }.toSet()
        if (muscles.isEmpty()) return emptyList()
        return library
            .filter { it.id !in inWorkoutSet && it.primaryMuscle in muscles }
            .sortedWith(compareByDescending<Exercise> { it.isFavorite }.thenBy { it.name.lowercase() })
            .take(limit)
    }
}
