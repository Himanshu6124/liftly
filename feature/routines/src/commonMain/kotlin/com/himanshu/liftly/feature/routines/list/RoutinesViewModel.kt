package com.himanshu.liftly.feature.routines.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himanshu.liftly.core.domain.model.RoutineWithExercises
import com.himanshu.liftly.core.domain.repository.RoutineRepository
import com.himanshu.liftly.core.domain.usecase.StartRoutineUseCase
import com.himanshu.liftly.core.domain.util.AppScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Instant

@Immutable
data class RoutineRow(
    val id: String,
    val name: String,
    val exerciseCount: Int,
    val exerciseNames: String,
    val lastPerformedAt: Instant?,
)

@Immutable
data class RoutinesUiState(val isLoading: Boolean = true, val routines: ImmutableList<RoutineRow> = persistentListOf())

sealed interface RoutinesIntent {
    data class Start(val routineId: String) : RoutinesIntent
    data class Delete(val routineId: String) : RoutinesIntent
    data object UndoDelete : RoutinesIntent
    data class Reorder(val orderedIds: List<String>) : RoutinesIntent
}

sealed interface RoutinesEffect {
    data object OpenWorkout : RoutinesEffect
    data class Deleted(val name: String) : RoutinesEffect
}

class RoutinesViewModel(
    private val routines: RoutineRepository,
    private val startRoutine: StartRoutineUseCase,
    private val appScope: AppScope,
) : ViewModel() {
    private var latest: List<RoutineWithExercises> = emptyList()
    private var deleted: RoutineWithExercises? = null

    val state: StateFlow<RoutinesUiState> = routines.observeRoutines().map { list ->
        latest = list
        RoutinesUiState(isLoading = false, routines = list.map { it.toRow() }.toImmutableList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutinesUiState())

    private val _effects = Channel<RoutinesEffect>(Channel.BUFFERED)
    val effects: Flow<RoutinesEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: RoutinesIntent) {
        when (intent) {
            is RoutinesIntent.Start -> latest.find { it.routine.id == intent.routineId }?.let { r ->
                viewModelScope.launch {
                    appScope.async { startRoutine(r) }.await()
                    _effects.send(RoutinesEffect.OpenWorkout)
                }
            }

            is RoutinesIntent.Delete -> viewModelScope.launch {
                deleted = appScope.async { routines.deleteRoutine(intent.routineId) }.await()
                deleted?.let { _effects.send(RoutinesEffect.Deleted(it.routine.name)) }
            }

            RoutinesIntent.UndoDelete -> deleted?.let { snapshot ->
                deleted = null
                appScope.launch { routines.restoreRoutine(snapshot) }
            }

            is RoutinesIntent.Reorder -> appScope.launch { routines.reorderRoutines(intent.orderedIds) }
        }
    }
}

fun RoutineWithExercises.toRow() = RoutineRow(
    id = routine.id,
    name = routine.name,
    exerciseCount = items.size,
    exerciseNames = items.joinToString(", ") { it.exercise.name },
    lastPerformedAt = routine.lastPerformedAt,
)
