package com.himanshu.liftly.feature.workout

import com.himanshu.liftly.core.domain.model.UserSettings
import com.himanshu.liftly.core.domain.model.WorkoutSet
import com.himanshu.liftly.core.domain.repository.SettingsRepository
import com.himanshu.liftly.core.platform.Haptics
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettings(initial: UserSettings = UserSettings()) : SettingsRepository {
    override val settings = MutableStateFlow(initial)
    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        settings.value = transform(settings.value)
    }
    override suspend fun seedVersion() = 1
    override suspend fun setSeedVersion(version: Int) = Unit
}

class CountingHaptics : Haptics {
    var light = 0
    var success = 0
    override fun light() {
        light++
    }
    override fun success() {
        success++
    }
}

class RecordingRestAlarm : com.himanshu.liftly.feature.workout.rest.RestAlarm {
    var scheduledFor: kotlin.time.Instant? = null
    var cancels = 0
    override fun schedule(endsAt: kotlin.time.Instant) {
        scheduledFor = endsAt
    }
    override fun cancel() {
        scheduledFor = null
        cancels++
    }
}

class FakeExercises : com.himanshu.liftly.core.domain.repository.ExerciseRepository {
    val restSeconds = mutableMapOf<String, Int>()
    override fun observeLibrary() = kotlinx.coroutines.flow.flowOf(emptyList<com.himanshu.liftly.core.domain.model.Exercise>())
    override fun observeExercise(id: String) = kotlinx.coroutines.flow.flowOf<com.himanshu.liftly.core.domain.model.Exercise?>(null)
    override suspend fun setFavorite(id: String, favorite: Boolean) = Unit
    override suspend fun setUserNote(id: String, note: String?) = Unit
    override suspend fun setRestSeconds(id: String, seconds: Int) {
        restSeconds[id] = seconds
    }
    override suspend fun createCustom(exercise: com.himanshu.liftly.core.domain.model.NewCustomExercise) = "custom"
}

fun sets(vararg pairs: Pair<Double?, Int>) = pairs.mapIndexed { i, (w, r) ->
    WorkoutSet("old$i", "oldwe", i, w, r, null, null, null, com.himanshu.liftly.core.domain.model.SetType.NORMAL)
}

class FakeRoutines : com.himanshu.liftly.core.domain.repository.RoutineRepository {
    val savedFromWorkout = mutableListOf<Pair<String, String>>()
    override fun observeRoutines() = kotlinx.coroutines.flow.flowOf(emptyList<com.himanshu.liftly.core.domain.model.RoutineWithExercises>())
    override fun observeRoutine(
        id: String,
    ) = kotlinx.coroutines.flow.flowOf<com.himanshu.liftly.core.domain.model.RoutineWithExercises?>(null)
    override suspend fun saveRoutine(
        id: String?,
        name: String,
        notes: String?,
        exercises: List<com.himanshu.liftly.core.domain.model.RoutineExerciseDraft>,
    ) = "r"
    override suspend fun deleteRoutine(id: String) = null
    override suspend fun restoreRoutine(snapshot: com.himanshu.liftly.core.domain.model.RoutineWithExercises) = Unit
    override suspend fun reorderRoutines(orderedIds: List<String>) = Unit
    override suspend fun saveWorkoutAsRoutine(workoutId: String, name: String): String {
        savedFromWorkout += workoutId to name
        return "r"
    }
    override suspend fun updateTargetsFromWorkout(routineId: String, workoutId: String) = Unit
}
