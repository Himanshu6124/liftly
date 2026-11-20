package com.himanshu.liftly.feature.history.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himanshu.liftly.core.domain.model.Muscle
import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.repository.SettingsRepository
import com.himanshu.liftly.core.domain.repository.WorkoutRepository
import com.himanshu.liftly.core.domain.util.AppScope
import com.himanshu.liftly.core.domain.util.MuscleSets
import com.himanshu.liftly.core.domain.util.TimeProvider
import com.himanshu.liftly.core.domain.util.WeekGroup
import com.himanshu.liftly.core.domain.util.WeekGrouping
import com.himanshu.liftly.feature.history.HistoryUndo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@Immutable
data class HistoryUiState(
    val isLoading: Boolean = true,
    val weeks: ImmutableList<WeekGroup> = persistentListOf(),
    val unit: WeightUnit = WeightUnit.KG,
)

/** Weekly sets per muscle (top of History); [weekOffset] 0 = this week, -1 = last week, … */
@Immutable
data class MuscleWeekUi(
    val weekOffset: Int = 0,
    val weekStart: LocalDate? = null,
    /** Highest first; only muscles with sets. */
    val sets: ImmutableList<Pair<Muscle, Double>> = persistentListOf(),
) {
    val canGoForward: Boolean get() = weekOffset < 0
}

@OptIn(ExperimentalCoroutinesApi::class)
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

    private val weekOffset = MutableStateFlow(0)

    val muscleWeek: StateFlow<MuscleWeekUi> = weekOffset.flatMapLatest { offset ->
        val (start, from, to) = WeekGrouping.weekBounds(time.now(), offset)
        workouts.observeWorkingSetsByExercise(from, to).map { counts ->
            val sets = MuscleSets.perMuscle(counts).entries.sortedByDescending { it.value }.map { it.key to it.value }
            MuscleWeekUi(offset, start, sets.toImmutableList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MuscleWeekUi())

    fun showWeek(delta: Int) = weekOffset.update { (it + delta).coerceAtMost(0) }

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
