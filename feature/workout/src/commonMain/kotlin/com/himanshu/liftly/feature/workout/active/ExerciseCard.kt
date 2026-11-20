package com.himanshu.liftly.feature.workout.active

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.himanshu.liftly.core.designsystem.component.ExerciseThumbnail
import com.himanshu.liftly.core.designsystem.component.StatusIcon
import com.himanshu.liftly.core.designsystem.component.label
import com.himanshu.liftly.core.designsystem.theme.LiftlyTheme
import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.feature.workout.resources.Res
import com.himanshu.liftly.feature.workout.resources.card_form
import com.himanshu.liftly.feature.workout.resources.card_last
import com.himanshu.liftly.feature.workout.resources.card_move_down
import com.himanshu.liftly.feature.workout.resources.card_move_up
import com.himanshu.liftly.feature.workout.resources.card_options
import com.himanshu.liftly.feature.workout.resources.card_quick_log
import com.himanshu.liftly.feature.workout.resources.card_remove
import com.himanshu.liftly.feature.workout.resources.card_skip
import com.himanshu.liftly.feature.workout.resources.card_status_logged
import com.himanshu.liftly.feature.workout.resources.card_status_pending
import com.himanshu.liftly.feature.workout.resources.card_status_skipped
import com.himanshu.liftly.feature.workout.resources.card_subtitle
import com.himanshu.liftly.feature.workout.resources.card_unskip
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ExerciseCard(
    card: ExerciseCardUi,
    isDragging: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit,
    onFormGuide: () -> Unit,
    onIntent: (ActiveWorkoutIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LiftlyTheme.spacing
    var menuOpen by remember { mutableStateOf(false) }
    val statusLabel = stringResource(
        when (card.status) {
            ExerciseStatus.PENDING -> Res.string.card_status_pending
            ExerciseStatus.LOGGED -> Res.string.card_status_logged
            ExerciseStatus.SKIPPED -> Res.string.card_status_skipped
        },
    )
    val skipLabel = stringResource(if (card.status == ExerciseStatus.SKIPPED) Res.string.card_unskip else Res.string.card_skip)
    val removeLabel = stringResource(Res.string.card_remove)
    val skipAction = {
        onIntent(
            if (card.status == ExerciseStatus.SKIPPED) {
                ActiveWorkoutIntent.Unskip(card.workoutExerciseId)
            } else {
                ActiveWorkoutIntent.Skip(card.workoutExerciseId)
            },
        )
    }

    val logLabel = stringResource(Res.string.card_quick_log)
    ElevatedCard(
        onClick = onClick,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isDragging) 8.dp else 1.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = modifier
            .fillMaxWidth()
            // Screen-reader users get the swipe/menu actions directly on the card.
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(logLabel) {
                        onIntent(ActiveWorkoutIntent.QuickLog(card.workoutExerciseId))
                        true
                    },
                    CustomAccessibilityAction(skipLabel) {
                        skipAction()
                        true
                    },
                    CustomAccessibilityAction(removeLabel) {
                        onIntent(ActiveWorkoutIntent.Remove(card.workoutExerciseId))
                        true
                    },
                )
            },
    ) {
        Row(
            Modifier.padding(spacing.sm).alpha(if (card.status == ExerciseStatus.SKIPPED) 0.6f else 1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            StatusIcon(card.status, statusLabel, Modifier.padding(start = spacing.xs))
            ExerciseThumbnail(card.imagePaths, contentDescription = null, size = 52.dp)
            Column(Modifier.weight(1f)) {
                Text(card.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                val logged = card.status == ExerciseStatus.LOGGED
                Text(
                    when {
                        logged && card.summary != null -> card.summary

                        card.status == ExerciseStatus.PENDING && card.lastHint != null -> stringResource(
                            Res.string.card_last,
                            card.lastHint,
                        )

                        card.status == ExerciseStatus.PENDING ->
                            stringResource(Res.string.card_subtitle, card.primaryMuscle.label(), card.equipment.label())

                        else -> statusLabel
                    },
                    style = if (logged) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                    color = if (logged) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onFormGuide) {
                Icon(Icons.Filled.Info, contentDescription = stringResource(Res.string.card_form, card.name))
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(Res.string.card_options, card.name))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text(skipLabel) }, onClick = {
                        menuOpen = false
                        skipAction()
                    })
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.card_move_up)) },
                        onClick = {
                            menuOpen = false
                            onIntent(ActiveWorkoutIntent.Move(card.workoutExerciseId, -1))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.card_move_down)) },
                        onClick = {
                            menuOpen = false
                            onIntent(ActiveWorkoutIntent.Move(card.workoutExerciseId, 1))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(removeLabel) },
                        onClick = {
                            menuOpen = false
                            onIntent(ActiveWorkoutIntent.Remove(card.workoutExerciseId))
                        },
                    )
                }
            }
        }
    }
}
