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
            notes: String?,
            personalRecords: List<com.himanshu.liftly.core.domain.usecase.PrHit>,
        ): com.himanshu.liftly.core.domain.model.LogSnapshot = error("not used")
        override suspend fun getSessionHistory(exerciseId: String, excludeWorkoutExerciseId: String) =
            emptyList<List<com.himanshu.liftly.core.domain.model.WorkoutSet>>()
        override fun observeRecords(exerciseId: String) =
            kotlinx.coroutines.flow.flowOf(emptyList<com.himanshu.liftly.core.domain.model.PersonalRecord>())
        override suspend fun restoreLog(snapshot: com.himanshu.liftly.core.domain.model.LogSnapshot) = Unit
        override fun observeLastFinished() = kotlinx.coroutines.flow.flowOf<com.himanshu.liftly.core.domain.model.WorkoutSummaryLine?>(null)
        override fun observeRecentExerciseIds(limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<String>())
    }

    private val routines = object : com.himanshu.liftly.core.domain.repository.RoutineRepository {
        override fun observeRoutines() = kotlinx.coroutines.flow.flowOf(
            emptyList<com.himanshu.liftly.core.domain.model.RoutineWithExercises>(),
        )
        override fun observeRoutine(
            id: String,
        ) = kotlinx.coroutines.flow.flowOf<com.himanshu.liftly.core.domain.model.RoutineWithExercises?>(null)
        override suspend fun saveRoutine(
            id: String?,
            name: String,
            notes: String?,
            exercises: List<com.himanshu.liftly.core.domain.model.RoutineExerciseDraft>,
        ) = "r"
        override suspend fun deleteRoutine(id: String) = null
        override suspend fun restoreRoutine(snapshot: com.himanshu.liftly.core.domain.model.RoutineWithExercises) = Unit
        override suspend fun reorderRoutines(orderedIds: List<String>) = Unit
        override suspend fun saveWorkoutAsRoutine(workoutId: String, name: String) = "r"
        override suspend fun updateTargetsFromWorkout(routineId: String, workoutId: String) = Unit
    }

    private val settings = object : com.himanshu.liftly.core.domain.repository.SettingsRepository {
        override val settings = kotlinx.coroutines.flow.MutableStateFlow(com.himanshu.liftly.core.domain.model.UserSettings())
        override suspend fun update(
            transform: (com.himanshu.liftly.core.domain.model.UserSettings) -> com.himanshu.liftly.core.domain.model.UserSettings,
        ) = Unit
        override suspend fun seedVersion() = 1
        override suspend fun setSeedVersion(version: Int) = Unit
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun mapsSyncStateToLabel() = runTest {
        val vm = HomeViewModel(
            sync,
            workouts,
            routines,
            settings,
            StartWorkoutUseCase(workouts),
            com.himanshu.liftly.core.domain.usecase.StartRoutineUseCase(StartWorkoutUseCase(workouts)),
        )
        vm.state.test {
            assertEquals(SyncLabel.LOCAL_ONLY, awaitItem().syncLabel)
            sync.flow.value = SyncState.Syncing
            assertEquals(SyncLabel.SYNCING, awaitItem().syncLabel)
        }
    }

    @Test
    fun startEmptyWorkoutEmitsNavigationEffect() = runTest {
        val vm = HomeViewModel(
            sync,
            workouts,
            routines,
            settings,
            StartWorkoutUseCase(workouts),
            com.himanshu.liftly.core.domain.usecase.StartRoutineUseCase(StartWorkoutUseCase(workouts)),
        )
        vm.effects.test {
            vm.onIntent(HomeIntent.StartEmptyWorkout)
            assertEquals(HomeEffect.OpenActiveWorkout, awaitItem())
            assertEquals(1, workouts.created)
        }
    }
}
