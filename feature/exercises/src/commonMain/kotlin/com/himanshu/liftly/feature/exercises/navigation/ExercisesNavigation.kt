package com.himanshu.liftly.feature.exercises.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.window.core.layout.WindowSizeClass
import com.himanshu.liftly.core.designsystem.component.EmptyState
import com.himanshu.liftly.feature.exercises.detail.ExerciseDetailScreen
import com.himanshu.liftly.feature.exercises.library.ExerciseLibraryScreen
import com.himanshu.liftly.feature.exercises.resources.Res
import com.himanshu.liftly.feature.exercises.resources.exercises_select_hint
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

@Serializable
data object ExercisesRoute

@Serializable
data class ExerciseDetailRoute(val exerciseId: String)

fun NavGraphBuilder.exercisesScreen(onOpenExercise: (String) -> Unit) {
    composable<ExercisesRoute> { ExercisesPane(onOpenExercise) }
}

fun NavGraphBuilder.exerciseDetailScreen(onBack: () -> Unit) {
    composable<ExerciseDetailRoute> { entry ->
        ExerciseDetailScreen(exerciseId = entry.toRoute<ExerciseDetailRoute>().exerciseId, onBack = onBack)
    }
}

/** Single pane on phones (detail is its own screen); list + detail side by side on expanded widths. */
@Composable
private fun ExercisesPane(onOpenExercise: (String) -> Unit) {
    val wide = currentWindowAdaptiveInfoV2().windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    if (!wide) {
        ExerciseLibraryScreen(onOpenExercise = onOpenExercise)
        return
    }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    Row(Modifier.fillMaxSize()) {
        ExerciseLibraryScreen(onOpenExercise = { selected = it }, modifier = Modifier.weight(LIST_WEIGHT))
        VerticalDivider()
        Box(Modifier.weight(1f - LIST_WEIGHT).fillMaxHeight()) {
            val id = selected
            if (id == null) {
                EmptyState(Icons.Filled.Search, stringResource(Res.string.exercises_select_hint), "")
            } else {
                ExerciseDetailScreen(exerciseId = id, onBack = { selected = null })
            }
        }
    }
}

private const val LIST_WEIGHT = 0.4f
