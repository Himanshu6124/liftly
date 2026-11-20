package com.himanshu.liftly.feature.exercises

import app.cash.turbine.test
import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.Muscle
import com.himanshu.liftly.core.domain.model.NewCustomExercise
import com.himanshu.liftly.core.domain.model.TrackingType
import com.himanshu.liftly.feature.exercises.picker.ExercisePickerIntent
import com.himanshu.liftly.feature.exercises.picker.ExercisePickerViewModel
import com.himanshu.liftly.feature.exercises.picker.PickerSection
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ExercisePickerViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val exercises = FakeExerciseRepository(
        listOf(
            exercise("Bench Press", primary = Muscle.CHEST),
            exercise("Cable Fly", primary = Muscle.CHEST, equipment = Equipment.CABLE),
            exercise("Squat", primary = Muscle.QUADS, favorite = true),
            exercise("Barbell Row", primary = Muscle.UPPER_BACK),
        ),
    )
    private val workouts = FakeRecentWorkoutRepository(recent = listOf("barbell_row"))

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(context: List<String> = listOf("bench_press")) =
        ExercisePickerViewModel(context, exercises, workouts, dispatcher)

    @Test
    fun browsingShowsSuggestedRecentFavoritesThenAll() = runTest(dispatcher) {
        vm().state.test {
            val sections = expectMostRecentItem().sections
            assertEquals(
                listOf(PickerSection.Kind.SUGGESTED, PickerSection.Kind.RECENT, PickerSection.Kind.FAVORITES),
                sections.take(3).map { it.kind },
            )
            assertEquals(listOf("Cable Fly"), sections[0].rows.map { it.name }, "same muscle, not already added")
            assertEquals(listOf("Barbell Row"), sections[1].rows.map { it.name })
            assertEquals(listOf("Squat"), sections[2].rows.map { it.name })
            assertTrue(sections.drop(3).all { it.kind == PickerSection.Kind.LETTER })
        }
    }

    @Test
    fun selectionKeepsTapOrderAndToggles() = runTest(dispatcher) {
        val vm = vm()
        vm.state.test {
            vm.onIntent(ExercisePickerIntent.SelectionToggled("squat"))
            vm.onIntent(ExercisePickerIntent.SelectionToggled("bench_press"))
            vm.onIntent(ExercisePickerIntent.SelectionToggled("cable_fly"))
            vm.onIntent(ExercisePickerIntent.SelectionToggled("bench_press"))
            assertEquals(listOf("squat", "cable_fly"), expectMostRecentItem().selected)
        }
    }

    @Test
    fun searchWithNoMatchFlagsNoResults() = runTest(dispatcher) {
        val vm = vm()
        vm.state.test {
            vm.onIntent(ExercisePickerIntent.QueryChanged("zercher"))
            val state = expectMostRecentItem()
            assertTrue(state.noResults)
            assertTrue(state.sections.isEmpty())
        }
    }

    @Test
    fun creatingCustomExerciseSelectsItAndClearsSearch() = runTest(dispatcher) {
        val vm = vm()
        vm.state.test {
            vm.onIntent(ExercisePickerIntent.QueryChanged("Landmine Press"))
            vm.onIntent(
                ExercisePickerIntent.CreateCustom(
                    NewCustomExercise("Landmine Press", Muscle.FRONT_DELTS, Equipment.BARBELL, TrackingType.WEIGHT_REPS),
                ),
            )
            val state = expectMostRecentItem()
            assertEquals(listOf("custom-4"), state.selected)
            assertEquals("", vm.query.value)
        }
    }
}
