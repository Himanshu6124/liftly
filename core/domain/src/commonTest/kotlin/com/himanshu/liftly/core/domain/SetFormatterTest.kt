package com.himanshu.liftly.core.domain

import com.himanshu.liftly.core.domain.model.DraftSet
import com.himanshu.liftly.core.domain.model.SetType
import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.util.SetFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SetFormatterTest {
    private fun sets(vararg pairs: Pair<Double?, Int?>) = pairs.map { (w, r) -> DraftSet(w, r) }

    @Test
    fun sameWeightSummary() =
        assertEquals("4 × 80 kg · 8, 8, 7, 6", SetFormatter.summary(sets(80.0 to 8, 80.0 to 8, 80.0 to 7, 80.0 to 6), WeightUnit.KG))

    @Test
    fun varyingWeightSummary() =
        assertEquals("80/80/75 kg · 8, 8, 10", SetFormatter.summary(sets(80.0 to 8, 80.0 to 8, 75.0 to 10), WeightUnit.KG))

    @Test
    fun repsOnlySummary() = assertEquals("3 sets · 12, 10, 8", SetFormatter.summary(sets(null to 12, null to 10, null to 8), WeightUnit.KG))

    @Test
    fun warmupsAreLeftOut() {
        val withWarmup = listOf(DraftSet(40.0, 10, setType = SetType.WARMUP)) + sets(80.0 to 8, 80.0 to 8)
        assertEquals("2 × 80 kg · 8, 8", SetFormatter.summary(withWarmup, WeightUnit.KG))
    }

    @Test
    fun hintAndDecimals() {
        assertEquals("77.5 kg × 8, 8, 8, 7", SetFormatter.hint(sets(77.5 to 8, 77.5 to 8, 77.5 to 8, 77.5 to 7), WeightUnit.KG))
        assertNull(SetFormatter.hint(sets(null to null, null to null), WeightUnit.KG))
    }

    @Test
    fun poundsAreConvertedForDisplay() {
        assertEquals("225 lb", SetFormatter.weight(102.0582, WeightUnit.LB))
        assertEquals("100 kg", SetFormatter.weight(100.0, WeightUnit.KG))
    }
}
