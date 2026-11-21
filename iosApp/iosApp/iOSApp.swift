import ComposeApp
import SwiftUI

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    init() {
        // Swift-only system UI the shared Kotlin code drives.
        OngoingWorkoutIndicatorHolder.shared.delegate = LiveActivityIndicator()
        HomeWidgetBridgeHolder.shared.delegate = AppGroupWidgetBridge()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
