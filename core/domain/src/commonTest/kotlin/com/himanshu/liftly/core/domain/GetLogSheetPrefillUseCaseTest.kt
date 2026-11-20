package com.himanshu.liftly.core.domain

import com.himanshu.liftly.core.domain.model.DraftSet
import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.core.domain.model.InputMode
import com.himanshu.liftly.core.domain.model.LastSession
import com.himanshu.liftly.core.domain.model.LogSheetData
import com.himanshu.liftly.core.domain.model.PrefillSource
import com.himanshu.liftly.core.domain.model.RoutineTarget
import com.himanshu.liftly.core.domain.model.SetType
import com.himanshu.liftly.core.domain.model.WorkoutExercise
import com.himanshu.liftly.core.domain.model.WorkoutSet
import com.himanshu.liftly.core.domain.usecase.GetLogSheetPrefillUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class GetLogSheetPrefillUseCaseTest {
    private val prefill = GetLogSheetPrefillUseCase()

    private fun we(status: ExerciseStatus = ExerciseStatus.PENDING, mode: InputMode = InputMode.UNIFORM) =
        WorkoutExercise("we", "w", "bench", 0, null, status, null, mode, null)

    private fun set(i: Int, kg: Double?, reps: Int?, type: SetType = SetType.NORMAL) =
        WorkoutSet("s$i", "we", i, kg, reps, null, null, null, type)

    private fun data(
        status: ExerciseStatus = ExerciseStatus.PENDING,
        current: List<WorkoutSet> = emptyList(),
        last: LastSession? = null,
        target: RoutineTarget? = null,
    ) = LogSheetData(we(status, InputMode.PER_SET), exercise("Bench"), current, last, target)

    private val lastSession = LastSession(
        workoutId = "old",
        performedAt = Instant.fromEpochMilliseconds(0),
        inputMode = InputMode.PER_SET,
        sets = listOf(set(1, 77.5, 7), set(0, 77.5, 8), set(2, 40.0, 10, SetType.WARMUP)),
    )

    @Test
    fun editingUsesTheSavedSetsOfThisWorkout() {
        val result = prefill(
            data(status = ExerciseStatus.LOGGED, current = listOf(set(0, 80.0, 8)), last = lastSession),
            defaultSetCount = 3,
        )
        assertEquals(PrefillSource.CURRENT_WORKOUT, result.source)
        assertEquals(listOf(DraftSet(80.0, 8)), result.sets)
        assertEquals(InputMode.PER_SET, result.inputMode)
    }

    @Test
    fun otherwiseCopiesTheLastSessionInOrderWithSetTypesAndMode() {
        val result = prefill(data(last = lastSession, target = RoutineTarget(5, 5, 100.0)), defaultSetCount = 3)
        assertEquals(PrefillSource.LAST_SESSION, result.source)
        assertEquals(
            listOf(DraftSet(77.5, 8), DraftSet(77.5, 7), DraftSet(40.0, 10, setType = SetType.WARMUP)),
            result.sets,
        )
        assertEquals(InputMode.PER_SET, result.inputMode)
    }

    @Test
    fun otherwiseUsesTheRoutineTarget() {
        val result = prefill(data(target = RoutineTarget(sets = 4, reps = 10, weightKg = 60.0)), defaultSetCount = 3)
        assertEquals(PrefillSource.ROUTINE_TARGET, result.source)
        assertEquals(List(4) { DraftSet(60.0, 10) }, result.sets)
        assertEquals(InputMode.UNIFORM, result.inputMode)
    }

    @Test
    fun otherwiseDefaultEmptySets() {
        val result = prefill(data(), defaultSetCount = 3)
        assertEquals(PrefillSource.DEFAULT, result.source)
        assertEquals(List(3) { DraftSet() }, result.sets)
    }

    @Test
    fun skippedExerciseIsNotTreatedAsLogged() {
        val result = prefill(data(status = ExerciseStatus.SKIPPED, current = listOf(set(0, 1.0, 1)), last = lastSession), 3)
        assertEquals(PrefillSource.LAST_SESSION, result.source)
    }
}
