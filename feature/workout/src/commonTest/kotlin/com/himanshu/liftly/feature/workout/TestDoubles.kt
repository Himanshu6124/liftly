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
    override fun light() {
        light++
    }
    override fun success() = Unit
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
