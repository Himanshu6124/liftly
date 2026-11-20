package com.himanshu.liftly.feature.workout.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.himanshu.liftly.feature.workout.WorkoutScreen
import kotlinx.serialization.Serializable

@Serializable
data object WorkoutRoute

fun NavGraphBuilder.workoutScreen() {
    composable<WorkoutRoute> { WorkoutScreen() }
}
