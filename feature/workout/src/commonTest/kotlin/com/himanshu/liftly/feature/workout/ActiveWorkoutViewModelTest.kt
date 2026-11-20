package com.himanshu.liftly.feature.workout

import app.cash.turbine.test
import com.himanshu.liftly.core.domain.model.ExerciseStatus
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

    private fun vm(repo: FakeWorkoutRepository) = ActiveWorkoutViewModel(repo, AppScope(CoroutineScope(dispatcher)))

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
    fun elapsedFormatting() {
        assertEquals("00:07", formatElapsed(7.seconds))
        assertEquals("34:12", formatElapsed(34.minutes + 12.seconds))
        assertEquals("1:05:00", formatElapsed(65.minutes))
    }
}
