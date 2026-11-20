package com.himanshu.liftly.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.himanshu.liftly.core.domain.sync.SyncController
import com.himanshu.liftly.core.domain.sync.SyncState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(syncController: SyncController) : ViewModel() {

    val state: StateFlow<HomeUiState> = syncController.state
        .map { HomeUiState(syncLabel = it.toLabel()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.StartEmptyWorkout -> viewModelScope.launch { _effects.send(HomeEffect.OpenActiveWorkout) }
        }
    }

    private fun SyncState.toLabel() = when (this) {
        SyncState.LocalOnly, SyncState.Idle -> SyncLabel.LOCAL_ONLY
        SyncState.Syncing -> SyncLabel.SYNCING
        is SyncState.Synced -> SyncLabel.SYNCED
        is SyncState.Failed -> SyncLabel.FAILED
    }
}
