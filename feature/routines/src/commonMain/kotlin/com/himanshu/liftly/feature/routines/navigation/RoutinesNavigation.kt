package com.himanshu.liftly.feature.routines.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.himanshu.liftly.feature.routines.editor.RoutineEditorScreen
import com.himanshu.liftly.feature.routines.list.RoutinesScreen
import kotlinx.serialization.Serializable

@Serializable
data object RoutinesRoute

/** [routineId] null = create a new routine. */
@Serializable
data class RoutineEditorRoute(val routineId: String? = null)

fun NavGraphBuilder.routinesScreen(onOpenWorkout: () -> Unit, onEditRoutine: (String?) -> Unit) {
    composable<RoutinesRoute> { RoutinesScreen(onOpenWorkout = onOpenWorkout, onEditRoutine = onEditRoutine) }
}

fun NavGraphBuilder.routineEditorScreen(onDone: () -> Unit, onOpenGuide: (String) -> Unit) {
    composable<RoutineEditorRoute> { entry ->
        RoutineEditorScreen(routineId = entry.toRoute<RoutineEditorRoute>().routineId, onDone = onDone, onOpenGuide = onOpenGuide)
    }
}
