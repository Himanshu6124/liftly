package com.himanshu.liftly.feature.exercises.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himanshu.liftly.core.domain.model.ExerciseSession
import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.repository.ExerciseRepository
import com.himanshu.liftly.core.domain.repository.SettingsRepository
import com.himanshu.liftly.core.domain.repository.WorkoutRepository
import com.himanshu.liftly.core.domain.util.AppScope
import com.himanshu.liftly.core.domain.util.ExerciseProgress
import com.himanshu.liftly.core.domain.util.ProgressMetric
import com.himanshu.liftly.core.domain.util.ProgressRange
import com.himanshu.liftly.core.domain.util.SetFormatter
import com.himanshu.liftly.core.domain.util.TimeProvider
import com.himanshu.liftly.core.domain.util.toDraftSets
import com.himanshu.liftly.core.platform.UrlLauncher
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class ExerciseDetailViewModel(
    private val exerciseId: String,
    private val repository: ExerciseRepository,
    private val appScope: AppScope,
    private val urlLauncher: UrlLauncher,
    workouts: WorkoutRepository,
    settings: SettingsRepository,
    private val time: TimeProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(ExerciseDetailUiState())
    val state: StateFlow<ExerciseDetailUiState> = _state.asStateFlow()

    private var sessions: List<ExerciseSession> = emptyList()
    private var noteLoaded = false
    private var lastSavedNote: String? = null

    init {
        repository.observeExercise(exerciseId)
            .onEach { exercise ->
                _state.update { s ->
                    s.copy(
                        exercise = exercise?.toDetailUi(),
                        isLoading = false,
                        // Take the stored note once; afterwards the draft is the source of truth.
                        noteDraft = if (noteLoaded) s.noteDraft else exercise?.userNote.orEmpty(),
                    )
                }
                if (!noteLoaded && exercise != null) {
                    noteLoaded = true
                    lastSavedNote = exercise.userNote.orEmpty()
                }
            }
            .launchIn(viewModelScope)

        combine(workouts.observeRecords(exerciseId), settings.settings) { records, s ->
            records.map { RecordUi(it.type, it.value, it.achievedAt) }.toImmutableList() to s.weightUnit
        }
            .onEach { (records, unit) -> _state.update { it.copy(records = records, unit = unit) } }
            .launchIn(viewModelScope)

        combine(workouts.observeExerciseSessions(exerciseId), settings.settings) { list, s -> list to s.weightUnit }
            .onEach { (list, unit) ->
                sessions = list
                _state.update { st ->
                    st.copy(
                        sessions = list.map { it.toUi(unit) }.toImmutableList(),
                        progress = progress(st.progress.metric, st.progress.range),
                    )
                }
            }
            .launchIn(viewModelScope)

        // Save the note shortly after typing stops.
        _state.map { it.noteDraft }
            .drop(1)
            .debounce(NOTE_DEBOUNCE_MS)
            .onEach(::saveNote)
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: ExerciseDetailIntent) {
        when (intent) {
            ExerciseDetailIntent.ToggleFavorite -> {
                val current = _state.value.exercise ?: return
                appScope.launch { repository.setFavorite(current.id, !current.isFavorite) }
            }

            is ExerciseDetailIntent.NoteChanged -> _state.update { it.copy(noteDraft = intent.text) }

            is ExerciseDetailIntent.TabSelected -> _state.update { it.copy(tab = intent.tab) }

            ExerciseDetailIntent.WatchVideo -> _state.value.exercise?.videoUrl?.let(urlLauncher::open)

            is ExerciseDetailIntent.MetricSelected -> _state.update {
                it.copy(progress = progress(intent.metric, it.progress.range))
            }

            is ExerciseDetailIntent.RangeSelected -> _state.update {
                it.copy(progress = progress(it.progress.metric, intent.range))
            }

            is ExerciseDetailIntent.PointSelected -> _state.update { it.copy(progress = it.progress.copy(selected = intent.index)) }
        }
    }

    /** Chart state for [metric] (or the first one with data) over [range]; selects the latest point. */
    private fun progress(metric: ProgressMetric?, range: ProgressRange): ProgressUi {
        val metrics = ExerciseProgress.available(sessions)
        val chosen = metric?.takeIf { it in metrics } ?: metrics.firstOrNull()
        val points = chosen?.let { ExerciseProgress.series(sessions, it, range, time.now()) }.orEmpty()
        return ProgressUi(
            metrics = metrics.toImmutableList(),
            metric = chosen,
            range = range,
            points = points.toImmutableList(),
            selected = points.lastIndex.takeIf { it >= 0 },
        )
    }

    private fun saveNote(note: String) {
        if (!noteLoaded || note == lastSavedNote) return
        lastSavedNote = note
        // App scope: the write completes even if the user leaves the screen right away.
        appScope.launch { repository.setUserNote(exerciseId, note) }
    }

    override fun onCleared() {
        saveNote(_state.value.noteDraft) // flush a pending debounce
        super.onCleared()
    }

    private companion object {
        const val NOTE_DEBOUNCE_MS = 500L
    }
}

private fun ExerciseSession.toUi(unit: WeightUnit) =
    SessionUi(workoutId, workoutName, performedAt, SetFormatter.summary(sets.toDraftSets(), unit))
