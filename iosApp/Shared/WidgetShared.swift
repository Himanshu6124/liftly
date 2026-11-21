import Foundation

/// The App Group both the app and the widget extension read. Enable it for your team
/// (Signing & Capabilities) to run on a real device; the simulator doesn't check it.
enum WidgetShared {
    static let appGroup = "group.com.himanshu.liftly"
    static let snapshotKey = "widgetSnapshot"
    static let widgetKind = "LiftlyWeekWidget"
}
