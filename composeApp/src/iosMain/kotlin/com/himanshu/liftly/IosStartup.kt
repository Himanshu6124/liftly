package com.himanshu.liftly

import com.himanshu.liftly.core.domain.sync.SyncController
import com.himanshu.liftly.di.initKoin
import org.koin.mp.KoinPlatform

/**
 * Starts Koin once, from whichever comes first: the UI, or a background task when iOS launches
 * the app in the background without showing it.
 */
object IosStartup {
    private var started = false

    fun ensureStarted() {
        if (started) return
        initKoin()
        started = true
    }
}

/**
 * Background app refresh (`BGAppRefreshTask`, scheduled from `AppDelegate.swift`): one sync run.
 * Must be called on the main thread. True when it succeeded or there was nothing to do.
 */
@Suppress("unused") // Called from Swift.
suspend fun runBackgroundSync(): Boolean {
    IosStartup.ensureStarted()
    return KoinPlatform.getKoin().get<SyncController>().syncNow()
}
