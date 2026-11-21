package com.himanshu.liftly.feature.workout.ongoing

import com.himanshu.liftly.core.designsystem.component.workoutDisplayNameText
import com.himanshu.liftly.core.domain.model.ActiveWorkout
import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.core.domain.repository.WorkoutRepository
import com.himanshu.liftly.core.domain.util.AppScope
import com.himanshu.liftly.core.domain.util.TimeProvider
import com.himanshu.liftly.core.platform.OngoingWorkout
import com.himanshu.liftly.core.platform.OngoingWorkoutIndicator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

/**
 * Mirrors the in-progress workout into the system UI (Android ongoing notification, iOS Live
 * Activity) for as long as the app process lives. Started once at app launch, in [AppScope].
 * A rest that runs out is dropped from the indicator at its end time even if the app never clears it.
 */
class OngoingWorkoutPublisher(
    private val workouts: WorkoutRepository,
    private val indicator: OngoingWorkoutIndicator,
    private val appScope: AppScope,
    private val time: TimeProvider,
    private val titleOf: suspend (ActiveWorkout) -> String = { workoutDisplayNameText(it.workout.name, it.workout.startedAt) },
) {
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job = appScope.launch {
            states().collect { state -> if (state == null) indicator.end() else indicator.show(state) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    internal fun states(): Flow<OngoingWorkout?> = workouts.observeActiveWorkout()
        .transformLatest { active ->
            if (active == null) {
                emit(null)
                return@transformLatest
            }
            val state = active.toOngoing(titleOf(active))
            val restEnds = state.restEndsAtMillis
            val now = time.now().toEpochMilliseconds()
            if (restEnds == null || restEnds <= now) {
                emit(state.copy(restEndsAtMillis = null))
            } else {
                emit(state)
                delay(restEnds - now)
                emit(state.copy(restEndsAtMillis = null))
            }
        }
        .distinctUntilChanged()

    internal companion object {
        fun ActiveWorkout.toOngoing(title: String) = OngoingWorkout(
            title = title,
            startedAtMillis = workout.startedAt.toEpochMilliseconds(),
            loggedCount = exercises.count { it.workoutExercise.status == ExerciseStatus.LOGGED },
            totalCount = exercises.size,
            nextExercise = exercises.firstOrNull { it.workoutExercise.status == ExerciseStatus.PENDING }?.exercise?.name,
            restEndsAtMillis = workout.restEndsAt?.toEpochMilliseconds(),
        )
    }
}
