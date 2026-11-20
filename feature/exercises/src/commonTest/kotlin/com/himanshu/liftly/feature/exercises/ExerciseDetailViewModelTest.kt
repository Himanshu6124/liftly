package com.himanshu.liftly.feature.exercises

import app.cash.turbine.test
import com.himanshu.liftly.core.domain.util.AppScope
import com.himanshu.liftly.feature.exercises.detail.DetailTab
import com.himanshu.liftly.feature.exercises.detail.ExerciseDetailIntent
import com.himanshu.liftly.feature.exercises.detail.ExerciseDetailViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeExerciseRepository(listOf(exercise("Bench Press", note = "seat 4")))
    private val urls = FakeUrlLauncher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = ExerciseDetailViewModel("bench_press", repo, AppScope(CoroutineScope(dispatcher)), urls)

    @Test
    fun loadsExerciseAndStoredNote() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        val state = vm.state.value
        assertEquals("Bench Press", state.exercise?.name)
        assertEquals("seat 4", state.noteDraft)
        assertEquals(DetailTab.FORM, state.tab)
    }

    @Test
    fun noteIsSavedOnceAfterTypingStops() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onIntent(ExerciseDetailIntent.NoteChanged("seat 5"))
        advanceTimeBy(200)
        vm.onIntent(ExerciseDetailIntent.NoteChanged("seat 5, pin 3"))
        advanceUntilIdle()
        assertEquals(listOf<String?>("seat 5, pin 3"), repo.noteWrites)
    }

    @Test
    fun loadingTheNoteDoesNotWriteIt() = runTest(dispatcher) {
        vm()
        advanceUntilIdle()
        assertTrue(repo.noteWrites.isEmpty())
    }

    @Test
    fun favoriteToggles() = runTest(dispatcher) {
        val vm = vm()
        vm.state.test {
            advanceUntilIdle()
            vm.onIntent(ExerciseDetailIntent.ToggleFavorite)
            advanceUntilIdle()
            assertEquals(true, expectMostRecentItem().exercise?.isFavorite)
        }
    }

    @Test
    fun unknownExerciseIsNotFound() = runTest(dispatcher) {
        val vm = ExerciseDetailViewModel("nope", repo, AppScope(CoroutineScope(dispatcher)), urls)
        advanceUntilIdle()
        assertTrue(vm.state.value.notFound)
    }
}
