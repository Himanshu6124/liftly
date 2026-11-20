package com.himanshu.liftly.core.domain.usecase

import com.himanshu.liftly.core.domain.model.DraftSet
import com.himanshu.liftly.core.domain.model.EntityId
import com.himanshu.liftly.core.domain.model.InputMode
import com.himanshu.liftly.core.domain.model.LogSnapshot
import com.himanshu.liftly.core.domain.repository.WorkoutRepository
import com.himanshu.liftly.core.domain.util.toDraftSets

/**
 * Saves all sets of one exercise at once (the Batch Log Sheet's Save, or a swipe quick-log).
 * Empty sets are dropped; anything out of range is rejected before touching the database.
 * Personal records are detected against every *other* session of the exercise and written in the
 * same transaction as the sets, so an edit that lowers the numbers also removes its PRs.
 */
class LogExerciseSetsUseCase(
    private val workouts: WorkoutRepository,
    private val detectRecords: DetectPersonalRecordsUseCase = DetectPersonalRecordsUseCase(),
) {
    sealed interface Result {
        data class Saved(val undo: LogSnapshot, val records: List<PrHit>) : Result
        data class Invalid(val error: SetValidation.Error) : Result
    }

    suspend operator fun invoke(
        workoutExerciseId: EntityId,
        sets: List<DraftSet>,
        inputMode: InputMode,
        notes: String? = null,
    ): Result {
        val toSave = sets.filterNot(SetValidation::isEmpty)
        SetValidation.validate(toSave)?.let { return Result.Invalid(it) }
        val data = workouts.getLogSheetData(workoutExerciseId)
        val records = if (data == null) {
            emptyList()
        } else {
            val history = workouts.getSessionHistory(data.exercise.id, workoutExerciseId).map { it.toDraftSets() }
            detectRecords(SessionHistory(history), toSave)
        }
        val undo = workouts.logSets(workoutExerciseId, toSave, inputMode, notes, records)
        return Result.Saved(undo, records)
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
