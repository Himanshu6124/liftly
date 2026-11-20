package com.himanshu.liftly.feature.workout.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.himanshu.liftly.feature.workout.active.ActiveWorkoutScreen
import com.himanshu.liftly.feature.workout.summary.WorkoutSummaryScreen
import kotlinx.serialization.Serializable

@Serializable
data object WorkoutRoute

@Serializable
data class WorkoutSummaryRoute(val workoutId: String)

/** [onClose] gets the finished workout's id, or null when it was discarded / not there. */
fun NavGraphBuilder.workoutScreen(onClose: (finishedWorkoutId: String?) -> Unit, onOpenGuide: (String) -> Unit) {
    composable<WorkoutRoute> { ActiveWorkoutScreen(onClose = onClose, onOpenGuide = onOpenGuide) }
}

fun NavGraphBuilder.workoutSummaryScreen(onDone: () -> Unit) {
    composable<WorkoutSummaryRoute> { entry -> WorkoutSummaryScreen(entry.toRoute<WorkoutSummaryRoute>().workoutId, onDone) }
}
