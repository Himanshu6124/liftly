package com.himanshu.liftly.core.domain

import com.himanshu.liftly.core.domain.model.MeasureKind
import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.util.Measurements
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MeasurementsTest {
    @Test
    fun lengthsFollowTheWeightUnit() {
        assertEquals(84.0, Measurements.toDisplay(84.0, MeasureKind.LENGTH, WeightUnit.KG))
        assertEquals(33.07, Measurements.toDisplay(84.0, MeasureKind.LENGTH, WeightUnit.LB), 0.01)
        assertEquals(
            84.0,
            Measurements.fromDisplay(Measurements.toDisplay(84.0, MeasureKind.LENGTH, WeightUnit.LB), MeasureKind.LENGTH, WeightUnit.LB),
            1e-9,
        )
        assertEquals("in", Measurements.symbol(MeasureKind.LENGTH, WeightUnit.LB))
        assertEquals(18.5, Measurements.toDisplay(18.5, MeasureKind.PERCENT, WeightUnit.LB), "percent never converts")
    }

    @Test
    fun formatsWithOneDecimalAndRejectsTypos() {
        assertEquals("80.4", Measurements.format(80.44))
        assertEquals("80", Measurements.format(80.0))
        assertFalse(Measurements.isPlausible(800.0, MeasureKind.PERCENT))
    }
}
