package com.himanshu.liftly.feature.history

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.himanshu.liftly.core.designsystem.component.EmptyState
import com.himanshu.liftly.feature.history.resources.Res
import com.himanshu.liftly.feature.history.resources.history_empty_message
import com.himanshu.liftly.feature.history.resources.history_empty_title
import org.jetbrains.compose.resources.stringResource

/** Placeholder until this feature's milestone. */
@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Filled.DateRange,
        title = stringResource(Res.string.history_empty_title),
        message = stringResource(Res.string.history_empty_message),
        modifier = modifier.statusBarsPadding(),
    )
}
