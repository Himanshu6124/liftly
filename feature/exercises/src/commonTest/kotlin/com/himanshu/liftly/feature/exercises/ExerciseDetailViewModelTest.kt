package com.himanshu.liftly.feature.exercises

import app.cash.turbine.test
import com.himanshu.liftly.core.domain.model.ExerciseSession
import com.himanshu.liftly.core.domain.model.SetType
import com.himanshu.liftly.core.domain.model.WorkoutSet
import com.himanshu.liftly.core.domain.util.AppScope
import com.himanshu.liftly.core.domain.util.ProgressMetric
import com.himanshu.liftly.core.domain.util.ProgressRange
import com.himanshu.liftly.core.domain.util.TimeProvider
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
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeExerciseRepository(listOf(exercise("Bench Press", note = "seat 4")))
    private val urls = FakeUrlLauncher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val now = Instant.fromEpochMilliseconds(1_800_000_000_000)
    private val workouts = FakeRecentWorkoutRepository()

    private fun session(daysAgo: Int, weight: Double, reps: Int) = ExerciseSession(
        "w$daysAgo",
        "Push",
        now - daysAgo.days,
        listOf(WorkoutSet("s$daysAgo", "we$daysAgo", 0, weight, reps, null, null, null, SetType.NORMAL)),
    )

    @Test
    fun historyTabChartsSessionsAndFollowsMetricAndRange() = runTest(dispatcher) {
        workouts.sessions.value = listOf(session(2, 100.0, 5), session(60, 90.0, 5), session(400, 80.0, 5))
        val vm = vm()
        advanceUntilIdle()
        with(vm.state.value) {
            assertEquals(3, sessions.size)
            assertEquals(ProgressMetric.EST_1RM, progress.metric)
            assertEquals(listOf(80.0, 90.0, 100.0).map { it * (1 + 5 / 30.0) }, progress.points.map { it.value })
            assertEquals(2, progress.selected, "latest session selected")
        }
        vm.onIntent(ExerciseDetailIntent.MetricSelected(ProgressMetric.BEST_WEIGHT))
        vm.onIntent(ExerciseDetailIntent.RangeSelected(ProgressRange.QUARTER))
        assertEquals(listOf(90.0, 100.0), vm.state.value.progress.points.map { it.value })
        vm.onIntent(ExerciseDetailIntent.PointSelected(0))
        assertEquals(0, vm.state.value.progress.selected)
    }

    private fun vm() = ExerciseDetailViewModel(
        "bench_press",
        repo,
        AppScope(CoroutineScope(dispatcher)),
        urls,
        workouts,
        FakeSettings(),
        TimeProvider { now },
    )

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
        val vm =
            ExerciseDetailViewModel(
                "nope",
                repo,
                AppScope(CoroutineScope(dispatcher)),
                urls,
                FakeRecentWorkoutRepository(),
                FakeSettings(),
                TimeProvider {
                    Instant.fromEpochMilliseconds(0)
                },
            )
        advanceUntilIdle()
        assertTrue(vm.state.value.notFound)
    }
}
