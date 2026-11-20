package com.himanshu.liftly.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.himanshu.liftly.feature.home.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

fun NavGraphBuilder.homeScreen(onOpenActiveWorkout: () -> Unit, onEditRoutine: (String?) -> Unit) {
    composable<HomeRoute> { HomeScreen(onOpenActiveWorkout = onOpenActiveWorkout, onEditRoutine = onEditRoutine) }
}
