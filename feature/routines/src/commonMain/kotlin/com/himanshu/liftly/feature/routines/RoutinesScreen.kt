package com.himanshu.liftly.feature.routines

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.himanshu.liftly.core.designsystem.component.EmptyState
import com.himanshu.liftly.feature.routines.resources.Res
import com.himanshu.liftly.feature.routines.resources.routines_empty_message
import com.himanshu.liftly.feature.routines.resources.routines_empty_title
import org.jetbrains.compose.resources.stringResource

/** Placeholder until this feature's milestone. */
@Composable
fun RoutinesScreen(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.AutoMirrored.Filled.List,
        title = stringResource(Res.string.routines_empty_title),
        message = stringResource(Res.string.routines_empty_message),
        modifier = modifier.statusBarsPadding(),
    )
}
