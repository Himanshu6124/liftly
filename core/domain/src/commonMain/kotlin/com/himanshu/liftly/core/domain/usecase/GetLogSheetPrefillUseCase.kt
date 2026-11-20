package com.himanshu.liftly.core.domain.usecase

import com.himanshu.liftly.core.domain.model.DraftSet
import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.core.domain.model.InputMode
import com.himanshu.liftly.core.domain.model.LogSheetData
import com.himanshu.liftly.core.domain.model.LogSheetPrefill
import com.himanshu.liftly.core.domain.model.PrefillSource
import com.himanshu.liftly.core.domain.model.WorkoutSet

/**
 * What the Batch Log Sheet opens with, so a normal day is "tap card → Save":
 * 1. Already logged in this workout → its saved sets (editing).
 * 2. Else the last session of this exercise → same set count, weights, reps, set types.
 * 3. Else the routine target → targetSets × targetReps @ targetWeight.
 * 4. Else [defaultSetCount] empty sets.
 * The input mode is the one remembered for this exercise (last session), else UNIFORM.
 */
class GetLogSheetPrefillUseCase {
    operator fun invoke(data: LogSheetData, defaultSetCount: Int): LogSheetPrefill {
        val we = data.workoutExercise
        if (we.status == ExerciseStatus.LOGGED && data.currentSets.isNotEmpty()) {
            return LogSheetPrefill(data.currentSets.toDrafts(), we.inputMode, PrefillSource.CURRENT_WORKOUT)
        }
        val last = data.lastSession
        if (last != null && last.sets.isNotEmpty()) {
            return LogSheetPrefill(last.sets.toDrafts(), last.inputMode, PrefillSource.LAST_SESSION)
        }
        val rememberedMode = last?.inputMode ?: InputMode.UNIFORM
        val target = data.routineTarget
        if (target != null && target.sets > 0) {
            val sets = List(target.sets) { DraftSet(weightKg = target.weightKg, reps = target.reps) }
            return LogSheetPrefill(sets, rememberedMode, PrefillSource.ROUTINE_TARGET)
        }
        return LogSheetPrefill(List(defaultSetCount.coerceAtLeast(1)) { DraftSet() }, rememberedMode, PrefillSource.DEFAULT)
    }

    private fun List<WorkoutSet>.toDrafts() = sortedBy { it.orderIndex }.map {
        DraftSet(it.weightKg, it.reps, it.durationSec, it.distanceM, it.rpe, it.setType)
    }
}
