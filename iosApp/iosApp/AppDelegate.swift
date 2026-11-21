import ComposeApp
import UIKit

/// Links into the app: home-screen Quick Actions (their type is the deep link to run) and liftly://
/// URLs from widgets and the Live Activity. Everything goes to `DeepLinks` in Kotlin.
final class AppDelegate: NSObject, UIApplicationDelegate {
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

    @discardableResult
    private static func handle(_ url: String) -> Bool {
        let handled = DeepLinks.shared.handle(url: url)
        NSLog("Liftly link \(url) handled=\(handled)")
        return handled
    }
}
