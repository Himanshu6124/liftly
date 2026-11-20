package com.himanshu.liftly.feature.exercises.detail

import androidx.compose.runtime.Immutable
import com.himanshu.liftly.core.domain.model.Difficulty
import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.Exercise
import com.himanshu.liftly.core.domain.model.Muscle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Immutable
data class ExerciseDetailUi(
    val id: String,
    val name: String,
    val primaryMuscle: Muscle,
    val secondaryMuscles: ImmutableList<Muscle>,
    val equipment: Equipment,
    val difficulty: Difficulty?,
    val imagePaths: ImmutableList<String>,
    val videoUrl: String?,
    val instructions: ImmutableList<String>,
    val formCues: ImmutableList<String>,
    val commonMistakes: ImmutableList<String>,
    val isFavorite: Boolean,
)

enum class DetailTab { FORM, HISTORY, RECORDS }

@Immutable
data class ExerciseDetailUiState(
    val exercise: ExerciseDetailUi? = null,
    /** Local draft so typing is never interrupted by DB round-trips. */
    val noteDraft: String = "",
    val tab: DetailTab = DetailTab.FORM,
    val isLoading: Boolean = true,
) {
    val notFound: Boolean get() = !isLoading && exercise == null
}

sealed interface ExerciseDetailIntent {
    data object ToggleFavorite : ExerciseDetailIntent
    data class NoteChanged(val text: String) : ExerciseDetailIntent
    data class TabSelected(val tab: DetailTab) : ExerciseDetailIntent
    data object WatchVideo : ExerciseDetailIntent
}

internal fun Exercise.toDetailUi() = ExerciseDetailUi(
    id = id,
    name = name,
    primaryMuscle = primaryMuscle,
    secondaryMuscles = secondaryMuscles.toImmutableList(),
    equipment = equipment,
    difficulty = difficulty,
    imagePaths = imagePaths.toImmutableList(),
    videoUrl = videoUrl,
    instructions = instructions.toImmutableList(),
    formCues = formCues.toImmutableList(),
    commonMistakes = commonMistakes.toImmutableList(),
    isFavorite = isFavorite,
)
