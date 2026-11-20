package com.himanshu.liftly.feature.exercises

import app.cash.turbine.test
import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.Muscle
import com.himanshu.liftly.core.domain.model.MuscleGroup
import com.himanshu.liftly.core.domain.util.AppScope
import com.himanshu.liftly.feature.exercises.library.ExerciseLibraryIntent
import com.himanshu.liftly.feature.exercises.library.ExerciseLibraryViewModel
import com.himanshu.liftly.feature.exercises.library.LibrarySection
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

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseLibraryViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val repo = FakeExerciseRepository(
        listOf(
            exercise("Bench Press", favorite = true),
            exercise("Barbell Row", primary = Muscle.UPPER_BACK),
            exercise("Dumbbell Curl", primary = Muscle.BICEPS, equipment = Equipment.DUMBBELL, aliases = listOf("DB Curl")),
            exercise("Squat", primary = Muscle.QUADS),
        ),
    )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = ExerciseLibraryViewModel(repo, AppScope(kotlinx.coroutines.CoroutineScope(dispatcher)), dispatcher)

    @Test
    fun browsingShowsFavoritesThenLetters() = runTest(dispatcher) {
        vm().state.test {
            val state = expectMostRecentItem()
            assertEquals(LibrarySection.Kind.FAVORITES, state.sections.first().kind)
            assertEquals(listOf("Bench Press"), state.sections.first().rows.map { it.name })
            assertEquals(listOf('B', 'D', 'S'), state.sections.drop(1).map { it.letter })
        }
    }

    @Test
    fun searchingShowsOneRankedResultsSection() = runTest(dispatcher) {
        val vm = vm()
        vm.state.test {
            vm.onIntent(ExerciseLibraryIntent.QueryChanged("db curl"))
            val state = expectMostRecentItem()
            assertEquals(listOf(LibrarySection.Kind.RESULTS), state.sections.map { it.kind })
            assertEquals(listOf("Dumbbell Curl"), state.sections.single().rows.map { it.name })
        }
    }

    @Test
    fun chipsToggleAndCombine() = runTest(dispatcher) {
        val vm = vm()
        vm.state.test {
            vm.onIntent(ExerciseLibraryIntent.MuscleGroupToggled(MuscleGroup.ARMS))
            assertEquals(listOf("Dumbbell Curl"), expectMostRecentItem().sections.single().rows.map { it.name })
            vm.onIntent(ExerciseLibraryIntent.MuscleGroupToggled(MuscleGroup.ARMS)) // tap again = off
            vm.onIntent(ExerciseLibraryIntent.EquipmentToggled(Equipment.BARBELL))
            val names = expectMostRecentItem().sections.single().rows.map { it.name }
            assertEquals(listOf("Barbell Row", "Bench Press", "Squat"), names)
        }
    }

    @Test
    fun noMatchesIsEmptyButEmptyLibraryIsLoading() = runTest(dispatcher) {
        val vm = vm()
        vm.state.test {
            vm.onIntent(ExerciseLibraryIntent.QueryChanged("zzzz"))
            assertTrue(expectMostRecentItem().isEmpty)
            vm.onIntent(ExerciseLibraryIntent.ClearFilters)
            repo.exercises.value = emptyList()
            val state = expectMostRecentItem()
            assertTrue(state.isLoading)
            assertFalse(state.isEmpty)
        }
    }

    @Test
    fun favoriteToggleWritesThroughAndMovesIntoFavorites() = runTest(dispatcher) {
        val vm = vm()
        vm.state.test {
            vm.onIntent(ExerciseLibraryIntent.FavoriteToggled("squat", true))
            val favorites = expectMostRecentItem().sections.first()
            assertEquals(listOf("Bench Press", "Squat"), favorites.rows.map { it.name })
        }
    }
}
