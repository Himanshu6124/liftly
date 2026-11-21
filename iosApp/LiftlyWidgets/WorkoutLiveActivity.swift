import ActivityKit
import SwiftUI
import WidgetKit

/// Lock screen banner and Dynamic Island for the workout in progress. Elapsed time (or the rest
/// countdown) is drawn by the system from dates, so it keeps ticking without app updates.
struct WorkoutLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: WorkoutActivityAttributes.self) { context in
            LockScreenView(state: context.state)
                .activityBackgroundTint(LiftlyColors.graphite)
                .activitySystemActionForegroundColor(.white)
                .widgetURL(URL(string: "liftly://workout"))
        } dynamicIsland: { context in
            let state = context.state
            return DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Label("\(state.loggedCount)/\(state.totalCount)", systemImage: "dumbbell.fill")
                        .font(.headline.monospacedDigit())
                        .foregroundStyle(LiftlyColors.red)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    TimerText(state: state)
                        .font(.title3.weight(.bold).monospacedDigit())
                        .multilineTextAlignment(.trailing)
                        .frame(maxWidth: 90)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(state.title).font(.subheadline.weight(.semibold)).lineLimit(1)
                        Text(NextLine.text(state)).font(.caption).foregroundStyle(.secondary).lineLimit(1)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            } compactLeading: {
                Image(systemName: state.restEndsAt != nil ? "hourglass" : "dumbbell.fill")
                    .foregroundStyle(state.restEndsAt != nil ? LiftlyColors.yellow : LiftlyColors.red)
            } compactTrailing: {
                TimerText(state: state)
                    .font(.caption.monospacedDigit())
                    .frame(maxWidth: 48)
            } minimal: {
                Image(systemName: state.restEndsAt != nil ? "hourglass" : "dumbbell.fill")
                    .foregroundStyle(LiftlyColors.red)
            }
            .widgetURL(URL(string: "liftly://workout"))
        }
    }
}

private struct LockScreenView: View {
    let state: WorkoutActivityAttributes.ContentState

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(state.restEndsAt != nil ? "RESTING" : "IN PROGRESS")
                    .font(.caption2.weight(.bold))
                    .foregroundStyle(LiftlyColors.yellow)
                Text(state.title)
                    .font(.headline)
                    .foregroundStyle(.white)
                    .lineLimit(1)
                Text(NextLine.text(state))
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.7))
                    .lineLimit(1)
            }
            Spacer(minLength: 8)
            VStack(alignment: .trailing, spacing: 4) {
                TimerText(state: state)
                    .font(.title.weight(.bold).monospacedDigit())
                    .foregroundStyle(state.restEndsAt != nil ? LiftlyColors.yellow : .white)
                    .multilineTextAlignment(.trailing)
                    .frame(maxWidth: 120)
                Text("\(state.loggedCount) of \(state.totalCount) logged")
                    .font(.caption.monospacedDigit())
                    .foregroundStyle(LiftlyColors.green)
            }
        }
        .padding(16)
    }
}

/// Counts down to the end of rest, otherwise up from the workout start.
private struct TimerText: View {
    let state: WorkoutActivityAttributes.ContentState

    var body: some View {
        if let restEndsAt = state.restEndsAt, restEndsAt > Date() {
            Text(timerInterval: Date()...restEndsAt, countsDown: true)
        } else {
            Text(state.startedAt, style: .timer)
        }
    }
}

private enum NextLine {
    static func text(_ state: WorkoutActivityAttributes.ContentState) -> String {
        if let next = state.nextExercise { return "Next: \(next)" }
        return "All exercises done"
    }
}
