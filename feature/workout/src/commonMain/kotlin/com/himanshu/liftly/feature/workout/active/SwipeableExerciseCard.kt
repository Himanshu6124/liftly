package com.himanshu.liftly.feature.workout.active

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.himanshu.liftly.core.designsystem.theme.LiftlyColors
import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.feature.workout.resources.Res
import com.himanshu.liftly.feature.workout.resources.card_quick_log
import com.himanshu.liftly.feature.workout.resources.card_skip
import org.jetbrains.compose.resources.stringResource

/**
 * Swipe right → quick-log the pre-filled values. Swipe left → skip. The card snaps back either
 * way (it stays in the checklist with its new status); both actions have Undo.
 */
@Composable
internal fun SwipeableExerciseCard(
    card: ExerciseCardUi,
    isDragging: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit,
    onFormGuide: () -> Unit,
    onIntent: (ActiveWorkoutIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val canLog = card.status != ExerciseStatus.LOGGED
    val canSkip = card.status == ExerciseStatus.PENDING
    val state = rememberSwipeToDismissBoxState()

    LaunchedEffect(state.currentValue) {
        when (state.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> onIntent(ActiveWorkoutIntent.QuickLog(card.workoutExerciseId))
            SwipeToDismissBoxValue.EndToStart -> onIntent(ActiveWorkoutIntent.Skip(card.workoutExerciseId))
            SwipeToDismissBoxValue.Settled -> return@LaunchedEffect
        }
        state.snapTo(SwipeToDismissBoxValue.Settled)
    }

    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = canLog,
        enableDismissFromEndToStart = canSkip,
        modifier = modifier,
        backgroundContent = {
            val toLog = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val color = when (state.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> LiftlyColors.Success
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.surfaceVariant
                SwipeToDismissBoxValue.Settled -> Color.Transparent
            }
            Row(
                Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(color).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (toLog) Arrangement.Start else Arrangement.End,
            ) {
                if (state.dismissDirection != SwipeToDismissBoxValue.Settled) {
                    Icon(
                        if (toLog) Icons.Filled.Check else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = if (toLog) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(if (toLog) Res.string.card_quick_log else Res.string.card_skip),
                        color = if (toLog) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        },
    ) {
        Box {
            ExerciseCard(
                card = card,
                isDragging = isDragging,
                highlighted = highlighted,
                onClick = onClick,
                onFormGuide = onFormGuide,
                onIntent = onIntent,
            )
        }
    }
}
