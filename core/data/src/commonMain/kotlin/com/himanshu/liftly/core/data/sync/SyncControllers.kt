package com.himanshu.liftly.core.data.sync

import com.himanshu.liftly.core.domain.auth.AuthRepository
import com.himanshu.liftly.core.domain.auth.AuthState
import com.himanshu.liftly.core.domain.sync.SyncController
import com.himanshu.liftly.core.domain.sync.SyncState
import com.himanshu.liftly.core.domain.util.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

/** For builds and tests without a backend: local writes are still tracked as PENDING. */
class LocalOnlySyncController : SyncController {
    override val state: Flow<SyncState> = flowOf(SyncState.LocalOnly)
    override fun requestSync() = Unit
}

/**
 * Syncs while someone is signed in, debounced after each write request, on sign-in and when the
 * app comes to the foreground. Signed out, it reports [SyncState.LocalOnly] and does nothing: local
 * data is kept and keeps collecting PENDING changes for the next sign-in.
 *
 * Before every run the local sync state is bound to the signed-in account ([SyncSql.bindAccount]):
 * a different account than last time gets this device's data uploaded as new, never mixed cursors.
 * Runs never overlap (a mutex), whether from requests or [syncNow].
 *
 * A failed run retries by itself after [retryBaseMillis], doubling up to [MAX_RETRY_MILLIS]
 * (30 s, 1, 2, 4… 30 min), so a phone that was offline at the gym catches up once it's back
 * online without anyone tapping anything. Any successful run resets the backoff.
 */
@OptIn(FlowPreview::class)
class AccountSyncController(
    private val auth: AuthRepository,
    private val engine: SyncEngine,
    private val sql: SyncSql,
    private val tables: SyncTables,
    private val time: TimeProvider,
    private val scope: CoroutineScope,
    debounceMillis: Long = 2_000,
    private val retryBaseMillis: Long = 30_000,
) : SyncController {
    private val mutex = Mutex()
    private var failures = 0
    private var retry: Job? = null

    // A channel keeps a request made before the collector starts (a SharedFlow would drop it).
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val runState = MutableStateFlow<SyncState>(SyncState.Idle)

    override val state: Flow<SyncState> = combine(auth.state, runState) { account, run ->
        if (account is AuthState.SignedIn) run else SyncState.LocalOnly
    }

    init {
        requests.receiveAsFlow().debounce(debounceMillis).onEach { runOnce() }.launchIn(scope)
        // Signing in (or starting up signed in) syncs right away.
        auth.state.map { (it as? AuthState.SignedIn)?.userId }.distinctUntilChanged()
            .onEach { if (it != null) requestSync() }
            .launchIn(scope)
    }

    override fun requestSync() {
        requests.trySend(Unit)
    }

    override suspend fun syncNow(): Boolean = runOnce()

    private suspend fun runOnce(): Boolean = mutex.withLock {
        val account = auth.state.value as? AuthState.SignedIn ?: return@withLock true
        retry?.cancel()
        runState.value = SyncState.Syncing
        val result = try {
            sql.bindAccount(account.userId, tables.all.map { it.name })
            engine.syncOnce()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Result.failure(e)
        }
        runState.value = result.fold(
            onSuccess = { SyncState.Synced(time.now()) },
            onFailure = { SyncState.Failed(it.message ?: "Sync failed") },
        )
        if (result.isSuccess) {
            failures = 0
        } else {
            val wait = (retryBaseMillis shl failures.coerceAtMost(MAX_DOUBLINGS)).coerceAtMost(MAX_RETRY_MILLIS)
            failures++
            retry = scope.launch {
                delay(wait)
                requestSync()
            }
        }
        result.isSuccess
    }

    private companion object {
        const val MAX_RETRY_MILLIS = 30 * 60 * 1000L
        const val MAX_DOUBLINGS = 10
    }
}
