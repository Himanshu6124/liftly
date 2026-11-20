package com.himanshu.liftly.core.domain

import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.util.Units
import kotlin.test.Test
import kotlin.test.assertEquals

class UnitsTest {
    @Test
    fun kgRoundTripsThroughPounds() {
        val lb = Units.kgToDisplay(100.0, WeightUnit.LB)
        assertEquals(220.462, lb, 0.001)
        assertEquals(100.0, Units.displayToKg(lb, WeightUnit.LB), 1e-9)
    }

    @Test
    fun roundsToIncrement() {
        assertEquals(80.0, Units.roundToIncrement(78.9, 2.5))
        assertEquals(77.5, Units.roundToIncrement(77.4, 2.5))
        assertEquals(225.0, Units.roundToIncrement(223.0, 5.0))
        assertEquals(42.0, Units.roundToIncrement(42.0, 0.0))
    }
}
