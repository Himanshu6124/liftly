package com.himanshu.liftly.core.data.sync

import com.himanshu.liftly.core.domain.sync.SyncController
import com.himanshu.liftly.core.domain.sync.SyncState
import com.himanshu.liftly.core.domain.util.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

/** Default while there is no backend: local writes are still tracked as PENDING for a future first sync. */
class LocalOnlySyncController : SyncController {
    override val state: Flow<SyncState> = flowOf(SyncState.LocalOnly)
    override fun requestSync() = Unit
}

/** Debounced background sync. Enable by binding this instead of [LocalOnlySyncController]. */
@OptIn(FlowPreview::class)
class BackgroundSyncController(
    private val engine: SyncEngine,
    private val time: TimeProvider,
    scope: CoroutineScope,
    debounceMillis: Long = 2_000,
) : SyncController {
    private val requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    override val state: Flow<SyncState> = _state.asStateFlow()

    init {
        requests
            .onStart { emit(Unit) } // sync once on start
            .debounce(debounceMillis)
            .onEach {
                _state.value = SyncState.Syncing
                _state.value = engine.syncOnce().fold(
                    onSuccess = { SyncState.Synced(time.now()) },
                    onFailure = { SyncState.Failed(it.message ?: "Sync failed") },
                )
            }
            .launchIn(scope)
    }

    override fun requestSync() {
        requests.tryEmit(Unit)
    }
}
