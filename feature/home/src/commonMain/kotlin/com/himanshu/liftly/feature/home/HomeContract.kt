package com.himanshu.liftly.feature.home

import androidx.compose.runtime.Immutable

@Immutable
data class HomeUiState(
    val syncLabel: SyncLabel = SyncLabel.LOCAL_ONLY,
)

enum class SyncLabel { LOCAL_ONLY, SYNCING, SYNCED, FAILED }

sealed interface HomeIntent {
    data object StartEmptyWorkout : HomeIntent
}

sealed interface HomeEffect {
    data object OpenActiveWorkout : HomeEffect
}
