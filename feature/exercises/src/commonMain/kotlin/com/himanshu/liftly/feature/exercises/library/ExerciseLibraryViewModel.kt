package com.himanshu.liftly.feature.exercises.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himanshu.liftly.core.domain.repository.ExerciseRepository
import com.himanshu.liftly.core.domain.search.ExerciseSearch
import com.himanshu.liftly.core.domain.util.AppScope
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

class ExerciseLibraryViewModel(
    private val repository: ExerciseRepository,
    private val appScope: AppScope,
    /** Search + grouping runs off the main thread; injectable for tests. */
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val filters = MutableStateFlow(ExerciseLibraryUiState())

    /**
     * The text field reads this directly (synchronously). Routing keystrokes through the async
     * search pipeline below would drop characters when typing fast.
     */
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val state: StateFlow<ExerciseLibraryUiState> = combine(repository.observeLibrary(), filters, _query) { all, filterState, q ->
        val f = filterState.copy(query = q)
        val results = ExerciseSearch.filter(all, f.query, f.muscleGroup, f.equipment)
        // An empty library without filters means first-launch seeding is still running.
        f.copy(sections = buildSections(results, browsing = !f.hasFilters), isLoading = all.isEmpty() && !f.hasFilters)
    }
        .flowOn(computeDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseLibraryUiState())

    fun onIntent(intent: ExerciseLibraryIntent) {
        when (intent) {
            is ExerciseLibraryIntent.QueryChanged -> _query.value = intent.query

            is ExerciseLibraryIntent.MuscleGroupToggled -> filters.update {
                it.copy(muscleGroup = intent.group.takeUnless { g -> g == it.muscleGroup })
            }

            is ExerciseLibraryIntent.EquipmentToggled -> filters.update {
                it.copy(equipment = intent.equipment.takeUnless { e -> e == it.equipment })
            }

            ExerciseLibraryIntent.ClearFilters -> {
                _query.value = ""
                filters.update { ExerciseLibraryUiState() }
            }

            is ExerciseLibraryIntent.FavoriteToggled -> appScope.launch { repository.setFavorite(intent.id, intent.favorite) }
        }
    }
}
