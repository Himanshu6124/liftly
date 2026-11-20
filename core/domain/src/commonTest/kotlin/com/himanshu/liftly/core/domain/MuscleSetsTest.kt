package com.himanshu.liftly.core.domain

import com.himanshu.liftly.core.domain.model.ExerciseSetCount
import com.himanshu.liftly.core.domain.model.Muscle
import com.himanshu.liftly.core.domain.util.MuscleSets
import kotlin.test.Test
import kotlin.test.assertEquals

class MuscleSetsTest {
    @Test
    fun primaryCountsOneSecondaryHalf() {
        val result = MuscleSets.perMuscle(
            listOf(
                ExerciseSetCount(Muscle.CHEST, listOf(Muscle.TRICEPS, Muscle.FRONT_DELTS), 4), // bench
                ExerciseSetCount(Muscle.TRICEPS, emptyList(), 3), // pushdown
                ExerciseSetCount(Muscle.FRONT_DELTS, listOf(Muscle.TRICEPS, Muscle.TRICEPS), 2), // duplicate secondary
            ),
        )
        assertEquals(4.0, result[Muscle.CHEST])
        assertEquals(3.0 + 2.0 + 1.0, result[Muscle.TRICEPS])
        assertEquals(2.0 + 2.0, result[Muscle.FRONT_DELTS])
        assertEquals(null, result[Muscle.QUADS])
    }
}
