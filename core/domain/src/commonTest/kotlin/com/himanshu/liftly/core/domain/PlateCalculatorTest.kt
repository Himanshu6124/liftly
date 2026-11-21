package com.himanshu.liftly.core.domain

import com.himanshu.liftly.core.domain.util.PlateCalculator
import com.himanshu.liftly.core.domain.util.WarmupGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlateCalculatorTest {
    private val kgPlates = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)

    @Test
    fun loadsHeaviestFirst() {
        val l = PlateCalculator.loadout(102.5, 20.0, kgPlates)
        assertEquals(listOf(25.0, 15.0, 1.25), l.perSide)
        assertEquals(102.5, l.total)
        assertTrue(l.exact)
    }

    @Test
    fun findsExactLoadWhereGreedyFails() {
        // 20 per side with only 15s and 10s: greedy takes 15 and gets stuck; exact search finds 10 + 10.
        val l = PlateCalculator.loadout(60.0, 20.0, listOf(15.0, 10.0))
        assertEquals(listOf(10.0, 10.0), l.perSide)
        assertTrue(l.exact)
    }

    @Test
    fun unreachableTargetGivesClosestBelow() {
        val l = PlateCalculator.loadout(101.0, 20.0, kgPlates)
        assertEquals(100.0, l.total)
        assertFalse(l.exact)
    }

    @Test
    fun belowOrAtTheBar() {
        assertEquals(PlateCalculator.Loadout(emptyList(), 20.0, exact = true), PlateCalculator.loadout(20.0, 20.0, kgPlates))
        assertFalse(PlateCalculator.loadout(15.0, 20.0, kgPlates).exact)
        assertEquals(20.0, PlateCalculator.nearestLoadable(12.0, 20.0, kgPlates))
    }

    @Test
    fun poundPlates() {
        val l = PlateCalculator.loadout(225.0, 45.0, listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5))
        assertEquals(listOf(45.0, 45.0), l.perSide)
    }

    @Test
    fun nearestLoadablePicksTheCloserSide() {
        val plates = listOf(20.0, 10.0, 5.0) // 10 kg steps on the bar
        assertEquals(60.0, PlateCalculator.nearestLoadable(57.0, 20.0, plates))
        assertEquals(50.0, PlateCalculator.nearestLoadable(54.0, 20.0, plates))
        assertEquals(50.0, PlateCalculator.nearestLoadable(55.0, 20.0, plates), "ties go lower")
    }

    @Test
    fun warmupsSnapToPlatesForABarbell() {
        val sets = WarmupGenerator.generate(100.0, 2.5, WarmupGenerator.Barbell(20.0, kgPlates))
        assertEquals(listOf(40.0 to 10, 60.0 to 5, 80.0 to 3), sets)
    }

    @Test
    fun warmupsUseTheIncrementWithoutABarAndDropDuplicates() {
        assertEquals(listOf(12.5 to 10, 17.5 to 5, 25.0 to 3), WarmupGenerator.generate(30.0, 2.5))
        // Light working weight: 40 % and 60 % both round to the empty bar → only one bar set.
        val light = WarmupGenerator.generate(35.0, 2.5, WarmupGenerator.Barbell(20.0, kgPlates))
        assertEquals(listOf(20.0 to 10, 27.5 to 3), light)
    }
}
