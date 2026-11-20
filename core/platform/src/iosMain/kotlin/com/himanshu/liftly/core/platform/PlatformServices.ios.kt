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

private class IosHaptics : Haptics {
    override fun light() = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight).impactOccurred()
    override fun success() = UINotificationFeedbackGenerator().notificationOccurred(
        UINotificationFeedbackType.UINotificationFeedbackTypeSuccess,
    )
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
    single<Notifier> { LoggingNotifier() }
    single<UrlLauncher> { IosUrlLauncher() }
    single<ShareService> { IosShareService() }
}
