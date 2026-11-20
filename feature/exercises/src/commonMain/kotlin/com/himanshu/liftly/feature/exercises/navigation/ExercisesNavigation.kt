package com.himanshu.liftly.feature.exercises.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.himanshu.liftly.feature.exercises.detail.ExerciseDetailScreen
import com.himanshu.liftly.feature.exercises.library.ExerciseLibraryScreen
import kotlinx.serialization.Serializable

@Serializable
data object ExercisesRoute

@Serializable
data class ExerciseDetailRoute(val exerciseId: String)

fun NavGraphBuilder.exercisesScreen(onOpenExercise: (String) -> Unit) {
    composable<ExercisesRoute> { ExerciseLibraryScreen(onOpenExercise = onOpenExercise) }
}

fun NavGraphBuilder.exerciseDetailScreen(onBack: () -> Unit) {
    composable<ExerciseDetailRoute> { entry ->
        ExerciseDetailScreen(exerciseId = entry.toRoute<ExerciseDetailRoute>().exerciseId, onBack = onBack)
    }
}
