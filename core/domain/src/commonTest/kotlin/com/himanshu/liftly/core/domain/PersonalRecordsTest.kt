package com.himanshu.liftly.core.domain

import com.himanshu.liftly.core.domain.model.DraftSet
import com.himanshu.liftly.core.domain.model.PrType
import com.himanshu.liftly.core.domain.model.SetType
import com.himanshu.liftly.core.domain.usecase.DetectPersonalRecordsUseCase
import com.himanshu.liftly.core.domain.usecase.ParseDurationInputUseCase
import com.himanshu.liftly.core.domain.usecase.SessionHistory
import com.himanshu.liftly.core.domain.util.Strength
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PersonalRecordsTest {
    private val detect = DetectPersonalRecordsUseCase()
    private fun s(w: Double?, r: Int?, type: SetType = SetType.NORMAL) = DraftSet(w, r, setType = type)
    private val lastWeek = SessionHistory(listOf(listOf(s(100.0, 5), s(100.0, 5), s(100.0, 4))))

    @Test
    fun heavierWeightIsANewPr() {
        val hits = detect(lastWeek, listOf(s(105.0, 5), s(100.0, 5))) // e1RM 122.5 > 116.7
        assertEquals(setOf(PrType.MAX_WEIGHT, PrType.EST_1RM), hits.map { it.type }.toSet() - PrType.MAX_VOLUME)
        assertEquals(0, hits.first { it.type == PrType.MAX_WEIGHT }.setIndex)
        assertEquals(105.0, hits.first { it.type == PrType.MAX_WEIGHT }.value)
    }

    @Test
    fun aTieIsNotAPr() {
        assertTrue(detect(lastWeek, listOf(s(100.0, 5), s(100.0, 5), s(100.0, 4))).isEmpty())
    }

    @Test
    fun warmupsAreIgnored() {
        val hits = detect(lastWeek, listOf(s(140.0, 1, SetType.WARMUP), s(100.0, 5)))
        assertTrue(hits.none { it.type == PrType.MAX_WEIGHT || it.type == PrType.EST_1RM })
        val heavyWarmupHistory = SessionHistory(listOf(listOf(s(150.0, 1, SetType.WARMUP), s(90.0, 5))))
        assertTrue(detect(heavyWarmupHistory, listOf(s(100.0, 5))).any { it.type == PrType.MAX_WEIGHT })
    }

    @Test
    fun editingDownRemovesThePr() {
        // Detection always runs against history without the session being edited.
        assertTrue(detect(lastWeek, listOf(s(110.0, 5))).any { it.type == PrType.MAX_WEIGHT })
        assertTrue(detect(lastWeek, listOf(s(90.0, 5))).none { it.type == PrType.MAX_WEIGHT })
    }

    @Test
    fun firstEverSessionSetsNoPrs() = assertTrue(detect(SessionHistory(emptyList()), listOf(s(60.0, 10))).isEmpty())

    @Test
    fun volumeComparesWholeSessions() {
        val history = SessionHistory(listOf(listOf(s(100.0, 5), s(100.0, 5)), listOf(s(50.0, 10))))
        val hits = detect(history, listOf(s(100.0, 5), s(100.0, 5), s(60.0, 1)))
        assertEquals(1060.0, hits.single { it.type == PrType.MAX_VOLUME }.value)
    }

    @Test
    fun epleyAndVolume() {
        assertEquals(116.67, Strength.estimatedOneRepMax(100.0, 5) ?: 0.0, 0.01)
        assertEquals(100.0, Strength.estimatedOneRepMax(100.0, 1))
        assertNull(Strength.estimatedOneRepMax(100.0, 13), "only for reps ≤ 12")
        assertEquals(1000.0, Strength.volume(listOf(s(100.0, 5), s(100.0, 5), s(40.0, 10, SetType.WARMUP))))
    }

    @Test
    fun durationParsing() {
        val p = ParseDurationInputUseCase()
        assertEquals(ParseDurationInputUseCase.Result.Valid(listOf(60, 45, 45)), p("60 45 45"))
        assertEquals(ParseDurationInputUseCase.Result.Valid(listOf(90, 90, 90)), p("1:30x3"))
        assertEquals(ParseDurationInputUseCase.Result.Valid(listOf(90)), p("1.30"))
        assertEquals(ParseDurationInputUseCase.Result.Invalid, p("1:75"))
        assertEquals(ParseDurationInputUseCase.Result.Empty, p(" "))
        assertEquals("1:30 0:45", ParseDurationInputUseCase.formatAll(listOf(90, 45)))
    }
}
