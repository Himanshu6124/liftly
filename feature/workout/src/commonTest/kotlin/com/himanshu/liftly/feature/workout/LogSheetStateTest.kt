package com.himanshu.liftly.feature.workout

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

    private fun TestScope.holder(
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
    )

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
        h.save { s, _ -> summary = s }

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
        h.save { _, _ -> }
        assertEquals(listOf(null, null, null), repo.logged.single().second.map { it.weightKg })
    }
}
