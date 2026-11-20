package com.himanshu.liftly.feature.routines.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.himanshu.liftly.feature.routines.RoutinesScreen
import kotlinx.serialization.Serializable

@Serializable
data object RoutinesRoute

fun NavGraphBuilder.routinesScreen() {
    composable<RoutinesRoute> { RoutinesScreen() }
}
