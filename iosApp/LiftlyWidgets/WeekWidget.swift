import SwiftUI
import WidgetKit

/// Mirror of the Kotlin `WidgetSnapshot` (line-based text written by the app).
struct WeekSnapshot {
    var thisWeekCount: Int
    var weeklyGoal: Int
    var streakWeeks: Int
    var weekStart: Date
    var nextRoutine: String?
    var workoutInProgress: Bool

    static func decode(_ text: String?) -> WeekSnapshot? {
        guard let parts = text?.components(separatedBy: "\n"), parts.count == 6,
              let count = Int(parts[0]), let goal = Int(parts[1]), let streak = Int(parts[2]),
              let start = Double(parts[3]) else { return nil }
        return WeekSnapshot(
            thisWeekCount: count, weeklyGoal: goal, streakWeeks: streak,
            weekStart: Date(timeIntervalSince1970: start / 1000),
            nextRoutine: parts[4].isEmpty ? nil : parts[4],
            workoutInProgress: parts[5] == "true"
        )
    }

    var nextWeekStart: Date { weekStart.addingTimeInterval(7 * 24 * 3600) }

    /// How it reads once a new week starts before the app refreshes it.
    func rolledOver() -> WeekSnapshot {
        var s = self
        s.streakWeeks = thisWeekCount >= weeklyGoal ? streakWeeks : 0
        s.thisWeekCount = 0
        s.weekStart = nextWeekStart
        return s
    }

    static func load() -> WeekSnapshot? {
        decode(UserDefaults(suiteName: WidgetShared.appGroup)?.string(forKey: WidgetShared.snapshotKey))
    }
}

struct WeekEntry: TimelineEntry {
    let date: Date
    let snapshot: WeekSnapshot?
}

struct WeekProvider: TimelineProvider {
    func placeholder(in context: Context) -> WeekEntry {
        WeekEntry(date: Date(), snapshot: WeekSnapshot(
            thisWeekCount: 2, weeklyGoal: 3, streakWeeks: 4, weekStart: Date(), nextRoutine: "Push", workoutInProgress: false
        ))
    }

    func getSnapshot(in context: Context, completion: @escaping (WeekEntry) -> Void) {
        completion(context.isPreview ? placeholder(in: context) : WeekEntry(date: Date(), snapshot: WeekSnapshot.load()))
    }

    /// The app reloads the timeline on every change; the second entry rolls over to a new week on its own.
    func getTimeline(in context: Context, completion: @escaping (Timeline<WeekEntry>) -> Void) {
        let now = Date()
        guard var snapshot = WeekSnapshot.load() else {
            completion(Timeline(entries: [WeekEntry(date: now, snapshot: nil)], policy: .never))
            return
        }
        while now >= snapshot.nextWeekStart { snapshot = snapshot.rolledOver() }
        let rolled = snapshot.rolledOver()
        completion(Timeline(
            entries: [WeekEntry(date: now, snapshot: snapshot), WeekEntry(date: snapshot.nextWeekStart, snapshot: rolled)],
            policy: .after(rolled.nextWeekStart)
        ))
    }
}

struct WeekWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: WidgetShared.widgetKind, provider: WeekProvider()) { entry in
            WeekWidgetView(snapshot: entry.snapshot)
        }
        .configurationDisplayName("Liftly week")
        .description("Weekly goal, streak and a button to start your next routine.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

private struct WeekWidgetView: View {
    let snapshot: WeekSnapshot?
    @Environment(\.widgetFamily) private var family

    private var link: URL {
        URL(string: snapshot?.workoutInProgress == true ? "liftly://workout" : "liftly://start-next-workout")!
    }

    private var buttonLabel: String {
        if snapshot?.workoutInProgress == true { return "Resume workout" }
        if let next = snapshot?.nextRoutine { return "Start \(next)" }
        return "Start workout"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("THIS WEEK").font(.caption2.weight(.bold)).foregroundStyle(LiftlyColors.yellow)
            if let s = snapshot {
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text("\(s.thisWeekCount)/\(s.weeklyGoal)").font(.title.weight(.bold).monospacedDigit())
                        .foregroundStyle(.white)
                    if family != .systemSmall { Text(streakLabel(s.streakWeeks)).font(.caption).foregroundStyle(.gray) }
                }
                if family == .systemSmall { Text(streakLabel(s.streakWeeks)).font(.caption2).foregroundStyle(.gray) }
                Plates(done: min(s.thisWeekCount, s.weeklyGoal), goal: s.weeklyGoal)
                    .accessibilityLabel("\(s.thisWeekCount) of \(s.weeklyGoal) workouts this week")
            } else {
                Text("Open Liftly to set up").font(.footnote).foregroundStyle(.white)
            }
            Spacer(minLength: 0)
            Link(destination: link) {
                Text(buttonLabel)
                    .font(.footnote.weight(.bold)).lineLimit(1).minimumScaleFactor(0.8)
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity, minHeight: 34)
                    .background(LiftlyColors.red, in: Capsule())
            }
        }
        .widgetURL(link)
        .widgetBackground(LiftlyColors.graphite)
    }

    private func streakLabel(_ weeks: Int) -> String {
        switch weeks {
        case 0: return "No streak yet"
        case 1: return "1-week streak"
        default: return "\(weeks)-week streak"
        }
    }
}

private struct Plates: View {
    let done: Int
    let goal: Int
    private let colors = [LiftlyColors.red, LiftlyColors.blue, LiftlyColors.yellow, LiftlyColors.green]

    var body: some View {
        HStack(spacing: 4) {
            ForEach(0..<max(goal, 1), id: \.self) { i in
                Capsule().fill(i < done ? colors[i % colors.count] : Color.white.opacity(0.12)).frame(height: 8)
            }
        }
    }
}

private extension View {
    /// `containerBackground` on iOS 17+, a plain background before.
    @ViewBuilder func widgetBackground(_ color: Color) -> some View {
        if #available(iOS 17.0, *) {
            containerBackground(color, for: .widget)
        } else {
            padding().background(color)
        }
    }
}
