package com.himanshu.liftly

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.himanshu.liftly.core.designsystem.component.FloatingTabBar
import com.himanshu.liftly.core.designsystem.component.FloatingTabBarHeight
import com.himanshu.liftly.core.designsystem.component.LocalFloatingBarInset
import com.himanshu.liftly.core.designsystem.component.TabItem
import com.himanshu.liftly.core.designsystem.theme.LiftlyTheme
import com.himanshu.liftly.feature.exercises.navigation.ExerciseDetailRoute
import com.himanshu.liftly.feature.exercises.navigation.exerciseDetailScreen
import com.himanshu.liftly.feature.exercises.navigation.exercisesScreen
import com.himanshu.liftly.feature.history.navigation.WorkoutDetailRoute
import com.himanshu.liftly.feature.history.navigation.historyScreen
import com.himanshu.liftly.feature.history.navigation.workoutDetailScreen
import com.himanshu.liftly.feature.home.navigation.HomeRoute
import com.himanshu.liftly.feature.home.navigation.homeScreen
import com.himanshu.liftly.feature.routines.navigation.RoutineEditorRoute
import com.himanshu.liftly.feature.routines.navigation.routineEditorScreen
import com.himanshu.liftly.feature.routines.navigation.routinesScreen
import com.himanshu.liftly.feature.settings.navigation.settingsScreen
import com.himanshu.liftly.feature.workout.navigation.WorkoutRoute
import com.himanshu.liftly.feature.workout.navigation.WorkoutSummaryRoute
import com.himanshu.liftly.feature.workout.navigation.workoutScreen
import com.himanshu.liftly.feature.workout.navigation.workoutSummaryScreen
import com.himanshu.liftly.navigation.AppAction
import com.himanshu.liftly.navigation.DeepLinks
import com.himanshu.liftly.navigation.TopLevelDestination
import kotlinx.coroutines.flow.filterNotNull
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(viewModel: AppViewModel = koinViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val loaded = settings ?: return // DataStore reads in a few ms; avoids a theme flash.

    LiftlyTheme(themeMode = loaded.themeMode, dynamicColor = loaded.dynamicColor) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            LiftlyNavShell(hasWorkoutInProgress = viewModel::hasWorkoutInProgress, runAction = viewModel::run)
        }
    }
}

@Composable
private fun LiftlyNavShell(hasWorkoutInProgress: suspend () -> Boolean, runAction: suspend (AppAction) -> Boolean) {
    val navController = rememberNavController()
    // Auto-resume once per app launch (saveable: not again after rotation).
    var resumeChecked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!resumeChecked) {
            resumeChecked = true
            if (hasWorkoutInProgress()) navController.navigate(WorkoutRoute) { launchSingleTop = true }
        }
    }
    // Shortcuts, Quick Actions, widgets and the Live Activity all arrive through DeepLinks.
    // One long-lived collector: consuming the link must not cancel the action it started.
    LaunchedEffect(Unit) {
        DeepLinks.pending.filterNotNull().collect { action ->
            DeepLinks.consume(action)
            if (runAction(action)) navController.navigate(WorkoutRoute) { launchSingleTop = true }
        }
    }
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination
    val showNavigation = current == null || TopLevelDestination.entries.any { current.isOn(it) }

    // Floating tab bar on compact, navigation rail on medium, drawer on expanded widths.
    val adaptiveType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfoV2())
    val floatingBar = showNavigation && adaptiveType == NavigationSuiteType.NavigationBar
    val selectedTab = TopLevelDestination.entries.indexOfFirst { current?.isOn(it) == true }
    val tabItems = TopLevelDestination.entries.map { TabItem(it.outlinedIcon, it.icon, stringResource(it.label)) }
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
        layoutType = if (showNavigation && !floatingBar) adaptiveType else NavigationSuiteType.None,
    ) {
        val barInset = if (floatingBar) {
            FloatingTabBarHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        } else {
            0.dp
        }
        Box(Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalFloatingBarInset provides barInset) {
                NavHost(navController = navController, startDestination = HomeRoute) {
                    val openWorkout = { navController.navigate(WorkoutRoute) { launchSingleTop = true } }
                    val editRoutine = { id: String? -> navController.navigate(RoutineEditorRoute(id)) }
                    homeScreen(onOpenActiveWorkout = openWorkout, onEditRoutine = editRoutine)
                    routinesScreen(onOpenWorkout = openWorkout, onEditRoutine = editRoutine)
                    routineEditorScreen(onDone = {
                        navController.popBackStack()
                    }, onOpenGuide = { navController.navigate(ExerciseDetailRoute(it)) })
                    exercisesScreen(onOpenExercise = { navController.navigate(ExerciseDetailRoute(it)) })
                    exerciseDetailScreen(onBack = { navController.popBackStack() })
                    historyScreen(onOpenWorkout = { navController.navigate(WorkoutDetailRoute(it)) })
                    workoutDetailScreen(onBack = {
                        navController.popBackStack()
                    }, onOpenGuide = { navController.navigate(ExerciseDetailRoute(it)) })
                    settingsScreen()
                    workoutScreen(
                        onClose = { finishedId ->
                            if (finishedId != null) {
                                navController.navigate(WorkoutSummaryRoute(finishedId)) { popUpTo(HomeRoute) }
                            } else {
                                navController.popBackStack(HomeRoute, inclusive = false)
                            }
                        },
                        onOpenGuide = { navController.navigate(ExerciseDetailRoute(it)) },
                    )
                    workoutSummaryScreen(onDone = { navController.popBackStack(HomeRoute, inclusive = false) })
                }
            }
            if (floatingBar) {
                FloatingTabBar(
                    items = tabItems,
                    selectedIndex = selectedTab.coerceAtLeast(0),
                    onSelect = { navController.navigateToTopLevel(TopLevelDestination.entries[it]) },
                    modifier = Modifier.align(Alignment.BottomCenter),
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
