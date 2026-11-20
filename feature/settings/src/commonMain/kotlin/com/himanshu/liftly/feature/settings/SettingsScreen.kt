package com.himanshu.liftly.feature.settings

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.himanshu.liftly.core.designsystem.component.EmptyState
import com.himanshu.liftly.feature.settings.resources.Res
import com.himanshu.liftly.feature.settings.resources.settings_empty_message
import com.himanshu.liftly.feature.settings.resources.settings_empty_title
import org.jetbrains.compose.resources.stringResource

/** Placeholder until this feature's milestone. */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Filled.Settings,
        title = stringResource(Res.string.settings_empty_title),
        message = stringResource(Res.string.settings_empty_message),
        modifier = modifier.statusBarsPadding(),
    )
}
