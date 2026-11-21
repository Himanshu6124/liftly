import ComposeApp
import SwiftUI

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    init() {
        // Swift-only system services the shared Kotlin code drives. Set before Koin starts.
        SecureStoreHolder.shared.delegate = KeychainStore()
        // Only with the capability (SIGN_IN_WITH_APPLE in Config.xcconfig; needs a paid team).
        if Bundle.main.object(forInfoDictionaryKey: "LiftlySignInWithApple") as? String == "YES" {
            AppleSignInBridgeHolder.shared.delegate = AppleSignIn()
        }
        GoogleSignInBridgeHolder.shared.delegate = GoogleSignIn()
        OngoingWorkoutIndicatorHolder.shared.delegate = LiveActivityIndicator()
        HomeWidgetBridgeHolder.shared.delegate = AppGroupWidgetBridge()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
