package com.himanshu.liftly.core.platform

/**
 * ActivityKit (Live Activities) is Swift-only, so the iOS app registers a Swift implementation here at
 * startup (`OngoingWorkoutIndicatorHolder.shared.delegate = ...`). The last state is replayed when the
 * delegate arrives, so registration order doesn't matter.
 */
object OngoingWorkoutIndicatorHolder : OngoingWorkoutIndicator {
    private var last: OngoingWorkout? = null

    var delegate: OngoingWorkoutIndicator? = null
        set(value) {
            field = value
            last?.let { value?.show(it) }
        }

    override fun show(workout: OngoingWorkout) {
        last = workout
        delegate?.show(workout)
    }

    override fun end() {
        last = null
        delegate?.end()
    }
}
