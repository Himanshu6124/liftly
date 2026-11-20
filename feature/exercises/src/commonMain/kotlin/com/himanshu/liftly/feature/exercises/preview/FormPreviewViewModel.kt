package com.himanshu.liftly.feature.exercises.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himanshu.liftly.core.domain.repository.ExerciseRepository
import com.himanshu.liftly.feature.exercises.detail.ExerciseDetailUi
import com.himanshu.liftly.feature.exercises.detail.toDetailUi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class FormPreviewViewModel(exerciseId: String, repository: ExerciseRepository) : ViewModel() {
    val exercise: StateFlow<ExerciseDetailUi?> = repository.observeExercise(exerciseId)
        .map { it?.toDetailUi() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
