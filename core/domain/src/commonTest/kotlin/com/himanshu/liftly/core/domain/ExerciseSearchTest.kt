package com.himanshu.liftly.core.domain

import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.Muscle
import com.himanshu.liftly.core.domain.model.MuscleGroup
import com.himanshu.liftly.core.domain.search.ExerciseSearch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExerciseSearchTest {
    private val library = listOf(
        exercise("Bench Press", listOf("Barbell Bench Press", "BB Bench")),
        exercise("Dumbbell Bench Press", listOf("DB Bench"), equipment = Equipment.DUMBBELL),
        exercise("Incline Dumbbell Press", listOf("Incline DB Press"), equipment = Equipment.DUMBBELL),
        exercise("Overhead Press", listOf("OHP", "Military Press"), primary = Muscle.FRONT_DELTS),
        exercise("Romanian Deadlift", listOf("RDL"), primary = Muscle.HAMSTRINGS),
        exercise("Deadlift", primary = Muscle.LOWER_BACK),
        exercise("Cable Crunch", primary = Muscle.ABS, equipment = Equipment.CABLE),
        exercise("Lateral Raise", primary = Muscle.SIDE_DELTS, equipment = Equipment.DUMBBELL),
    )

    private fun search(q: String) = ExerciseSearch.filter(library, q).map { it.name }

    @Test
    fun abbreviationExpandsAndRanksExactExerciseFirst() {
        assertEquals("Dumbbell Bench Press", search("db bench").first())
    }

    @Test
    fun wordOrderDoesNotMatter() {
        assertEquals("Dumbbell Bench Press", search("bench dumbbell").first())
    }

    @Test
    fun aliasesMatch() {
        assertEquals(listOf("Overhead Press"), search("ohp"))
        assertEquals("Romanian Deadlift", search("rdl").first())
    }

    @Test
    fun exactNameBeatsLongerName() {
        assertEquals(listOf("Deadlift", "Romanian Deadlift"), search("deadlift"))
    }

    @Test
    fun caseAndPunctuationInsensitive() {
        assertEquals("Bench Press", search("  BENCH-press ").first())
    }

    @Test
    fun toleratesOneTypoAndPlurals() {
        assertTrue("Lateral Raise" in search("laterl raises"))
        assertTrue("Cable Crunch" in search("crunches"))
    }

    @Test
    fun everyTokenMustMatch() {
        assertEquals(emptyList(), search("bench squat"))
    }

    @Test
    fun emptyQueryReturnsAllAlphabetically() {
        assertEquals(library.map { it.name }.sortedBy { it.lowercase() }, search(""))
    }

    @Test
    fun filtersByMuscleGroupAndEquipment() {
        val shoulders = ExerciseSearch.filter(library, "", muscleGroup = MuscleGroup.SHOULDERS).map { it.name }
        assertEquals(listOf("Lateral Raise", "Overhead Press"), shoulders)
        val dbPress = ExerciseSearch.filter(library, "press", equipment = Equipment.DUMBBELL).map { it.name }
        assertEquals(setOf("Dumbbell Bench Press", "Incline Dumbbell Press"), dbPress.toSet())
    }

    @Test
    fun withinOneEdit() {
        assertTrue(ExerciseSearch.withinOneEdit("press", "pres"))
        assertTrue(ExerciseSearch.withinOneEdit("curl", "cirl"))
        assertTrue(!ExerciseSearch.withinOneEdit("curl", "cart"))
    }
}
