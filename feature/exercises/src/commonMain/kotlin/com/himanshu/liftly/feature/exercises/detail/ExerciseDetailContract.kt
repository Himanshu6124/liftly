package com.himanshu.liftly.feature.exercises.detail

import androidx.compose.runtime.Immutable
import com.himanshu.liftly.core.domain.model.Difficulty
import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.Exercise
import com.himanshu.liftly.core.domain.model.Muscle
import com.himanshu.liftly.core.domain.model.PrType
import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.util.ProgressMetric
import com.himanshu.liftly.core.domain.util.ProgressPoint
import com.himanshu.liftly.core.domain.util.ProgressRange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Instant

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
data class RecordUi(val type: PrType, val value: Double, val achievedAt: Instant)

/** One past session in the History tab. */
@Immutable
data class SessionUi(val workoutId: String, val workoutName: String, val performedAt: Instant, val summary: String?)

/** The chart on the History tab: values are in kg (weights) and are converted when formatted. */
@Immutable
data class ProgressUi(
    val metrics: ImmutableList<ProgressMetric> = persistentListOf(),
    val metric: ProgressMetric? = null,
    val range: ProgressRange = ProgressRange.ALL,
    val points: ImmutableList<ProgressPoint> = persistentListOf(),
    /** Index into [points]; defaults to the latest session. */
    val selected: Int? = null,
)

@Immutable
data class ExerciseDetailUiState(
    val exercise: ExerciseDetailUi? = null,
    /** Local draft so typing is never interrupted by DB round-trips. */
    val noteDraft: String = "",
    val tab: DetailTab = DetailTab.FORM,
    val records: ImmutableList<RecordUi> = persistentListOf(),
    val unit: WeightUnit = WeightUnit.KG,
    val sessions: ImmutableList<SessionUi> = persistentListOf(),
    val progress: ProgressUi = ProgressUi(),
    val isLoading: Boolean = true,
) {
    val notFound: Boolean get() = !isLoading && exercise == null
}

sealed interface ExerciseDetailIntent {
    data object ToggleFavorite : ExerciseDetailIntent
    data class NoteChanged(val text: String) : ExerciseDetailIntent
    data class TabSelected(val tab: DetailTab) : ExerciseDetailIntent
    data object WatchVideo : ExerciseDetailIntent
    data class MetricSelected(val metric: ProgressMetric) : ExerciseDetailIntent
    data class RangeSelected(val range: ProgressRange) : ExerciseDetailIntent
    data class PointSelected(val index: Int) : ExerciseDetailIntent
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
