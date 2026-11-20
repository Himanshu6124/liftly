package com.himanshu.liftly.feature.workout.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.core.domain.repository.WorkoutRepository
import com.himanshu.liftly.core.domain.util.AppScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The active workout is read straight from Room, so the screen survives process death:
 * reopening the app restores exactly what's in the database.
 */
class ActiveWorkoutViewModel(
    private val workouts: WorkoutRepository,
    private val appScope: AppScope,
) : ViewModel() {

    private val _state = MutableStateFlow(ActiveWorkoutUiState())
    val state: StateFlow<ActiveWorkoutUiState> = _state.asStateFlow()

    private val _effects = Channel<ActiveWorkoutEffect>(Channel.BUFFERED)
    val effects: Flow<ActiveWorkoutEffect> = _effects.receiveAsFlow()

    /** Reverts the last undoable change; only the latest one can be undone (one snackbar at a time). */
    private var undo: (suspend () -> Unit)? = null

    init {
        workouts.observeActiveWorkout()
            .onEach { active ->
                _state.update { s -> active?.toUiState(s) ?: s.copy(isLoading = false, workoutId = null, cards = s.cards) }
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: ActiveWorkoutIntent) {
        val workoutId = _state.value.workoutId
        when (intent) {
            is ActiveWorkoutIntent.Rename -> workoutId?.let { write { workouts.rename(it, intent.name) } }

            is ActiveWorkoutIntent.AddExercises -> workoutId?.let { write { workouts.addExercises(it, intent.exerciseIds) } }

            is ActiveWorkoutIntent.CommitOrder -> workoutId?.let { write { workouts.reorder(it, intent.workoutExerciseIds) } }

            is ActiveWorkoutIntent.Move -> move(intent.workoutExerciseId, intent.by)

            is ActiveWorkoutIntent.Skip -> skip(intent.workoutExerciseId)

            is ActiveWorkoutIntent.Unskip -> write { workouts.setStatus(intent.workoutExerciseId, ExerciseStatus.PENDING) }

            is ActiveWorkoutIntent.Remove -> remove(intent.workoutExerciseId)

            ActiveWorkoutIntent.Undo -> undo?.let { action ->
                undo = null
                write { action() }
            }

            ActiveWorkoutIntent.FinishRequested -> when {
                _state.value.pendingCount > 0 -> _state.update { it.copy(showFinishSheet = true) }
                else -> finish()
            }

            ActiveWorkoutIntent.FinishConfirmed -> finish()

            ActiveWorkoutIntent.FinishCancelled -> _state.update { it.copy(showFinishSheet = false) }

            ActiveWorkoutIntent.DiscardRequested -> _state.update { it.copy(showDiscardDialog = true) }

            ActiveWorkoutIntent.DiscardCancelled -> _state.update { it.copy(showDiscardDialog = false) }

            ActiveWorkoutIntent.DiscardConfirmed -> workoutId?.let { id ->
                _state.update { it.copy(showDiscardDialog = false) }
                viewModelScope.launch {
                    appScope.launch { workouts.discard(id) }.join()
                    _effects.send(ActiveWorkoutEffect.Closed(finished = false))
                }
            }
        }
    }

    private fun skip(workoutExerciseId: String) {
        val card = _state.value.cards.find { it.workoutExerciseId == workoutExerciseId } ?: return
        val previous = card.status
        undo = { workouts.setStatus(workoutExerciseId, previous) }
        write { workouts.setStatus(workoutExerciseId, ExerciseStatus.SKIPPED) }
        _effects.trySend(ActiveWorkoutEffect.Undoable(ActiveWorkoutEffect.Undoable.Kind.SKIPPED, card.name))
    }

    private fun remove(workoutExerciseId: String) {
        val card = _state.value.cards.find { it.workoutExerciseId == workoutExerciseId } ?: return
        viewModelScope.launch {
            val removed = appScope.async { workouts.removeExercise(workoutExerciseId) }.await() ?: return@launch
            undo = { workouts.restoreExercise(removed) }
            _effects.send(ActiveWorkoutEffect.Undoable(ActiveWorkoutEffect.Undoable.Kind.REMOVED, card.name))
        }
    }

    /** Accessible alternative to drag-and-drop (card menu "Move up/down"). */
    private fun move(workoutExerciseId: String, by: Int) {
        val ids = _state.value.cards.map { it.workoutExerciseId }.toMutableList()
        val from = ids.indexOf(workoutExerciseId)
        val to = (from + by).coerceIn(0, ids.lastIndex)
        if (from < 0 || from == to) return
        ids.add(to, ids.removeAt(from))
        onIntent(ActiveWorkoutIntent.CommitOrder(ids))
    }

    private fun finish() {
        val s = _state.value
        val id = s.workoutId ?: return
        _state.update { it.copy(showFinishSheet = false) }
        viewModelScope.launch {
            // A workout with no exercises at all has nothing worth keeping in history.
            if (s.cards.isEmpty()) {
                appScope.launch { workouts.discard(id) }.join()
                _effects.send(ActiveWorkoutEffect.Closed(finished = false))
            } else {
                appScope.launch { workouts.finish(id) }.join()
                _effects.send(ActiveWorkoutEffect.Closed(finished = true))
            }
        }
    }

    /** Writes run in the app scope so they complete even if the screen is closed mid-write. */
    private fun write(block: suspend () -> Unit) {
        appScope.launch { block() }
    }
}
