package com.himanshu.liftly.feature.home

import androidx.compose.runtime.Immutable
import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.util.TrainingCalendar
import com.himanshu.liftly.feature.routines.list.RoutineRow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Instant

@Immutable
data class InProgressBanner(val name: String, val startedAt: Instant, val exerciseCount: Int, val loggedCount: Int)

@Immutable
data class LastWorkoutCard(
    val name: String,
    val startedAt: Instant,
    val endedAt: Instant?,
    val exerciseCount: Int,
    val loggedCount: Int,
    val volumeKg: Double,
)

@Immutable
data class HomeUiState(
    val syncLabel: SyncLabel = SyncLabel.LOCAL_ONLY,
    val inProgress: InProgressBanner? = null,
    /** The routine done longest ago: the big 1-tap button. */
    val nextRoutine: RoutineRow? = null,
    val routines: ImmutableList<RoutineRow> = persistentListOf(),
    val lastWorkout: LastWorkoutCard? = null,
    val unit: WeightUnit = WeightUnit.KG,
    /** Heatmap, weekly goal and streak; null until there's at least one finished workout. */
    val calendar: TrainingCalendar? = null,
    /** True until the first data arrives, so empty-state hints don't flash on launch. */
    val isLoading: Boolean = true,
)

enum class SyncLabel { LOCAL_ONLY, SYNCING, SYNCED, FAILED }

sealed interface HomeIntent {
    /** Starts an empty workout, or resumes the one in progress (only one can exist). */
    data object StartEmptyWorkout : HomeIntent
    data object ResumeWorkout : HomeIntent
    data class StartRoutine(val routineId: String) : HomeIntent
}

sealed interface HomeEffect {
    data object OpenActiveWorkout : HomeEffect
}
