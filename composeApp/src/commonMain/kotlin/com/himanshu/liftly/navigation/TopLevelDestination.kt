package com.himanshu.liftly.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.himanshu.liftly.feature.exercises.navigation.ExercisesRoute
import com.himanshu.liftly.feature.history.navigation.HistoryRoute
import com.himanshu.liftly.feature.home.navigation.HomeRoute
import com.himanshu.liftly.feature.routines.navigation.RoutinesRoute
import com.himanshu.liftly.feature.settings.navigation.SettingsRoute
import com.himanshu.liftly.resources.Res
import com.himanshu.liftly.resources.nav_exercises
import com.himanshu.liftly.resources.nav_history
import com.himanshu.liftly.resources.nav_home
import com.himanshu.liftly.resources.nav_routines
import com.himanshu.liftly.resources.nav_settings
import org.jetbrains.compose.resources.StringResource

enum class TopLevelDestination(val route: Any, val icon: ImageVector, val label: StringResource) {
    HOME(HomeRoute, Icons.Filled.Home, Res.string.nav_home),
    ROUTINES(RoutinesRoute, Icons.AutoMirrored.Filled.List, Res.string.nav_routines),
    EXERCISES(ExercisesRoute, Icons.Filled.Search, Res.string.nav_exercises),
    HISTORY(HistoryRoute, Icons.Filled.DateRange, Res.string.nav_history),
    SETTINGS(SettingsRoute, Icons.Filled.Settings, Res.string.nav_settings),
}
