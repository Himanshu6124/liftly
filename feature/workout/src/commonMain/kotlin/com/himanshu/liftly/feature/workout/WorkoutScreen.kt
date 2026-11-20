package com.himanshu.liftly.feature.workout

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.himanshu.liftly.core.designsystem.component.EmptyState
import com.himanshu.liftly.feature.workout.resources.Res
import com.himanshu.liftly.feature.workout.resources.workout_empty_message
import com.himanshu.liftly.feature.workout.resources.workout_empty_title
import org.jetbrains.compose.resources.stringResource

/** Placeholder until this feature's milestone. */
@Composable
fun WorkoutScreen(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Filled.PlayArrow,
        title = stringResource(Res.string.workout_empty_title),
        message = stringResource(Res.string.workout_empty_message),
        modifier = modifier.statusBarsPadding(),
    )
}
