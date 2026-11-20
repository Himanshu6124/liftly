package com.himanshu.liftly.feature.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.himanshu.liftly.feature.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

fun NavGraphBuilder.settingsScreen() {
    composable<SettingsRoute> { SettingsScreen() }
}
