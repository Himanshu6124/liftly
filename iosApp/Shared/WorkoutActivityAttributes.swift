import ActivityKit
import Foundation
import SwiftUI

/// Shared by the app (starts/updates the Live Activity) and the widget extension (draws it).
@available(iOS 16.1, *)
struct WorkoutActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        var title: String
        var startedAt: Date
        var loggedCount: Int
        var totalCount: Int
        var nextExercise: String?
        /// Set while a rest is running: the Live Activity counts down to it.
        var restEndsAt: Date?
    }
}

/// Plate colors from the app's "Calibrated plates" palette.
enum LiftlyColors {
    static let red = Color(red: 0xE0 / 255, green: 0x26 / 255, blue: 0x2C / 255)
    static let yellow = Color(red: 0xF2 / 255, green: 0xC1 / 255, blue: 0x2E / 255)
    static let green = Color(red: 0x1F / 255, green: 0xA8 / 255, blue: 0x63 / 255)
    static let graphite = Color(red: 0x16 / 255, green: 0x18 / 255, blue: 0x1C / 255)
}
