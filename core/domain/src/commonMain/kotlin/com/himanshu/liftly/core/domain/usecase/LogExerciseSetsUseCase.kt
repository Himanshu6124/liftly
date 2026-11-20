package com.himanshu.liftly.core.domain.usecase

import com.himanshu.liftly.core.domain.model.DraftSet
import com.himanshu.liftly.core.domain.model.EntityId
import com.himanshu.liftly.core.domain.model.InputMode
import com.himanshu.liftly.core.domain.model.LogSnapshot
import com.himanshu.liftly.core.domain.repository.WorkoutRepository

/**
 * Saves all sets of one exercise at once (the Batch Log Sheet's Save, or a swipe quick-log).
 * Empty sets are dropped; anything out of range is rejected before touching the database.
 * The repository writes everything in one transaction. PR detection hooks in here (Milestone 5).
 */
class LogExerciseSetsUseCase(private val workouts: WorkoutRepository) {
    sealed interface Result {
        data class Saved(val undo: LogSnapshot) : Result
        data class Invalid(val error: SetValidation.Error) : Result
    }

    suspend operator fun invoke(workoutExerciseId: EntityId, sets: List<DraftSet>, inputMode: InputMode): Result {
        val toSave = sets.filterNot(SetValidation::isEmpty)
        SetValidation.validate(toSave)?.let { return Result.Invalid(it) }
        return Result.Saved(workouts.logSets(workoutExerciseId, toSave, inputMode))
    }
}

object SetValidation {
    val REPS = 1..100
    const val MAX_WEIGHT_KG = 1000.0

    enum class Error { NO_SETS, REPS_OUT_OF_RANGE, WEIGHT_OUT_OF_RANGE, MISSING_REPS }

    fun isEmpty(set: DraftSet) = set.reps == null && set.durationSec == null && set.distanceM == null

    /** null = valid. Expects empty sets to be removed already. */
    fun validate(sets: List<DraftSet>): Error? {
        if (sets.isEmpty()) return Error.NO_SETS
        sets.forEach { s ->
            if (s.reps != null && s.reps !in REPS) return Error.REPS_OUT_OF_RANGE
            if (s.weightKg != null && (s.weightKg < 0.0 || s.weightKg > MAX_WEIGHT_KG)) return Error.WEIGHT_OUT_OF_RANGE
            if (s.weightKg != null && isEmpty(s)) return Error.MISSING_REPS
        }
        return null
    }
}
