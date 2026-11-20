package com.himanshu.liftly.feature.exercises.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himanshu.liftly.core.domain.repository.ExerciseRepository
import com.himanshu.liftly.core.domain.repository.WorkoutRepository
import com.himanshu.liftly.core.domain.search.ExerciseSearch
import com.himanshu.liftly.core.domain.usecase.SuggestExercises
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * @param contextExerciseIds exercises already in the workout/routine being edited; drives "Suggested".
 */
class ExercisePickerViewModel(
    private val contextExerciseIds: List<String>,
    private val exercises: ExerciseRepository,
    workouts: WorkoutRepository,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _query = MutableStateFlow("")

    /** Read synchronously by the text field (see ExerciseLibraryViewModel.query). */
    val query: StateFlow<String> = _query

    private val filters = MutableStateFlow(ExercisePickerUiState())

    val state: StateFlow<ExercisePickerUiState> = combine(
        exercises.observeLibrary(),
        workouts.observeRecentExerciseIds(RECENT_LIMIT),
        filters,
        _query,
    ) { all, recentIds, f, q ->
        val browsing = q.isBlank() && f.muscleGroup == null && f.equipment == null
        val results = ExerciseSearch.filter(all, q, f.muscleGroup, f.equipment)
        val byId = all.associateBy { it.id }
        f.copy(
            sections = buildPickerSections(
                results = results,
                browsing = browsing,
                suggested = if (browsing) SuggestExercises.suggest(all, contextExerciseIds) else emptyList(),
                recent = if (browsing) recentIds.mapNotNull { byId[it] } else emptyList(),
            ),
            isLoading = all.isEmpty() && browsing,
            noResults = !browsing && results.isEmpty(),
        )
    }
        .flowOn(computeDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExercisePickerUiState())

    fun onIntent(intent: ExercisePickerIntent) {
        when (intent) {
            is ExercisePickerIntent.QueryChanged -> _query.value = intent.query

            is ExercisePickerIntent.MuscleGroupToggled -> filters.update {
                it.copy(muscleGroup = intent.group.takeUnless { g -> g == it.muscleGroup })
            }

            is ExercisePickerIntent.EquipmentToggled -> filters.update {
                it.copy(equipment = intent.equipment.takeUnless { e -> e == it.equipment })
            }

            is ExercisePickerIntent.SelectionToggled -> filters.update {
                val selected = if (intent.exerciseId in it.selected) it.selected - intent.exerciseId else it.selected + intent.exerciseId
                it.copy(selected = selected.toImmutableList())
            }

            is ExercisePickerIntent.CreateCustom -> viewModelScope.launch {
                val id = exercises.createCustom(intent.exercise)
                // A just-created exercise is obviously wanted: select it and clear the search.
                _query.value = ""
                filters.update { it.copy(selected = (it.selected + id).toImmutableList()) }
            }
        }
    }

    private companion object {
        const val RECENT_LIMIT = 10
    }
}
