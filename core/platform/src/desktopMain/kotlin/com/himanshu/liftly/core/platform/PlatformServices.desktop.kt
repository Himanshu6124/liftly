package com.himanshu.liftly.core.platform

import org.koin.core.module.Module
import org.koin.dsl.module
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI

private object NoHaptics : Haptics {
    override fun light() = Unit
    override fun success() = Unit
}

private class DesktopUrlLauncher : UrlLauncher {
    override fun open(url: String) {
        if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI(url))
    }
}

/** Desktop has no share sheet: text goes to the clipboard, files are revealed in the file manager. */
private class DesktopShareService : ShareService {
    override fun shareText(text: String) = Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    override fun shareFile(path: String, mime: String) {
        if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(File(path).parentFile)
    }
}

actual val platformServicesModule: Module = module {
    single<Haptics> { NoHaptics }
    single<Notifier> { LoggingNotifier() }
    single<UrlLauncher> { DesktopUrlLauncher() }
    single<ShareService> { DesktopShareService() }
    single<OngoingWorkoutIndicator> { NoOngoingWorkoutIndicator }
    single<HomeWidgetBridge> { NoHomeWidgets }
    single<NativeSignIn> { NoNativeSignIn }
    single<SecureStore> { InMemorySecureStore() }
}
