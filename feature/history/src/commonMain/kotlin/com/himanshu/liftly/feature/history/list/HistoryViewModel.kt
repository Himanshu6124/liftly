package com.himanshu.liftly.feature.history.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.repository.SettingsRepository
import com.himanshu.liftly.core.domain.repository.WorkoutRepository
import com.himanshu.liftly.core.domain.util.AppScope
import com.himanshu.liftly.core.domain.util.TimeProvider
import com.himanshu.liftly.core.domain.util.WeekGroup
import com.himanshu.liftly.core.domain.util.WeekGrouping
import com.himanshu.liftly.feature.history.HistoryUndo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class HistoryUiState(
    val isLoading: Boolean = true,
    val weeks: ImmutableList<WeekGroup> = persistentListOf(),
    val unit: WeightUnit = WeightUnit.KG,
)

class HistoryViewModel(
    private val workouts: WorkoutRepository,
    settings: SettingsRepository,
    private val time: TimeProvider,
    private val undo: HistoryUndo,
    private val appScope: AppScope,
) : ViewModel() {
    val state: StateFlow<HistoryUiState> = combine(workouts.observeHistory(), settings.settings) { items, s ->
        HistoryUiState(false, WeekGrouping.group(items, time.now()).toImmutableList(), s.weightUnit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    /** Set when a workout was just deleted; the screen shows Undo for it. */
    val pendingUndo = undo.pending

    fun delete(workoutId: String) = viewModelScope.launch {
        workouts.deleteWorkout(workoutId)?.let(undo::offer)
    }

    fun undoDelete() {
        undo.take()?.let { appScope.launch { workouts.restoreWorkout(it) } }
    }

    fun dismissUndo() {
        undo.take()
    }
}
