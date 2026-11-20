package com.himanshu.liftly.feature.workout

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.himanshu.liftly.core.domain.model.SetType
import com.himanshu.liftly.core.domain.model.TrackingType
import com.himanshu.liftly.core.domain.model.UserSettings
import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.usecase.GetLogSheetPrefillUseCase
import com.himanshu.liftly.core.domain.usecase.LogExerciseSetsUseCase
import com.himanshu.liftly.core.domain.usecase.ParseRepInputUseCase
import com.himanshu.liftly.feature.workout.logsheet.LogSheetError
import com.himanshu.liftly.feature.workout.logsheet.LogSheetState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LogSheetStateTest {
    private val haptics = CountingHaptics()

    private suspend fun TestScope.holder(
        repo: FakeWorkoutRepository,
        settings: UserSettings = UserSettings(),
    ) = LogSheetState(
        "we0",
        repo,
        FakeSettings(settings),
        GetLogSheetPrefillUseCase(),
        ParseRepInputUseCase(),
        LogExerciseSetsUseCase(repo),
        haptics,
        backgroundScope,
    ).also { it.load() }

    private val benchWithHistory get() = FakeWorkoutRepository(
        listOf("Bench"),
        mapOf("Bench" to sets(77.5 to 8, 77.5 to 8, 77.5 to 8, 77.5 to 7)),
    )

    @Test
    fun opensPrefilledFromLastSession() = runTest(UnconfinedTestDispatcher()) {
        val s = holder(benchWithHistory).state
        assertFalse(s.isLoading)
        assertEquals("77.5", s.weightText)
        assertEquals(4, s.setCount)
        assertEquals("8 8 8 7", s.repsText)
        assertEquals("77.5 kg × 8, 8, 8, 7", s.lastHint)
        assertTrue(s.canSave)
    }

    @Test
    fun firstTimeOpensWithDefaultSetCountAndEmptyFields() = runTest(UnconfinedTestDispatcher()) {
        val s = holder(FakeWorkoutRepository(listOf("Bench")), UserSettings(defaultSetCount = 3)).state
        assertEquals(3, s.setCount)
        assertEquals("", s.repsText)
        assertEquals("", s.weightText)
        assertFalse(s.canSave, "nothing to save yet")
    }

    @Test
    fun changingSetCountPadsWithLastValueOrTrims() = runTest(UnconfinedTestDispatcher()) {
        val h = holder(benchWithHistory)
        h.changeSetCount(+1)
        assertEquals("8 8 8 7 7", h.state.repsText)
        h.changeSetCount(-2)
        assertEquals("8 8 8", h.state.repsText)
        assertEquals(3, h.state.setCount)
    }

    @Test
    fun typingRepsUpdatesSetCount() = runTest(UnconfinedTestDispatcher()) {
        val h = holder(benchWithHistory)
        h.onRepsChanged("10x5")
        assertEquals(5, h.state.setCount)
        h.onRepsChanged("12 10")
        assertEquals(2, h.state.setCount)
    }

    @Test
    fun nudgeUsesTheIncrementAndRoundsToIt() = runTest(UnconfinedTestDispatcher()) {
        val h = holder(benchWithHistory)
        h.nudgeWeight(+1)
        assertEquals("80", h.state.weightText)
        h.onWeightChanged("81")
        h.nudgeWeight(-1)
        assertEquals("77.5", h.state.weightText)
    }

    @Test
    fun invalidInputShowsErrorAndBlocksSave() = runTest(UnconfinedTestDispatcher()) {
        val h = holder(benchWithHistory)
        h.onRepsChanged("8 abc")
        assertEquals(LogSheetError.REPS_INVALID, h.state.repsError)
        assertFalse(h.state.canSave)
        h.onRepsChanged("8 8")
        h.onWeightChanged("2000")
        assertEquals(LogSheetError.WEIGHT_RANGE, h.state.weightError)
        assertFalse(h.state.canSave)
        h.onWeightChanged("80,5")
        assertNull(h.state.weightError, "comma decimals are fine")
    }

    @Test
    fun saveWritesKilogramsEvenInPounds() = runTest(UnconfinedTestDispatcher()) {
        val repo = FakeWorkoutRepository(listOf("Bench"))
        val h = holder(repo, UserSettings(weightUnit = WeightUnit.LB))
        h.onWeightChanged("225")
        h.onRepsChanged("5x3")
        var summary: String? = null
        h.save { s, _, _ -> summary = s }

        val saved = repo.logged.single().second
        assertEquals(3, saved.size)
        assertEquals(102.058, saved.first().weightKg ?: 0.0, 0.001)
        assertEquals("3 × 225 lb · 5, 5, 5", summary)
        assertEquals(1, haptics.light)
    }

    @Test
    fun repsOnlyExercisesHaveNoWeight() = runTest(UnconfinedTestDispatcher()) {
        val repo = FakeWorkoutRepository(listOf("Pull-Up"), trackingType = TrackingType.REPS_ONLY)
        val h = holder(repo)
        assertFalse(h.state.showsWeight)
        h.onRepsChanged("10 8 6")
        h.save { _, _, _ -> }
        assertEquals(listOf(null, null, null), repo.logged.single().second.map { it.weightKg })
    }

    @Test
    fun perSetModeRoundTripsAndSavesSetTypes() = runTest(UnconfinedTestDispatcher()) {
        val repo = benchWithHistory
        val h = holder(repo)
        h.setPerSet(true)
        assertEquals(4, h.rows.size)
        assertEquals("77.5", h.rows.first().weight.text.toString())
        h.rows.last().weight.setTextAndPlaceCursorAtEnd("60")
        h.cycleType(h.rows.last()) // Normal → Drop
        h.addRow()
        assertEquals(5, h.state.setCount)

        h.save { _, _, _ -> }
        val saved = repo.logged.single().second
        assertEquals(listOf(77.5, 77.5, 77.5, 60.0, 60.0), saved.map { it.weightKg })
        assertEquals(SetType.DROP, saved[3].setType)

        h.setPerSet(false)
        assertEquals("77.5", h.state.weightText, "back to one weight: the heaviest")
    }

    @Test
    fun warmupsAreSavedFirstAndMarked() = runTest(UnconfinedTestDispatcher()) {
        val repo = benchWithHistory
        val h = holder(repo)
        h.addWarmup()
        h.addWarmup()
        assertEquals("30", h.warmups.first().weight.text.toString(), "40% of 77.5 rounded to 2.5")
        h.save { _, _, _ -> }
        val saved = repo.logged.single().second
        assertEquals(listOf(SetType.WARMUP, SetType.WARMUP), saved.take(2).map { it.setType })
        assertEquals(6, saved.size)
    }

    @Test
    fun rpeAndNoteAreSaved() = runTest(UnconfinedTestDispatcher()) {
        val repo = benchWithHistory
        val h = holder(repo)
        h.rpeField.setTextAndPlaceCursorAtEnd("8")
        h.noteField.setTextAndPlaceCursorAtEnd("belt on")
        h.save { _, _, _ -> }
        assertEquals(listOf(8.0, 8.0, 8.0, 8.0), repo.logged.single().second.map { it.rpe })
        assertEquals("belt on", repo.lastNotes)

        h.rpeField.setTextAndPlaceCursorAtEnd("11")
        assertEquals(LogSheetError.RPE_INVALID, h.state.otherError)
        assertFalse(h.state.canSave)
    }

    @Test
    fun timeExercisesLogDurations() = runTest(UnconfinedTestDispatcher()) {
        val repo = FakeWorkoutRepository(listOf("Plank"), trackingType = TrackingType.TIME)
        val h = holder(repo)
        h.durationField.setTextAndPlaceCursorAtEnd("1:00x3")
        assertEquals(3, h.state.setCount)
        h.changeSetCount(+1)
        assertEquals("1:00 1:00 1:00 1:00", h.durationField.text.toString())
        h.save { _, _, _ -> }
        assertEquals(listOf(60, 60, 60, 60), repo.logged.single().second.map { it.durationSec })
    }

    @Test
    fun distanceExercisesLogMetresAndTime() = runTest(UnconfinedTestDispatcher()) {
        val repo = FakeWorkoutRepository(listOf("Run"), trackingType = TrackingType.DISTANCE)
        val h = holder(repo)
        h.distanceField.setTextAndPlaceCursorAtEnd("5000")
        h.distanceTimeField.setTextAndPlaceCursorAtEnd("25:00")
        var summary: String? = null
        h.save { s, _, _ -> summary = s }
        assertEquals(5000.0 to 1500, repo.logged.single().second.single().let { it.distanceM to it.durationSec })
        assertEquals("5000 m · 25:00", summary)
    }

    @Test
    fun beatingLastSessionReportsRecordsAndSuccessHaptic() = runTest(UnconfinedTestDispatcher()) {
        val repo = benchWithHistory
        val h = holder(repo)
        h.onWeightChanged("82.5")
        var records = emptyList<com.himanshu.liftly.core.domain.usecase.PrHit>()
        h.save { _, r, _ -> records = r }
        assertTrue(records.any { it.type == com.himanshu.liftly.core.domain.model.PrType.MAX_WEIGHT })
    }
}
