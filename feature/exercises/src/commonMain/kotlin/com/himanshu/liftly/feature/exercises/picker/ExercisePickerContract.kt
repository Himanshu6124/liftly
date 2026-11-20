package com.himanshu.liftly.feature.exercises.picker

import androidx.compose.runtime.Immutable
import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.Exercise
import com.himanshu.liftly.core.domain.model.Muscle
import com.himanshu.liftly.core.domain.model.MuscleGroup
import com.himanshu.liftly.core.domain.model.NewCustomExercise
import com.himanshu.liftly.core.domain.model.TrackingType
import com.himanshu.liftly.feature.exercises.library.ExerciseRow
import com.himanshu.liftly.feature.exercises.library.toRow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Immutable
data class PickerSection(val kind: Kind, val letter: Char? = null, val rows: ImmutableList<ExerciseRow>) {
    enum class Kind { SUGGESTED, RECENT, FAVORITES, LETTER, RESULTS }

    val key: String get() = "${kind.name}-${letter ?: ""}"
}

@Immutable
data class ExercisePickerUiState(
    val muscleGroup: MuscleGroup? = null,
    val equipment: Equipment? = null,
    val sections: ImmutableList<PickerSection> = persistentListOf(),
    /** Selection order = the order exercises are added to the workout. */
    val selected: ImmutableList<String> = persistentListOf(),
    val isLoading: Boolean = true,
    val noResults: Boolean = false,
)

sealed interface ExercisePickerIntent {
    data class QueryChanged(val query: String) : ExercisePickerIntent
    data class MuscleGroupToggled(val group: MuscleGroup) : ExercisePickerIntent
    data class EquipmentToggled(val equipment: Equipment) : ExercisePickerIntent
    data class SelectionToggled(val exerciseId: String) : ExercisePickerIntent
    data class CreateCustom(val exercise: NewCustomExercise) : ExercisePickerIntent
}

/** Sensible defaults for the "Create '<query>'" form, taken from the active filters. */
internal fun customDefaults(query: String, group: MuscleGroup?, equipment: Equipment?) = NewCustomExercise(
    name = query.trim(),
    primaryMuscle = group?.muscles?.first() ?: Muscle.CHEST,
    equipment = equipment ?: Equipment.BARBELL,
    trackingType = if (equipment == Equipment.BODYWEIGHT) TrackingType.REPS_ONLY else TrackingType.WEIGHT_REPS,
)

/**
 * Browsing: Suggested → Recent → Favorites → All A–Z. Searching or filtering: ranked Results.
 * Suggested/Recent only make sense without filters, so they disappear while filtering.
 */
internal fun buildPickerSections(
    results: List<Exercise>,
    browsing: Boolean,
    suggested: List<Exercise>,
    recent: List<Exercise>,
): ImmutableList<PickerSection> {
    fun List<Exercise>.rows() = map { it.toRow() }.toImmutableList()
    if (!browsing) {
        return if (results.isEmpty()) {
            persistentListOf()
        } else {
            persistentListOf(
                PickerSection(PickerSection.Kind.RESULTS, rows = results.rows()),
            )
        }
    }
    val sections = mutableListOf<PickerSection>()
    if (suggested.isNotEmpty()) sections += PickerSection(PickerSection.Kind.SUGGESTED, rows = suggested.rows())
    if (recent.isNotEmpty()) sections += PickerSection(PickerSection.Kind.RECENT, rows = recent.rows())
    results.filter { it.isFavorite }.takeIf { it.isNotEmpty() }?.let {
        sections += PickerSection(PickerSection.Kind.FAVORITES, rows = it.rows())
    }
    results
        .groupBy { it.name.firstOrNull()?.uppercaseChar()?.takeIf(Char::isLetter) ?: '#' }
        .entries
        .sortedBy { it.key }
        .forEach { (letter, items) -> sections += PickerSection(PickerSection.Kind.LETTER, letter, items.rows()) }
    return sections.toImmutableList()
}
