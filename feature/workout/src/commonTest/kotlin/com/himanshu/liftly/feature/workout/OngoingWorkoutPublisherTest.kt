package com.himanshu.liftly.feature.workout

import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.core.domain.util.AppScope
import com.himanshu.liftly.core.domain.util.TimeProvider
import com.himanshu.liftly.core.platform.OngoingWorkout
import com.himanshu.liftly.core.platform.OngoingWorkoutIndicator
import com.himanshu.liftly.feature.workout.ongoing.OngoingWorkoutPublisher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class OngoingWorkoutPublisherTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val clock = TimeProvider { Instant.fromEpochMilliseconds(dispatcher.scheduler.currentTime) }

    private class RecordingIndicator : OngoingWorkoutIndicator {
        var shown: OngoingWorkout? = null
        var ended = 0
        override fun show(workout: OngoingWorkout) {
            shown = workout
        }
        override fun end() {
            shown = null
            ended++
        }
    }

    @Test
    fun mirrorsProgressRestAndEnd() = runTest(dispatcher) {
        val repo = FakeWorkoutRepository(listOf("Bench", "Row", "Curl"))
        val indicator = RecordingIndicator()
        val scope = CoroutineScope(dispatcher)
        OngoingWorkoutPublisher(repo, indicator, AppScope(scope), clock) { "Push day" }.start()

        assertEquals(OngoingWorkout("Push day", 0, 0, 3, "Bench", null), indicator.shown)

        repo.setStatus("we0", ExerciseStatus.LOGGED)
        repo.setStatus("we1", ExerciseStatus.SKIPPED)
        assertEquals(1, indicator.shown?.loggedCount)
        assertEquals("Curl", indicator.shown?.nextExercise)

        repo.setRestTimer("w1", Instant.fromEpochMilliseconds(90_000))
        assertEquals(90_000L, indicator.shown?.restEndsAtMillis)
        // The rest runs out without the app clearing it: the indicator drops it on time.
        advanceTimeBy(90_001)
        assertNull(indicator.shown?.restEndsAtMillis)
        assertEquals("Push day", indicator.shown?.title)

        repo.active.value = null
        assertNull(indicator.shown)
        assertEquals(1, indicator.ended)
        scope.cancel()
    }
}
