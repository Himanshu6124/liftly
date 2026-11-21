import ActivityKit
import ComposeApp
import Foundation

/// Implements the Kotlin `OngoingWorkoutIndicator` with a Live Activity (lock screen + Dynamic Island).
/// Registered at startup through `OngoingWorkoutIndicatorHolder`. Does nothing below iOS 16.1 or
/// when the user turned Live Activities off for Liftly.
final class LiveActivityIndicator: NSObject, OngoingWorkoutIndicator {
    func show(workout: OngoingWorkout) {
        guard #available(iOS 16.1, *) else { return }
        let state = WorkoutActivityAttributes.ContentState(
            title: workout.title,
            startedAt: Date(timeIntervalSince1970: Double(workout.startedAtMillis) / 1000),
            loggedCount: Int(workout.loggedCount),
            totalCount: Int(workout.totalCount),
            nextExercise: workout.nextExercise,
            restEndsAt: workout.restEndsAtMillis.map { Date(timeIntervalSince1970: Double($0.int64Value) / 1000) }
        )
        DispatchQueue.main.async { Self.apply(state) }
    }

    func end() {
        guard #available(iOS 16.1, *) else { return }
        Task {
            for activity in Activity<WorkoutActivityAttributes>.activities {
                await activity.end(dismissalPolicy: .immediate)
            }
        }
    }

    @available(iOS 16.1, *)
    private static func apply(_ state: WorkoutActivityAttributes.ContentState) {
        if let activity = Activity<WorkoutActivityAttributes>.activities.first {
            Task { await activity.update(using: state) }
            return
        }
        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }
        do {
            _ = try Activity.request(attributes: WorkoutActivityAttributes(), contentState: state, pushType: nil)
        } catch {
            print("Live Activity not started: \(error)")
        }
    }
}
