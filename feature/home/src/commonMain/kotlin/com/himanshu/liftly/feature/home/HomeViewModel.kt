package com.himanshu.liftly.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.core.domain.repository.WorkoutRepository
import com.himanshu.liftly.core.domain.sync.SyncController
import com.himanshu.liftly.core.domain.sync.SyncState
import com.himanshu.liftly.core.domain.usecase.StartWorkoutUseCase
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
    private val startWorkout: StartWorkoutUseCase,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(syncController.state, workouts.observeActiveWorkout()) { sync, active ->
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
        )
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
        }
    }

    private fun SyncState.toLabel() = when (this) {
        SyncState.LocalOnly, SyncState.Idle -> SyncLabel.LOCAL_ONLY
        SyncState.Syncing -> SyncLabel.SYNCING
        is SyncState.Synced -> SyncLabel.SYNCED
        is SyncState.Failed -> SyncLabel.FAILED
    }
}
