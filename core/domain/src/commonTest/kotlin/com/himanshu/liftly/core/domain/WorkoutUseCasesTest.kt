package com.himanshu.liftly.core.domain

import com.himanshu.liftly.core.domain.model.Muscle
import com.himanshu.liftly.core.domain.usecase.StartWorkoutUseCase
import com.himanshu.liftly.core.domain.usecase.SuggestExercises
import com.himanshu.liftly.core.domain.util.TimeOfDay
import com.himanshu.liftly.core.domain.util.timeOfDay
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WorkoutUseCasesTest {
    @Test
    fun startCreatesWhenNothingInProgress() = runTest {
        val repo = FakeWorkoutRepository()
        val result = StartWorkoutUseCase(repo)(exerciseIds = listOf("bench"))
        assertIs<StartWorkoutUseCase.Result.Started>(result)
        assertEquals(1, repo.created.size)
    }

    @Test
    fun startResumesInsteadOfCreatingASecondWorkout() = runTest {
        val repo = FakeWorkoutRepository()
        val first = StartWorkoutUseCase(repo)()
        val second = StartWorkoutUseCase(repo)()
        assertIs<StartWorkoutUseCase.Result.Resumed>(second)
        assertEquals(first.workoutId, second.workoutId)
        assertEquals(1, repo.created.size)
    }

    @Test
    fun suggestionsShareMusclesAndExcludeAdded() {
        val library = listOf(
            exercise("Bench Press", id = "bench", primary = Muscle.CHEST),
            exercise("Incline Press", id = "incline", primary = Muscle.CHEST),
            exercise("Cable Fly", id = "fly", primary = Muscle.CHEST).copy(isFavorite = true),
            exercise("Squat", id = "squat", primary = Muscle.QUADS),
        )
        val suggested = SuggestExercises.suggest(library, inWorkout = listOf("bench")).map { it.id }
        assertEquals(listOf("fly", "incline"), suggested) // favorite first
        assertEquals(emptyList(), SuggestExercises.suggest(library, inWorkout = emptyList()))
    }

    @Test
    fun timeOfDayBuckets() {
        fun at(h: Int) = LocalDateTime(2026, 1, 1, h, 0).toInstant(TimeZone.UTC)
        assertEquals(TimeOfDay.MORNING, timeOfDay(at(7), TimeZone.UTC))
        assertEquals(TimeOfDay.AFTERNOON, timeOfDay(at(13), TimeZone.UTC))
        assertEquals(TimeOfDay.EVENING, timeOfDay(at(19), TimeZone.UTC))
        assertEquals(TimeOfDay.NIGHT, timeOfDay(at(2), TimeZone.UTC))
    }
}
