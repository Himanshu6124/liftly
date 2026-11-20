package com.himanshu.liftly

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.himanshu.liftly.core.designsystem.theme.LiftlyTheme
import com.himanshu.liftly.feature.exercises.navigation.ExerciseDetailRoute
import com.himanshu.liftly.feature.exercises.navigation.exerciseDetailScreen
import com.himanshu.liftly.feature.exercises.navigation.exercisesScreen
import com.himanshu.liftly.feature.history.navigation.historyScreen
import com.himanshu.liftly.feature.home.navigation.HomeRoute
import com.himanshu.liftly.feature.home.navigation.homeScreen
import com.himanshu.liftly.feature.routines.navigation.routinesScreen
import com.himanshu.liftly.feature.settings.navigation.settingsScreen
import com.himanshu.liftly.feature.workout.navigation.WorkoutRoute
import com.himanshu.liftly.feature.workout.navigation.workoutScreen
import com.himanshu.liftly.navigation.TopLevelDestination
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(viewModel: AppViewModel = koinViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val loaded = settings ?: return // DataStore reads in a few ms; avoids a theme flash.

    LiftlyTheme(themeMode = loaded.themeMode, dynamicColor = loaded.dynamicColor) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            LiftlyNavShell(hasWorkoutInProgress = viewModel::hasWorkoutInProgress)
        }
    }
}

@Composable
private fun LiftlyNavShell(hasWorkoutInProgress: suspend () -> Boolean) {
    val navController = rememberNavController()
    // Auto-resume once per app launch (saveable: not again after rotation).
    var resumeChecked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!resumeChecked) {
            resumeChecked = true
            if (hasWorkoutInProgress()) navController.navigate(WorkoutRoute) { launchSingleTop = true }
        }
    }
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination
    val showNavigation = current == null || TopLevelDestination.entries.any { current.isOn(it) }

    // Bottom bar on compact, navigation rail on medium, drawer on expanded widths.
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { dest ->
                item(
                    selected = current?.isOn(dest) == true,
                    onClick = { navController.navigateToTopLevel(dest) },
                    icon = { Icon(dest.icon, contentDescription = null) },
                    label = { Text(stringResource(dest.label)) },
                )
            }
        },
        layoutType = if (showNavigation) {
            NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfoV2())
        } else {
            NavigationSuiteType.None
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = HomeRoute) {
                homeScreen(onOpenActiveWorkout = { navController.navigate(WorkoutRoute) { launchSingleTop = true } })
                routinesScreen()
                exercisesScreen(onOpenExercise = { navController.navigate(ExerciseDetailRoute(it)) })
                exerciseDetailScreen(onBack = { navController.popBackStack() })
                historyScreen()
                settingsScreen()
                workoutScreen(
                    // Summary screen arrives in Milestone 7; for now finishing returns Home.
                    onClose = { navController.popBackStack(HomeRoute, inclusive = false) },
                    onOpenGuide = { navController.navigate(ExerciseDetailRoute(it)) },
                )
            }
        }
    }
}

private fun NavDestination.isOn(dest: TopLevelDestination) =
    hierarchy.any { it.hasRoute(dest.route::class) }

private fun NavHostController.navigateToTopLevel(dest: TopLevelDestination) {
    navigate(dest.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
