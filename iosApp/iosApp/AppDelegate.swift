import BackgroundTasks
import ComposeApp
import UIKit

/// Links into the app: home-screen Quick Actions (their type is the deep link to run) and liftly://
/// URLs from widgets and the Live Activity. Everything goes to `DeepLinks` in Kotlin.
/// Also runs background sync (`BackgroundSync`).
final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        BackgroundSync.register()
        return true
    }

    func application(
        _ application: UIApplication,
        configurationForConnecting connectingSceneSession: UISceneSession,
        options: UIScene.ConnectionOptions
    ) -> UISceneConfiguration {
        let config = UISceneConfiguration(name: nil, sessionRole: connectingSceneSession.role)
        config.delegateClass = LinkSceneDelegate.self
        return config
    }
}

final class LinkSceneDelegate: NSObject, UIWindowSceneDelegate {
    // Cold start from a Quick Action or a link.
    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options: UIScene.ConnectionOptions) {
        if let item = options.shortcutItem { Self.handle(item.type) }
        options.urlContexts.forEach { Self.handle($0.url.absoluteString) }
    }

    // Quick Action while the app is running.
    func windowScene(
        _ windowScene: UIWindowScene,
        performActionFor shortcutItem: UIApplicationShortcutItem,
        completionHandler: @escaping (Bool) -> Void
    ) {
        completionHandler(Self.handle(shortcutItem.type))
    }

    // Link while the app is running.
    func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
        URLContexts.forEach { Self.handle($0.url.absoluteString) }
    }

    func sceneDidEnterBackground(_ scene: UIScene) {
        BackgroundSync.schedule()
    }

    @discardableResult
    private static func handle(_ url: String) -> Bool {
        let handled = DeepLinks.shared.handle(url: url)
        NSLog("Liftly link \(url) handled=\(handled)")
        return handled
    }
}

/// Syncs in the background through `BGAppRefreshTask`, which iOS runs when it sees fit (usage
/// patterns, battery, network). We ask for at most every 6 hours; it's a no-op while signed out.
enum BackgroundSync {
    static let identifier = "com.himanshu.liftly.sync"
    private static let interval: TimeInterval = 6 * 60 * 60

    /// Must run before the app finishes launching.
    static func register() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: identifier, using: nil) { task in
            guard let task = task as? BGAppRefreshTask else { return }
            handle(task)
        }
    }

    static func schedule() {
        let request = BGAppRefreshTaskRequest(identifier: identifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: interval)
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            NSLog("Liftly background sync not scheduled: \(error)")
        }
    }

    private static func handle(_ task: BGAppRefreshTask) {
        schedule() // keep the chain going
        let completion = Completion(task)
        task.expirationHandler = { completion.finish(success: false) }
        // Kotlin suspend functions are called from the main thread.
        DispatchQueue.main.async {
            IosStartupKt.runBackgroundSync { ok, _ in completion.finish(success: ok?.boolValue ?? false) }
        }
    }

    /// iOS requires exactly one setTaskCompleted call, whether the sync or the expiry comes first.
    private final class Completion {
        private let task: BGAppRefreshTask
        private var done = false
        init(_ task: BGAppRefreshTask) { self.task = task }

        func finish(success: Bool) {
            DispatchQueue.main.async {
                guard !self.done else { return }
                self.done = true
                self.task.setTaskCompleted(success: success)
            }
        }
    }
}
