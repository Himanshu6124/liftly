package com.himanshu.liftly.core.domain.sync

import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/**
 * What the UI may know about synchronisation. The app is fully functional without a backend:
 * the local database is always the source of truth, sync only reconciles it with a server.
 */
sealed interface SyncState {
    /** No backend configured (current default). */
    data object LocalOnly : SyncState
    data object Idle : SyncState
    data object Syncing : SyncState
    data class Synced(val at: Instant) : SyncState
    data class Failed(val message: String) : SyncState
}

interface SyncController {
    val state: Flow<SyncState>

    /** Fire-and-forget request; debounced and executed when online. Safe to call after every write. */
    fun requestSync()

    /**
     * Syncs now and waits for the result (background tasks: Android WorkManager, iOS app refresh).
     * True when there was nothing to do or it succeeded.
     */
    suspend fun syncNow(): Boolean {
        requestSync()
        return true
    }
}
