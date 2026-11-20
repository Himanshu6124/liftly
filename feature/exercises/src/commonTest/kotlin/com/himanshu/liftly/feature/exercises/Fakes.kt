package com.himanshu.liftly.feature.exercises

import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.Exercise
import com.himanshu.liftly.core.domain.model.Muscle
import com.himanshu.liftly.core.domain.model.TrackingType
import com.himanshu.liftly.core.domain.repository.ExerciseRepository
import com.himanshu.liftly.core.platform.UrlLauncher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeExerciseRepository(initial: List<Exercise>) : ExerciseRepository {
    val exercises = MutableStateFlow(initial)
    val noteWrites = mutableListOf<String?>()

    override fun observeLibrary(): Flow<List<Exercise>> = exercises.map { all -> all.sortedBy { it.name } }
    override fun observeExercise(id: String): Flow<Exercise?> = exercises.map { all -> all.find { it.id == id } }
    override suspend fun setFavorite(id: String, favorite: Boolean) = exercises.update { all ->
        all.map { if (it.id == id) it.copy(isFavorite = favorite) else it }
    }
    override suspend fun setUserNote(id: String, note: String?) {
        noteWrites += note
        exercises.update { all -> all.map { if (it.id == id) it.copy(userNote = note) else it } }
    }
}

class FakeUrlLauncher : UrlLauncher {
    val opened = mutableListOf<String>()
    override fun open(url: String) {
        opened += url
    }
}

fun exercise(
    name: String,
    id: String = name.lowercase().replace(' ', '_'),
    primary: Muscle = Muscle.CHEST,
    equipment: Equipment = Equipment.BARBELL,
    favorite: Boolean = false,
    note: String? = null,
    aliases: List<String> = emptyList(),
) = Exercise(
    id = id, seedKey = id, name = name, aliases = aliases, primaryMuscle = primary, secondaryMuscles = emptyList(),
    equipment = equipment, trackingType = TrackingType.WEIGHT_REPS, mechanic = null, difficulty = null,
    imagePaths = listOf("files/exercises/$id/0.jpg"), videoUrl = null, instructions = listOf("Do it"),
    formCues = listOf("Cue"), commonMistakes = listOf("Mistake"), userNote = note, isFavorite = favorite,
    isCustom = false, isArchived = false,
)
