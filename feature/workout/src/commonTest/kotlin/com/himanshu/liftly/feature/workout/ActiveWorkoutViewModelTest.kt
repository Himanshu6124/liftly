package com.himanshu.liftly.feature.workout

import app.cash.turbine.test
import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.core.domain.usecase.GetLogSheetPrefillUseCase
import com.himanshu.liftly.core.domain.usecase.LogExerciseSetsUseCase
import com.himanshu.liftly.core.domain.util.AppScope
import com.himanshu.liftly.feature.workout.active.ActiveWorkoutEffect
import com.himanshu.liftly.feature.workout.active.ActiveWorkoutIntent
import com.himanshu.liftly.feature.workout.active.ActiveWorkoutViewModel
import com.himanshu.liftly.feature.workout.active.formatElapsed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val haptics = CountingHaptics()

    private val routines = FakeRoutines()

    private fun vm(repo: FakeWorkoutRepository, settings: FakeSettings = FakeSettings()) = ActiveWorkoutViewModel(
        repo,
        routines,
        settings,
        LogExerciseSetsUseCase(repo),
        GetLogSheetPrefillUseCase(),
        haptics,
        AppScope(CoroutineScope(dispatcher)),
    )

    @Test
    fun loadsCardsFromTheActiveWorkout() = runTest(dispatcher) {
        val vm = vm(FakeWorkoutRepository(listOf("Bench", "Row")))
        assertEquals(listOf("Bench", "Row"), vm.state.value.cards.map { it.name })
        assertEquals(2, vm.state.value.pendingCount)
    }

    @Test
    fun skipShowsUndoAndUndoRestoresStatus() = runTest(dispatcher) {
        val repo = FakeWorkoutRepository(listOf("Bench"))
        val vm = vm(repo)
        vm.effects.test {
            vm.onIntent(ActiveWorkoutIntent.Skip("we0"))
            assertEquals(ActiveWorkoutEffect.Undoable(ActiveWorkoutEffect.Undoable.Kind.SKIPPED, "Bench"), awaitItem())
            assertEquals(ExerciseStatus.SKIPPED, repo.status("we0"))
            vm.onIntent(ActiveWorkoutIntent.Undo)
            assertEquals(ExerciseStatus.PENDING, repo.status("we0"))
        }
    }

    @Test
    fun removeShowsUndoAndUndoPutsItBackInPlace() = runTest(dispatcher) {
        val repo = FakeWorkoutRepository(listOf("A", "B", "C"))
        val vm = vm(repo)
        vm.effects.test {
            vm.onIntent(ActiveWorkoutIntent.Remove("we1"))
            awaitItem()
            assertEquals(listOf("A", "C"), repo.names())
            vm.onIntent(ActiveWorkoutIntent.Undo)
            assertEquals(listOf("A", "B", "C"), repo.names())
        }
    }

    @Test
    fun moveDownReorders() = runTest(dispatcher) {
        val repo = FakeWorkoutRepository(listOf("A", "B", "C"))
        vm(repo).onIntent(ActiveWorkoutIntent.Move("we0", 1))
        assertEquals(listOf("B", "A", "C"), repo.names())
    }

    @Test
    fun finishWithPendingAsksFirst() = runTest(dispatcher) {
        val repo = FakeWorkoutRepository(listOf("A"))
        val vm = vm(repo)
        vm.onIntent(ActiveWorkoutIntent.FinishRequested)
        assertTrue(vm.state.value.showFinishSheet)
        assertFalse(repo.finished)

        vm.effects.test {
            vm.onIntent(ActiveWorkoutIntent.FinishConfirmed)
            assertEquals(ActiveWorkoutEffect.Closed(finished = true), awaitItem())
            assertTrue(repo.finished)
        }
    }

    @Test
    fun finishWithEverythingDoneSkipsTheSheet() = runTest(dispatcher) {
        val repo = FakeWorkoutRepository(listOf("A"))
        repo.setStatus("we0", ExerciseStatus.LOGGED)
        val vm = vm(repo)
        vm.effects.test {
            vm.onIntent(ActiveWorkoutIntent.FinishRequested)
            assertEquals(ActiveWorkoutEffect.Closed(finished = true), awaitItem())
        }
    }

    @Test
    fun finishingAnEmptyWorkoutDiscardsIt() = runTest(dispatcher) {
        val repo = FakeWorkoutRepository(emptyList())
        val vm = vm(repo)
        vm.effects.test {
            vm.onIntent(ActiveWorkoutIntent.FinishRequested)
            assertEquals(ActiveWorkoutEffect.Closed(finished = false), awaitItem())
            assertTrue(repo.discarded)
            assertFalse(repo.finished)
        }
    }

    @Test
    fun discardNeedsConfirmation() = runTest(dispatcher) {
        val repo = FakeWorkoutRepository(listOf("A"))
        val vm = vm(repo)
        vm.onIntent(ActiveWorkoutIntent.DiscardRequested)
        assertTrue(vm.state.value.showDiscardDialog)
        assertFalse(repo.discarded)
        vm.onIntent(ActiveWorkoutIntent.DiscardConfirmed)
        assertTrue(repo.discarded)
        assertTrue(vm.state.value.noWorkout)
    }

    @Test
    fun cardsShowLastSessionHintAndLoggedSummary() = runTest(dispatcher) {
        val repo = FakeWorkoutRepository(listOf("Bench"), lastSessions = mapOf("Bench" to sets(77.5 to 8, 77.5 to 7)))
        val vm = vm(repo)
        assertEquals("77.5 kg × 8, 7", vm.state.value.cards.single().lastHint)

        vm.onIntent(ActiveWorkoutIntent.QuickLog("we0"))
        assertEquals("2 × 77.5 kg · 8, 7", vm.state.value.cards.single().summary)
    }

    @Test
    fun quickLogLogsThePrefillWithUndoAndHaptic() = runTest(dispatcher) {
        val repo = FakeWorkoutRepository(listOf("Bench", "Row"), lastSessions = mapOf("Bench" to sets(80.0 to 8, 80.0 to 8)))
        val vm = vm(repo)
        vm.effects.test {
            vm.onIntent(ActiveWorkoutIntent.QuickLog("we0"))
            assertEquals(ActiveWorkoutEffect.Undoable(ActiveWorkoutEffect.Undoable.Kind.LOGGED, "Bench"), awaitItem())
            assertEquals(ActiveWorkoutEffect.ScrollTo("we1"), awaitItem())
            assertEquals(ExerciseStatus.LOGGED, repo.status("we0"))
            assertEquals(listOf(80.0 to 8, 80.0 to 8), repo.logged.single().second.map { it.weightKg to it.reps })
            assertEquals(1, haptics.light)
            assertEquals("we1", vm.state.value.highlightedId, "next pending card is highlighted")

            vm.onIntent(ActiveWorkoutIntent.Undo)
            assertEquals(ExerciseStatus.PENDING, repo.status("we0"))
        }
    }

    @Test
    fun quickLogWithoutHistoryOpensTheSheetInstead() = runTest(dispatcher) {
        val repo = FakeWorkoutRepository(listOf("Bench"))
        val vm = vm(repo)
        vm.onIntent(ActiveWorkoutIntent.QuickLog("we0"))
        assertEquals("we0", vm.state.value.logSheetFor)
        assertTrue(repo.logged.isEmpty())
    }

    @Test
    fun savingFromTheSheetOpensTheNextOneWhenEnabled() = runTest(dispatcher) {
        val repo = FakeWorkoutRepository(listOf("A", "B", "C"))
        val vm = vm(repo, FakeSettings(com.himanshu.liftly.core.domain.model.UserSettings(openNextExerciseAutomatically = true)))
        vm.onIntent(ActiveWorkoutIntent.OpenLogSheet("we1"))
        val undo = repo.logSets(
            "we1",
            listOf(com.himanshu.liftly.core.domain.model.DraftSet(50.0, 10)),
            com.himanshu.liftly.core.domain.model.InputMode.UNIFORM,
        )
        vm.onIntent(ActiveWorkoutIntent.Logged("we1", undo))
        assertEquals("we2", vm.state.value.logSheetFor)
    }

    @Test
    fun advanceWrapsAroundToEarlierPendingCards() = runTest(dispatcher) {
        val repo = FakeWorkoutRepository(listOf("A", "B"))
        val vm = vm(repo, FakeSettings(com.himanshu.liftly.core.domain.model.UserSettings(openNextExerciseAutomatically = true)))
        val undo = repo.logSets(
            "we1",
            listOf(com.himanshu.liftly.core.domain.model.DraftSet(50.0, 10)),
            com.himanshu.liftly.core.domain.model.InputMode.UNIFORM,
        )
        vm.onIntent(ActiveWorkoutIntent.Logged("we1", undo))
        assertEquals("we0", vm.state.value.logSheetFor)
    }

    @Test
    fun saveAsRoutineAndUpdateTargets() = runTest(dispatcher) {
        val repo = FakeWorkoutRepository(listOf("Bench"))
        val vm = vm(repo)
        vm.effects.test {
            vm.onIntent(ActiveWorkoutIntent.SaveAsRoutineRequested)
            assertTrue(vm.state.value.showSaveRoutineDialog)
            vm.onIntent(ActiveWorkoutIntent.SaveAsRoutine(" Push "))
            assertEquals(ActiveWorkoutEffect.Info(ActiveWorkoutEffect.Info.Kind.ROUTINE_SAVED, "Push"), awaitItem())
            assertEquals(listOf("w1" to " Push "), routines.savedFromWorkout)
            assertFalse(vm.state.value.showSaveRoutineDialog)
        }
    }

    @Test
    fun elapsedFormatting() {
        assertEquals("00:07", formatElapsed(7.seconds))
        assertEquals("34:12", formatElapsed(34.minutes + 12.seconds))
        assertEquals("1:05:00", formatElapsed(65.minutes))
    }
}
