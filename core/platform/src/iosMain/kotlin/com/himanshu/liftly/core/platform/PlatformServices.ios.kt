package com.himanshu.liftly.core.platform

import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.time.Clock
import kotlin.time.Instant

private class IosHaptics : Haptics {
    override fun light() = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight).impactOccurred()
    override fun success() = UINotificationFeedbackGenerator().notificationOccurred(
        UINotificationFeedbackType.UINotificationFeedbackTypeSuccess,
    )
}

/**
 * Local notifications. Permission is asked the first time something is scheduled (the first rest
 * timer), not at launch. While the app is in the foreground iOS doesn't show it; the app buzzes instead.
 */
private class IosNotifier : Notifier {
    private val center get() = UNUserNotificationCenter.currentNotificationCenter()

    override fun schedule(id: String, at: Instant, title: String, body: String) {
        val seconds = (at - Clock.System.now()).inWholeMilliseconds / MILLIS_PER_SECOND
        if (seconds <= 0) return
        center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { granted, _ ->
            if (!granted) return@requestAuthorizationWithOptions
            val content = UNMutableNotificationContent().apply {
                setTitle(title)
                setBody(body)
                setSound(UNNotificationSound.defaultSound)
            }
            val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(seconds, repeats = false)
            center.addNotificationRequest(UNNotificationRequest.requestWithIdentifier(id, content, trigger), withCompletionHandler = null)
        }
    }

    override fun cancel(id: String) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(id))
        center.removeDeliveredNotificationsWithIdentifiers(listOf(id))
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000.0
    }
}

private class IosUrlLauncher : UrlLauncher {
    override fun open(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
    }
}

private class IosShareService : ShareService {
    override fun shareText(text: String) = present(listOf(text))
    override fun shareFile(path: String, mime: String) = present(listOf(NSURL.fileURLWithPath(path)))

    private fun present(items: List<Any>) {
        val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        root.presentViewController(UIActivityViewController(items, null), animated = true, completion = null)
    }
}

actual val platformServicesModule: Module = module {
    single<Haptics> { IosHaptics() }
    single<Notifier> { IosNotifier() }
    single<UrlLauncher> { IosUrlLauncher() }
    single<ShareService> { IosShareService() }
    single<OngoingWorkoutIndicator> { OngoingWorkoutIndicatorHolder }
}
