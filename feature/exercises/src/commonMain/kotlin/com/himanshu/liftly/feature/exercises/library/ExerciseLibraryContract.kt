package com.himanshu.liftly.feature.exercises.library

import androidx.compose.runtime.Immutable
import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.Exercise
import com.himanshu.liftly.core.domain.model.Muscle
import com.himanshu.liftly.core.domain.model.MuscleGroup
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Immutable
data class ExerciseRow(
    val id: String,
    val name: String,
    val primaryMuscle: Muscle,
    val equipment: Equipment,
    val imagePaths: ImmutableList<String>,
    val isFavorite: Boolean,
)

@Immutable
data class LibrarySection(
    /** null = a letter section (header is the letter). */
    val kind: Kind,
    val letter: Char? = null,
    val rows: ImmutableList<ExerciseRow>,
) {
    enum class Kind { FAVORITES, RESULTS, LETTER }

    val key: String get() = "${kind.name}-${letter ?: ""}"
}

@Immutable
data class ExerciseLibraryUiState(
    val query: String = "",
    val muscleGroup: MuscleGroup? = null,
    val equipment: Equipment? = null,
    val sections: ImmutableList<LibrarySection> = persistentListOf(),
    val isLoading: Boolean = true,
) {
    val hasFilters: Boolean get() = query.isNotBlank() || muscleGroup != null || equipment != null
    val isEmpty: Boolean get() = !isLoading && sections.isEmpty()
}

sealed interface ExerciseLibraryIntent {
    data class QueryChanged(val query: String) : ExerciseLibraryIntent
    data class MuscleGroupToggled(val group: MuscleGroup) : ExerciseLibraryIntent
    data class EquipmentToggled(val equipment: Equipment) : ExerciseLibraryIntent
    data class FavoriteToggled(val id: String, val favorite: Boolean) : ExerciseLibraryIntent
    data object ClearFilters : ExerciseLibraryIntent
}

internal fun Exercise.toRow() = ExerciseRow(
    id = id,
    name = name,
    primaryMuscle = primaryMuscle,
    equipment = equipment,
    imagePaths = imagePaths.toImmutableList(),
    isFavorite = isFavorite,
)

/**
 * Browsing (no query, no filters): Favorites first, then all exercises grouped A–Z.
 * Searching or filtering: one ranked "Results" section.
 */
internal fun buildSections(results: List<Exercise>, browsing: Boolean): ImmutableList<LibrarySection> {
    if (results.isEmpty()) return persistentListOf()
    if (!browsing) {
        return persistentListOf(LibrarySection(LibrarySection.Kind.RESULTS, rows = results.map { it.toRow() }.toImmutableList()))
    }
    val favorites = results.filter { it.isFavorite }
    val letters = results
        .groupBy { it.name.firstOrNull()?.uppercaseChar()?.takeIf(Char::isLetter) ?: '#' }
        .entries
        .sortedBy { it.key }
        .map { (letter, items) -> LibrarySection(LibrarySection.Kind.LETTER, letter, items.map { it.toRow() }.toImmutableList()) }
    val favoriteSection = if (favorites.isEmpty()) {
        emptyList()
    } else {
        listOf(LibrarySection(LibrarySection.Kind.FAVORITES, rows = favorites.map { it.toRow() }.toImmutableList()))
    }
    return (favoriteSection + letters).toImmutableList()
}
