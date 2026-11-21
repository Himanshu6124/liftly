package com.himanshu.liftly.core.platform

import co.touchlab.kermit.Logger
import org.koin.core.module.Module
import kotlin.time.Instant

interface Haptics {
    fun light()
    fun success()
}

interface Notifier {
    fun schedule(id: String, at: Instant, title: String, body: String)
    fun cancel(id: String)
}

interface UrlLauncher {
    fun open(url: String)
}

interface ShareService {
    fun shareText(text: String)
    fun shareFile(path: String, mime: String)
}

/** Used where a capability doesn't exist yet (e.g. local notifications arrive with the rest timer in Phase 2). */
class LoggingNotifier : Notifier {
    private val log = Logger.withTag("Notifier")
    override fun schedule(id: String, at: Instant, title: String, body: String) = log.d { "schedule $id at $at: $title" }
    override fun cancel(id: String) = log.d { "cancel $id" }
}

/**
 * Binds [Haptics], [Notifier], [UrlLauncher], [ShareService], [OngoingWorkoutIndicator] and (except on
 * Android, where the Glance widget lives in the app module) [HomeWidgetBridge] for the current platform.
 */
expect val platformServicesModule: Module
