package com.himanshu.liftly.feature.home

import androidx.compose.runtime.Immutable
import kotlin.time.Instant

@Immutable
data class InProgressBanner(val name: String, val startedAt: Instant, val exerciseCount: Int, val loggedCount: Int)

@Immutable
data class HomeUiState(
    val syncLabel: SyncLabel = SyncLabel.LOCAL_ONLY,
    val inProgress: InProgressBanner? = null,
)

enum class SyncLabel { LOCAL_ONLY, SYNCING, SYNCED, FAILED }

sealed interface HomeIntent {
    /** Starts an empty workout, or resumes the one in progress (only one can exist). */
    data object StartEmptyWorkout : HomeIntent
    data object ResumeWorkout : HomeIntent
}

sealed interface HomeEffect {
    data object OpenActiveWorkout : HomeEffect
}
