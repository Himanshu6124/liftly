package com.himanshu.liftly.feature.exercises

import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.Exercise
import com.himanshu.liftly.core.domain.model.Muscle
import com.himanshu.liftly.core.domain.model.NewCustomExercise
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
    override suspend fun createCustom(exercise: NewCustomExercise): String {
        val id = "custom-${exercises.value.size}"
        exercises.update { it + exercise(exercise.name, id = id, primary = exercise.primaryMuscle, equipment = exercise.equipment) }
        return id
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

class FakeRecentWorkoutRepository(recent: List<String> = emptyList()) : com.himanshu.liftly.core.domain.repository.WorkoutRepository {
    val recentIds = MutableStateFlow(recent)
    override fun observeLastFinished() = kotlinx.coroutines.flow.flowOf<com.himanshu.liftly.core.domain.model.WorkoutSummaryLine?>(null)
    override fun observeRecentExerciseIds(limit: Int): Flow<List<String>> = recentIds
    override fun observeActiveWorkout() = kotlinx.coroutines.flow.flowOf(null)
    override suspend fun getInProgressWorkout() = null
    override suspend fun createWorkout(name: String, routineId: String?, exerciseIds: List<String>) = "w"
    override suspend fun addExercises(workoutId: String, exerciseIds: List<String>) = Unit
    override suspend fun rename(workoutId: String, name: String) = Unit
    override suspend fun reorder(workoutId: String, orderedWorkoutExerciseIds: List<String>) = Unit
    override suspend fun setStatus(workoutExerciseId: String, status: com.himanshu.liftly.core.domain.model.ExerciseStatus) = Unit
    override suspend fun removeExercise(workoutExerciseId: String) = null
    override suspend fun restoreExercise(removed: com.himanshu.liftly.core.domain.model.RemovedWorkoutExercise) = Unit
    override suspend fun getLogSheetData(workoutExerciseId: String): com.himanshu.liftly.core.domain.model.LogSheetData? = null
    override suspend fun getLastSession(
        exerciseId: String,
        excludeWorkoutId: String,
    ): com.himanshu.liftly.core.domain.model.LastSession? = null
    override suspend fun logSets(
        workoutExerciseId: String,
        sets: List<com.himanshu.liftly.core.domain.model.DraftSet>,
        inputMode: com.himanshu.liftly.core.domain.model.InputMode,
        notes: String?,
        personalRecords: List<com.himanshu.liftly.core.domain.usecase.PrHit>,
    ): com.himanshu.liftly.core.domain.model.LogSnapshot = error("not used")
    override suspend fun getSessionHistory(exerciseId: String, excludeWorkoutExerciseId: String) =
        emptyList<List<com.himanshu.liftly.core.domain.model.WorkoutSet>>()
    override fun observeRecords(exerciseId: String) =
        kotlinx.coroutines.flow.flowOf(emptyList<com.himanshu.liftly.core.domain.model.PersonalRecord>())
    override suspend fun restoreLog(snapshot: com.himanshu.liftly.core.domain.model.LogSnapshot) = Unit
    override suspend fun finish(workoutId: String) = Unit
    override suspend fun discard(workoutId: String) = Unit
}

class FakeSettings : com.himanshu.liftly.core.domain.repository.SettingsRepository {
    override val settings = MutableStateFlow(com.himanshu.liftly.core.domain.model.UserSettings())
    override suspend fun update(
        transform: (com.himanshu.liftly.core.domain.model.UserSettings) -> com.himanshu.liftly.core.domain.model.UserSettings,
    ) {
        settings.value = transform(settings.value)
    }
    override suspend fun seedVersion() = 1
    override suspend fun setSeedVersion(version: Int) = Unit
}
