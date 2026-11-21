package com.himanshu.liftly.feature.workout.active

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.himanshu.liftly.core.designsystem.component.ExerciseThumbnail
import com.himanshu.liftly.core.designsystem.component.LiftlyCard
import com.himanshu.liftly.core.designsystem.component.PlateStack
import com.himanshu.liftly.core.designsystem.component.PrBadge
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
import com.himanshu.liftly.feature.workout.resources.card_plates
import com.himanshu.liftly.feature.workout.resources.card_pr
import com.himanshu.liftly.feature.workout.resources.card_quick_log
import com.himanshu.liftly.feature.workout.resources.card_remove
import com.himanshu.liftly.feature.workout.resources.card_skip
import com.himanshu.liftly.feature.workout.resources.card_status_logged
import com.himanshu.liftly.feature.workout.resources.card_status_pending
import com.himanshu.liftly.feature.workout.resources.card_status_skipped
import com.himanshu.liftly.feature.workout.resources.card_subtitle
import com.himanshu.liftly.feature.workout.resources.card_superset_leave
import com.himanshu.liftly.feature.workout.resources.card_superset_next
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
    val lift by animateFloatAsState(if (isDragging) DRAG_SCALE else 1f, label = "dragLift")
    LiftlyCard(
        onClick = onClick,
        containerColor = if (highlighted) MaterialTheme.colorScheme.primaryContainer else Color.Unspecified,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = lift
                scaleY = lift
                shadowElevation = if (isDragging) DRAG_SHADOW_DP * density else 0f
                shape = RoundedCornerShape(CARD_RADIUS_DP.dp)
                clip = false
            }
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Text(
                        card.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (card.hasPr) PrBadge(stringResource(Res.string.card_pr))
                }
                CardDetails(card, statusLabel)
            }
            IconButton(onClick = onFormGuide) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = stringResource(Res.string.card_form, card.name),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            CardMenu(card, skipLabel, removeLabel, skipAction, onIntent)
        }
    }
}

/** The card's ⋮ menu: every swipe/drag action as a plain menu item (also the accessible path). */
@Composable
private fun CardMenu(
    card: ExerciseCardUi,
    skipLabel: String,
    removeLabel: String,
    onSkip: () -> Unit,
    onIntent: (ActiveWorkoutIntent) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val id = card.workoutExerciseId
    val items = buildList {
        add(skipLabel to onSkip)
        add(stringResource(Res.string.card_move_up) to { onIntent(ActiveWorkoutIntent.Move(id, -1)) })
        add(stringResource(Res.string.card_move_down) to { onIntent(ActiveWorkoutIntent.Move(id, 1)) })
        if (card.canLinkNext) add(stringResource(Res.string.card_superset_next) to { onIntent(ActiveWorkoutIntent.SupersetWithNext(id)) })
        if (card.supersetGroup !=
            null
        ) {
            add(stringResource(Res.string.card_superset_leave) to { onIntent(ActiveWorkoutIntent.LeaveSuperset(id)) })
        }
        add(removeLabel to { onIntent(ActiveWorkoutIntent.Remove(id)) })
    }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(Res.string.card_options, card.name))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            items.forEach { (label, action) ->
                DropdownMenuItem(text = { Text(label) }, onClick = {
                    open = false
                    action()
                })
            }
        }
    }
}

/** Second line (last time / logged summary / muscle) and, for barbell work, the plate stack. */
@Composable
private fun CardDetails(card: ExerciseCardUi, statusLabel: String) {
    val spacing = LiftlyTheme.spacing
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
    if (card.platesPerSideKg != null && card.platesText != null) {
        PlateStack(
            perSideKg = card.platesPerSideKg,
            description = stringResource(Res.string.card_plates, card.platesText),
            height = 24.dp,
            modifier = Modifier.padding(top = spacing.xs),
        )
    }
}

private const val DRAG_SCALE = 1.03f
private const val DRAG_SHADOW_DP = 12f
private const val CARD_RADIUS_DP = 20
