package com.himanshu.liftly.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.core.domain.model.RoutineWithExercises
import com.himanshu.liftly.core.domain.repository.RoutineRepository
import com.himanshu.liftly.core.domain.repository.SettingsRepository
import com.himanshu.liftly.core.domain.repository.WorkoutRepository
import com.himanshu.liftly.core.domain.sync.SyncController
import com.himanshu.liftly.core.domain.sync.SyncState
import com.himanshu.liftly.core.domain.usecase.NextRoutine
import com.himanshu.liftly.core.domain.usecase.StartRoutineUseCase
import com.himanshu.liftly.core.domain.usecase.StartWorkoutUseCase
import com.himanshu.liftly.core.domain.util.TimeProvider
import com.himanshu.liftly.core.domain.util.TrainingCalendar
import com.himanshu.liftly.feature.routines.list.toRow
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    syncController: SyncController,
    workouts: WorkoutRepository,
    routines: RoutineRepository,
    settings: SettingsRepository,
    private val startWorkout: StartWorkoutUseCase,
    private val startRoutine: StartRoutineUseCase,
    private val time: TimeProvider,
) : ViewModel() {
    private var latestRoutines: List<RoutineWithExercises> = emptyList()

    private val base = combine(
        syncController.state,
        workouts.observeActiveWorkout(),
        routines.observeRoutines(),
        workouts.observeLastFinished(),
        settings.settings,
    ) { sync, active, routineList, last, s ->
        latestRoutines = routineList
        HomeUiState(
            syncLabel = sync.toLabel(),
            inProgress = active?.let { a ->
                InProgressBanner(
                    name = a.workout.name,
                    startedAt = a.workout.startedAt,
                    exerciseCount = a.exercises.size,
                    loggedCount = a.exercises.count { it.workoutExercise.status == ExerciseStatus.LOGGED },
                )
            },
            nextRoutine = NextRoutine.pick(routineList)?.toRow(),
            routines = routineList.map { it.toRow() }.toImmutableList(),
            lastWorkout = last?.let { l ->
                LastWorkoutCard(l.workout.name, l.workout.startedAt, l.workout.endedAt, l.exerciseCount, l.loggedCount, l.volumeKg)
            },
            unit = s.weightUnit,
            isLoading = false,
        )
    }

    val state: StateFlow<HomeUiState> = combine(base, workouts.observeHistory(), settings.settings) { ui, history, s ->
        ui.copy(calendar = history.takeIf { it.isNotEmpty() }?.let { TrainingCalendar.from(it, s.weeklyGoal, time.now()) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.StartEmptyWorkout -> viewModelScope.launch {
                startWorkout()
                _effects.send(HomeEffect.OpenActiveWorkout)
            }

            HomeIntent.ResumeWorkout -> viewModelScope.launch { _effects.send(HomeEffect.OpenActiveWorkout) }

            is HomeIntent.StartRoutine -> latestRoutines.find { it.routine.id == intent.routineId }?.let { r ->
                viewModelScope.launch {
                    startRoutine(r)
                    _effects.send(HomeEffect.OpenActiveWorkout)
                }
            }
        }
    }

    private fun SyncState.toLabel() = when (this) {
        SyncState.LocalOnly, SyncState.Idle -> SyncLabel.LOCAL_ONLY
        SyncState.Syncing -> SyncLabel.SYNCING
        is SyncState.Synced -> SyncLabel.SYNCED
        is SyncState.Failed -> SyncLabel.FAILED
    }
}
