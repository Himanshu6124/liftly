package com.himanshu.liftly.core.platform

/**
 * What the system-level "workout in progress" UI shows (Android ongoing notification, iOS Live Activity).
 * Times are epoch millis so the type is easy to read from Swift.
 */
data class OngoingWorkout(
    val title: String,
    val startedAtMillis: Long,
    val loggedCount: Int,
    val totalCount: Int,
    /** First exercise not logged yet, or null when everything is done. */
    val nextExercise: String?,
    /** When the running rest ends; null while no rest is running. */
    val restEndsAtMillis: Long?,
)

/** Shows, updates and removes the out-of-app workout indicator. Calls are idempotent. */
interface OngoingWorkoutIndicator {
    fun show(workout: OngoingWorkout)
    fun end()
}

object NoOngoingWorkoutIndicator : OngoingWorkoutIndicator {
    override fun show(workout: OngoingWorkout) = Unit
    override fun end() = Unit
}
