package com.himanshu.liftly.feature.history.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.himanshu.liftly.feature.history.HistoryScreen
import kotlinx.serialization.Serializable

@Serializable
data object HistoryRoute

fun NavGraphBuilder.historyScreen() {
    composable<HistoryRoute> { HistoryScreen() }
}
