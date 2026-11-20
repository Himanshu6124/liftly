package com.himanshu.liftly.feature.home

import app.cash.turbine.test
import com.himanshu.liftly.core.domain.sync.SyncController
import com.himanshu.liftly.core.domain.sync.SyncState
import com.himanshu.liftly.core.domain.usecase.StartWorkoutUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val sync = object : SyncController {
        val flow = MutableStateFlow<SyncState>(SyncState.LocalOnly)
        override val state = flow
        override fun requestSync() = Unit
    }

    private val workouts = object : com.himanshu.liftly.core.domain.repository.WorkoutRepository {
        var created = 0
        override fun observeActiveWorkout() = kotlinx.coroutines.flow.flowOf(null)
        override suspend fun getInProgressWorkout() = null
        override suspend fun createWorkout(name: String, routineId: String?, exerciseIds: List<String>): String {
            created++
            return "w"
        }
        override suspend fun addExercises(workoutId: String, exerciseIds: List<String>) = Unit
        override suspend fun rename(workoutId: String, name: String) = Unit
        override suspend fun reorder(workoutId: String, orderedWorkoutExerciseIds: List<String>) = Unit
        override suspend fun setStatus(workoutExerciseId: String, status: com.himanshu.liftly.core.domain.model.ExerciseStatus) = Unit
        override suspend fun removeExercise(workoutExerciseId: String) = null
        override suspend fun restoreExercise(removed: com.himanshu.liftly.core.domain.model.RemovedWorkoutExercise) = Unit
        override suspend fun finish(workoutId: String) = Unit
        override suspend fun discard(workoutId: String) = Unit
        override suspend fun getLogSheetData(workoutExerciseId: String): com.himanshu.liftly.core.domain.model.LogSheetData? = null
        override suspend fun getLastSession(
            exerciseId: String,
            excludeWorkoutId: String,
        ): com.himanshu.liftly.core.domain.model.LastSession? = null
        override suspend fun logSets(
            workoutExerciseId: String,
            sets: List<com.himanshu.liftly.core.domain.model.DraftSet>,
            inputMode: com.himanshu.liftly.core.domain.model.InputMode,
        ): com.himanshu.liftly.core.domain.model.LogSnapshot = error("not used")
        override suspend fun restoreLog(snapshot: com.himanshu.liftly.core.domain.model.LogSnapshot) = Unit
        override fun observeRecentExerciseIds(limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<String>())
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun mapsSyncStateToLabel() = runTest {
        val vm = HomeViewModel(sync, workouts, StartWorkoutUseCase(workouts))
        vm.state.test {
            assertEquals(SyncLabel.LOCAL_ONLY, awaitItem().syncLabel)
            sync.flow.value = SyncState.Syncing
            assertEquals(SyncLabel.SYNCING, awaitItem().syncLabel)
        }
    }

    @Test
    fun startEmptyWorkoutEmitsNavigationEffect() = runTest {
        val vm = HomeViewModel(sync, workouts, StartWorkoutUseCase(workouts))
        vm.effects.test {
            vm.onIntent(HomeIntent.StartEmptyWorkout)
            assertEquals(HomeEffect.OpenActiveWorkout, awaitItem())
            assertEquals(1, workouts.created)
        }
    }
}
