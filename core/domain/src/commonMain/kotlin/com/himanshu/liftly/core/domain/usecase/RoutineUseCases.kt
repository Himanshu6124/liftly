package com.himanshu.liftly.core.domain.usecase

import com.himanshu.liftly.core.domain.model.DraftSet
import com.himanshu.liftly.core.domain.model.RoutineWithExercises
import com.himanshu.liftly.core.domain.model.SetType

/**
 * The routine Home offers as the big 1-tap button: the one done longest ago, so routines
 * rotate (Push → Pull → Legs → Push...). Never-done routines come first, in the user's order.
 */
object NextRoutine {
    fun pick(routines: List<RoutineWithExercises>): RoutineWithExercises? = routines
        .filter { it.items.isNotEmpty() }
        .minWithOrNull(
            compareBy<RoutineWithExercises> { it.routine.lastPerformedAt != null }
                .thenBy { it.routine.lastPerformedAt }
                .thenBy { it.routine.orderIndex },
        )
}

/** Routine targets derived from what was actually done: working sets, typical reps, top weight. */
object TargetsFromSets {
    data class Targets(val sets: Int, val reps: Int?, val weightKg: Double?)

    fun derive(sets: List<DraftSet>): Targets? {
        val working = sets.filter { it.setType != SetType.WARMUP && it.reps != null }
        if (working.isEmpty()) return null
        val reps = working.mapNotNull { it.reps }
        // Most common rep count; ties → the higher one (the target you were aiming for).
        val typical = reps.groupingBy { it }.eachCount().entries.maxWith(compareBy({ it.value }, { it.key })).key
        return Targets(sets = working.size, reps = typical, weightKg = working.mapNotNull { it.weightKg }.maxOrNull())
    }
}

/** Starts a workout from a routine with its exercises in order (or resumes the active one). */
class StartRoutineUseCase(private val startWorkout: StartWorkoutUseCase) {
    suspend operator fun invoke(routine: RoutineWithExercises): StartWorkoutUseCase.Result = startWorkout(
        name = routine.routine.name,
        routineId = routine.routine.id,
        exerciseIds = routine.items.sortedBy { it.routineExercise.orderIndex }.map { it.exercise.id },
    )
}
