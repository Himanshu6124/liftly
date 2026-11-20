package com.himanshu.liftly.core.domain

import com.himanshu.liftly.core.domain.model.DraftSet
import com.himanshu.liftly.core.domain.model.Routine
import com.himanshu.liftly.core.domain.model.RoutineExercise
import com.himanshu.liftly.core.domain.model.RoutineItem
import com.himanshu.liftly.core.domain.model.RoutineWithExercises
import com.himanshu.liftly.core.domain.model.SetType
import com.himanshu.liftly.core.domain.usecase.NextRoutine
import com.himanshu.liftly.core.domain.usecase.TargetsFromSets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class RoutineUseCasesTest {
    private fun routine(name: String, order: Int, lastDay: Long?, exercises: Int = 1) = RoutineWithExercises(
        Routine(name, name, null, order, lastDay?.let { Instant.fromEpochMilliseconds(it * 86_400_000) }, Instant.fromEpochMilliseconds(0)),
        List(exercises) { i ->
            RoutineItem(RoutineExercise("$name$i", name, "ex$i", i, null, 3, 8, null), exercise("Ex$i", id = "ex$i"))
        },
    )

    @Test
    fun rotatesToTheRoutineDoneLongestAgo() {
        val push = routine("Push", 0, lastDay = 3)
        val pull = routine("Pull", 1, lastDay = 1)
        val legs = routine("Legs", 2, lastDay = 2)
        assertEquals("Pull", NextRoutine.pick(listOf(push, pull, legs))?.routine?.name)
    }

    @Test
    fun neverDoneRoutinesComeFirstInUserOrder() {
        val push = routine("Push", 0, lastDay = 1)
        val pull = routine("Pull", 1, lastDay = null)
        val legs = routine("Legs", 2, lastDay = null)
        assertEquals("Pull", NextRoutine.pick(listOf(push, legs, pull))?.routine?.name)
    }

    @Test
    fun emptyRoutinesAreIgnored() {
        assertNull(NextRoutine.pick(listOf(routine("Empty", 0, null, exercises = 0))))
        assertNull(NextRoutine.pick(emptyList()))
    }

    @Test
    fun targetsFromWhatWasDone() {
        val sets = listOf(
            DraftSet(40.0, 10, setType = SetType.WARMUP),
            DraftSet(80.0, 8),
            DraftSet(80.0, 8),
            DraftSet(77.5, 7),
            DraftSet(75.0, 8),
        )
        assertEquals(TargetsFromSets.Targets(sets = 4, reps = 8, weightKg = 80.0), TargetsFromSets.derive(sets))
        assertNull(TargetsFromSets.derive(listOf(DraftSet(40.0, 10, setType = SetType.WARMUP))))
    }
}
