package com.himanshu.liftly.feature.exercises.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.himanshu.liftly.feature.exercises.ExercisesScreen
import kotlinx.serialization.Serializable

@Serializable
data object ExercisesRoute

fun NavGraphBuilder.exercisesScreen() {
    composable<ExercisesRoute> { ExercisesScreen() }
}
