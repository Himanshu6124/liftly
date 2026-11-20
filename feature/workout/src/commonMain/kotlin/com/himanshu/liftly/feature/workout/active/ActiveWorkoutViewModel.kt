package com.himanshu.liftly.feature.workout.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himanshu.liftly.core.domain.model.DraftSet
import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.core.domain.model.UserSettings
import com.himanshu.liftly.core.domain.repository.RoutineRepository
import com.himanshu.liftly.core.domain.repository.SettingsRepository
import com.himanshu.liftly.core.domain.repository.WorkoutRepository
import com.himanshu.liftly.core.domain.usecase.GetLogSheetPrefillUseCase
import com.himanshu.liftly.core.domain.usecase.LogExerciseSetsUseCase
import com.himanshu.liftly.core.domain.util.AppScope
import com.himanshu.liftly.core.domain.util.toDraftSets
import com.himanshu.liftly.core.platform.Haptics
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    private val routines: RoutineRepository,
    private val settings: SettingsRepository,
    private val logSets: LogExerciseSetsUseCase,
    private val prefill: GetLogSheetPrefillUseCase,
    private val haptics: Haptics,
    private val appScope: AppScope,
) : ViewModel() {

    private val _state = MutableStateFlow(ActiveWorkoutUiState())
    val state: StateFlow<ActiveWorkoutUiState> = _state.asStateFlow()

    private val _effects = Channel<ActiveWorkoutEffect>(Channel.BUFFERED)
    val effects: Flow<ActiveWorkoutEffect> = _effects.receiveAsFlow()

    /** Reverts the last undoable change; only the latest one can be undone (one snackbar at a time). */
    private var undo: (suspend () -> Unit)? = null

    /** Last-session sets per exercise id, for the "Last: …" hint on pending cards. */
    private val lastSets = MutableStateFlow<Map<String, List<DraftSet>>>(emptyMap())
    private var userSettings = UserSettings()
    private val requestedHints = mutableSetOf<String>()

    init {
        combine(workouts.observeActiveWorkout(), settings.settings, lastSets) { active, s, last ->
            userSettings = s
            if (active != null) loadMissingHints(active.workout.id, active.exercises.map { it.exercise.id }, last)
            active?.toUiState(_state.value, s.weightUnit, last)
        }
            .onEach { ui -> _state.update { s -> ui ?: s.copy(isLoading = false, workoutId = null) } }
            .launchIn(viewModelScope)
    }

    private fun loadMissingHints(workoutId: String, exerciseIds: List<String>, known: Map<String, List<DraftSet>>) {
        val missing = exerciseIds.filter { it !in known && requestedHints.add(it) }
        if (missing.isEmpty()) return
        viewModelScope.launch {
            val loaded = missing.associateWith { id -> workouts.getLastSession(id, workoutId)?.sets?.toDraftSets().orEmpty() }
            lastSets.update { it + loaded }
        }
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

            is ActiveWorkoutIntent.OpenLogSheet -> _state.update { it.copy(logSheetFor = intent.workoutExerciseId) }

            ActiveWorkoutIntent.CloseLogSheet -> _state.update { it.copy(logSheetFor = null) }

            is ActiveWorkoutIntent.Logged -> {
                undo = { workouts.restoreLog(intent.undo) }
                _state.update { it.copy(logSheetFor = null) }
                advanceFrom(intent.workoutExerciseId)
            }

            is ActiveWorkoutIntent.QuickLog -> quickLog(intent.workoutExerciseId)

            ActiveWorkoutIntent.FinishRequested -> when {
                _state.value.pendingCount > 0 -> _state.update { it.copy(showFinishSheet = true) }
                else -> finish()
            }

            ActiveWorkoutIntent.FinishConfirmed -> finish()

            ActiveWorkoutIntent.FinishCancelled -> _state.update { it.copy(showFinishSheet = false) }

            ActiveWorkoutIntent.SaveAsRoutineRequested -> _state.update { it.copy(showSaveRoutineDialog = true) }

            ActiveWorkoutIntent.SaveAsRoutineCancelled -> _state.update { it.copy(showSaveRoutineDialog = false) }

            is ActiveWorkoutIntent.SaveAsRoutine -> workoutId?.let { id ->
                _state.update { it.copy(showSaveRoutineDialog = false) }
                viewModelScope.launch {
                    appScope.async { routines.saveWorkoutAsRoutine(id, intent.name) }.await()
                    _effects.send(ActiveWorkoutEffect.Info(ActiveWorkoutEffect.Info.Kind.ROUTINE_SAVED, intent.name.trim()))
                }
            }

            ActiveWorkoutIntent.UpdateRoutineTargets -> {
                val routineId = _state.value.routineId
                if (workoutId != null && routineId != null) {
                    viewModelScope.launch {
                        appScope.async { routines.updateTargetsFromWorkout(routineId, workoutId) }.await()
                        _effects.send(ActiveWorkoutEffect.Info(ActiveWorkoutEffect.Info.Kind.TARGETS_UPDATED))
                    }
                }
            }

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

    /** Logs exactly the pre-fill. With nothing to pre-fill from (first time), opens the sheet instead. */
    private fun quickLog(workoutExerciseId: String) {
        val card = _state.value.cards.find { it.workoutExerciseId == workoutExerciseId } ?: return
        viewModelScope.launch {
            val saved: LogExerciseSetsUseCase.Result.Saved? = appScope.async {
                val data = workouts.getLogSheetData(workoutExerciseId) ?: return@async null
                val sets = prefill(data, userSettings.defaultSetCount).sets
                if (sets.none { it.reps != null || it.durationSec != null || it.distanceM != null }) return@async null
                logSets(workoutExerciseId, sets, data.workoutExercise.inputMode) as? LogExerciseSetsUseCase.Result.Saved
            }.await()
            if (saved == null) {
                _state.update { it.copy(logSheetFor = workoutExerciseId) }
                return@launch
            }
            if (saved.records.isNotEmpty()) haptics.success() else haptics.light()
            undo = { workouts.restoreLog(saved.undo) }
            _effects.send(ActiveWorkoutEffect.Undoable(ActiveWorkoutEffect.Undoable.Kind.LOGGED, card.name))
            advanceFrom(workoutExerciseId)
        }
    }

    /** After a log: highlight and scroll to the next pending card, or open its sheet if the user wants that. */
    private fun advanceFrom(workoutExerciseId: String) {
        val cards = _state.value.cards
        val start = cards.indexOfFirst { it.workoutExerciseId == workoutExerciseId }
        val next = (cards.drop(start + 1) + cards.take(start.coerceAtLeast(0)))
            .firstOrNull { it.status == ExerciseStatus.PENDING && it.workoutExerciseId != workoutExerciseId }
            ?: return
        if (userSettings.openNextExerciseAutomatically) {
            _state.update { it.copy(logSheetFor = next.workoutExerciseId) }
            return
        }
        _state.update { it.copy(highlightedId = next.workoutExerciseId) }
        _effects.trySend(ActiveWorkoutEffect.ScrollTo(next.workoutExerciseId))
        viewModelScope.launch {
            delay(HIGHLIGHT_MS)
            _state.update { if (it.highlightedId == next.workoutExerciseId) it.copy(highlightedId = null) else it }
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
                _effects.send(ActiveWorkoutEffect.Closed(finished = true, workoutId = id))
            }
        }
    }

    /** Writes run in the app scope so they complete even if the screen is closed mid-write. */
    private fun write(block: suspend () -> Unit) {
        appScope.launch { block() }
    }

    private companion object {
        const val HIGHLIGHT_MS = 1_500L
    }
}
