package com.himanshu.liftly.feature.history.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.himanshu.liftly.feature.history.detail.WorkoutDetailScreen
import com.himanshu.liftly.feature.history.list.HistoryScreen
import kotlinx.serialization.Serializable

@Serializable
data object HistoryRoute

@Serializable
data class WorkoutDetailRoute(val workoutId: String)

fun NavGraphBuilder.historyScreen(onOpenWorkout: (String) -> Unit) {
    composable<HistoryRoute> { HistoryScreen(onOpenWorkout = onOpenWorkout) }
}

fun NavGraphBuilder.workoutDetailScreen(onBack: () -> Unit, onOpenGuide: (String) -> Unit) {
    composable<WorkoutDetailRoute> { entry ->
        WorkoutDetailScreen(entry.toRoute<WorkoutDetailRoute>().workoutId, onBack, onOpenGuide)
    }
}
