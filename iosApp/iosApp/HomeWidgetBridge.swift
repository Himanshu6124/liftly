import ComposeApp
import Foundation
import WidgetKit

/// Implements the Kotlin `HomeWidgetBridge`: stores the snapshot in the App Group and reloads the widget.
final class AppGroupWidgetBridge: NSObject, HomeWidgetBridge {
    func update(snapshot: WidgetSnapshot) {
        UserDefaults(suiteName: WidgetShared.appGroup)?.set(snapshot.encode(), forKey: WidgetShared.snapshotKey)
        WidgetCenter.shared.reloadTimelines(ofKind: WidgetShared.widgetKind)
    }
}
