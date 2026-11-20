package com.himanshu.liftly.feature.workout.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.himanshu.liftly.feature.workout.active.ActiveWorkoutScreen
import kotlinx.serialization.Serializable

@Serializable
data object WorkoutRoute

fun NavGraphBuilder.workoutScreen(onClose: (finished: Boolean) -> Unit, onOpenGuide: (String) -> Unit) {
    composable<WorkoutRoute> { ActiveWorkoutScreen(onClose = onClose, onOpenGuide = onOpenGuide) }
}
