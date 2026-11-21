import ComposeApp
import SwiftUI

@main
struct iOSApp: App {
    init() {
        // Swift-only system UI the shared Kotlin code drives.
        OngoingWorkoutIndicatorHolder.shared.delegate = LiveActivityIndicator()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
