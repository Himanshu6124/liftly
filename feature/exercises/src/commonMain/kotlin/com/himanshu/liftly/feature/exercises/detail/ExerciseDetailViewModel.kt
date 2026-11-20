package com.himanshu.liftly.feature.exercises.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himanshu.liftly.core.domain.repository.ExerciseRepository
import com.himanshu.liftly.core.domain.util.AppScope
import com.himanshu.liftly.core.platform.UrlLauncher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) : ViewModel() {

    private val _state = MutableStateFlow(ExerciseDetailUiState())
    val state: StateFlow<ExerciseDetailUiState> = _state.asStateFlow()

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
        }
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
