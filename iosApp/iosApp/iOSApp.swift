import ComposeApp
import SwiftUI

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    init() {
        // Swift-only system services the shared Kotlin code drives. Set before Koin starts.
        SecureStoreHolder.shared.delegate = KeychainStore()
        AppleSignInBridgeHolder.shared.delegate = AppleSignIn()
        OngoingWorkoutIndicatorHolder.shared.delegate = LiveActivityIndicator()
        HomeWidgetBridgeHolder.shared.delegate = AppGroupWidgetBridge()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
