package com.himanshu.liftly.feature.workout.active

import androidx.compose.runtime.Immutable
import com.himanshu.liftly.core.domain.model.ActiveWorkout
import com.himanshu.liftly.core.domain.model.DraftSet
import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.core.domain.model.Muscle
import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.util.SetFormatter
import com.himanshu.liftly.core.domain.util.toDraftSets
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Instant

@Immutable
data class ExerciseCardUi(
    val workoutExerciseId: String,
    val exerciseId: String,
    val name: String,
    val imagePaths: ImmutableList<String>,
    val primaryMuscle: Muscle,
    val equipment: Equipment,
    val status: ExerciseStatus,
    /** Logged: "4 × 80 kg · 8, 8, 7, 6". */
    val summary: String? = null,
    /** Pending: "77.5 kg × 8, 8, 8, 7" from the last session. */
    val lastHint: String? = null,
    /** Set a personal record in this workout. */
    val hasPr: Boolean = false,
)

@Immutable
data class ActiveWorkoutUiState(
    val isLoading: Boolean = true,
    val workoutId: String? = null,
    /** Empty = user never named it; the UI shows "Morning workout" etc. */
    val name: String = "",
    val startedAt: Instant? = null,
    val cards: ImmutableList<ExerciseCardUi> = persistentListOf(),
    val showFinishSheet: Boolean = false,
    /** Workout exercise whose Batch Log Sheet is open. */
    val logSheetFor: String? = null,
    /** Briefly highlighted card (the next pending one after a log). */
    val highlightedId: String? = null,
    val showDiscardDialog: Boolean = false,
) {
    val pendingCount: Int get() = cards.count { it.status == ExerciseStatus.PENDING }
    val exerciseIds: List<String> get() = cards.map { it.exerciseId }

    /** Loaded, but there is no workout in progress (finished, discarded, or never started). */
    val noWorkout: Boolean get() = !isLoading && workoutId == null
}

sealed interface ActiveWorkoutIntent {
    data class Rename(val name: String) : ActiveWorkoutIntent
    data class AddExercises(val exerciseIds: List<String>) : ActiveWorkoutIntent
    data class CommitOrder(val workoutExerciseIds: List<String>) : ActiveWorkoutIntent
    data class Move(val workoutExerciseId: String, val by: Int) : ActiveWorkoutIntent
    data class Skip(val workoutExerciseId: String) : ActiveWorkoutIntent
    data class Unskip(val workoutExerciseId: String) : ActiveWorkoutIntent
    data class Remove(val workoutExerciseId: String) : ActiveWorkoutIntent
    data object Undo : ActiveWorkoutIntent
    data class OpenLogSheet(val workoutExerciseId: String) : ActiveWorkoutIntent
    data object CloseLogSheet : ActiveWorkoutIntent
    data class Logged(
        val workoutExerciseId: String,
        val undo: com.himanshu.liftly.core.domain.model.LogSnapshot,
        val newRecord: Boolean = false,
    ) : ActiveWorkoutIntent

    /** Swipe right: log exactly the pre-filled values, zero extra taps. */
    data class QuickLog(val workoutExerciseId: String) : ActiveWorkoutIntent
    data object FinishRequested : ActiveWorkoutIntent
    data object FinishConfirmed : ActiveWorkoutIntent
    data object FinishCancelled : ActiveWorkoutIntent
    data object DiscardRequested : ActiveWorkoutIntent
    data object DiscardConfirmed : ActiveWorkoutIntent
    data object DiscardCancelled : ActiveWorkoutIntent
}

sealed interface ActiveWorkoutEffect {
    /** Undo-able change; the screen shows a snackbar with an Undo action. */
    data class Undoable(val kind: Kind, val exerciseName: String) : ActiveWorkoutEffect {
        enum class Kind { SKIPPED, REMOVED, LOGGED }
    }

    data class Closed(val finished: Boolean) : ActiveWorkoutEffect

    /** Scroll the list so this card is visible. */
    data class ScrollTo(val workoutExerciseId: String) : ActiveWorkoutEffect
}

internal fun ActiveWorkout.toUiState(
    previous: ActiveWorkoutUiState,
    unit: WeightUnit,
    lastSets: Map<String, List<DraftSet>>,
) = previous.copy(
    isLoading = false,
    workoutId = workout.id,
    name = workout.name,
    startedAt = workout.startedAt,
    cards = exercises.map { e ->
        ExerciseCardUi(
            workoutExerciseId = e.workoutExercise.id,
            exerciseId = e.exercise.id,
            name = e.exercise.name,
            imagePaths = e.exercise.imagePaths.toImmutableList(),
            primaryMuscle = e.exercise.primaryMuscle,
            equipment = e.exercise.equipment,
            status = e.workoutExercise.status,
            summary = if (e.workoutExercise.status == ExerciseStatus.LOGGED) SetFormatter.summary(e.sets.toDraftSets(), unit) else null,
            lastHint = lastSets[e.exercise.id]?.let { SetFormatter.hint(it, unit) },
            hasPr = e.personalRecords.isNotEmpty(),
        )
    }.toImmutableList(),
)
