package com.himanshu.liftly.feature.workout.logsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.himanshu.liftly.core.designsystem.component.NumberStepper
import com.himanshu.liftly.core.designsystem.component.RepeatingIconButton
import com.himanshu.liftly.core.designsystem.theme.LiftlyTheme
import com.himanshu.liftly.core.domain.model.LogSnapshot
import com.himanshu.liftly.core.domain.util.SetFormatter.symbol
import com.himanshu.liftly.feature.workout.resources.Res
import com.himanshu.liftly.feature.workout.resources.days_ago
import com.himanshu.liftly.feature.workout.resources.days_today
import com.himanshu.liftly.feature.workout.resources.days_yesterday
import com.himanshu.liftly.feature.workout.resources.log_error_reps_invalid
import com.himanshu.liftly.feature.workout.resources.log_error_reps_range
import com.himanshu.liftly.feature.workout.resources.log_error_weight_invalid
import com.himanshu.liftly.feature.workout.resources.log_error_weight_range
import com.himanshu.liftly.feature.workout.resources.log_first_time
import com.himanshu.liftly.feature.workout.resources.log_form
import com.himanshu.liftly.feature.workout.resources.log_last_when
import com.himanshu.liftly.feature.workout.resources.log_reps
import com.himanshu.liftly.feature.workout.resources.log_reps_hint
import com.himanshu.liftly.feature.workout.resources.log_save
import com.himanshu.liftly.feature.workout.resources.log_sets
import com.himanshu.liftly.feature.workout.resources.log_sets_down
import com.himanshu.liftly.feature.workout.resources.log_sets_up
import com.himanshu.liftly.feature.workout.resources.log_warmups_kept
import com.himanshu.liftly.feature.workout.resources.log_weight
import com.himanshu.liftly.feature.workout.resources.log_weight_down
import com.himanshu.liftly.feature.workout.resources.log_weight_up
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Batch Log Sheet: all sets of one exercise in one go, pre-filled so a normal day is just Save.
 * Nothing is written until Save; dismissing the sheet discards the draft.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSheet(
    workoutExerciseId: String,
    onSaved: (exerciseName: String, summary: String?, undo: LogSnapshot) -> Unit,
    onDismiss: () -> Unit,
    onFormGuide: (exerciseId: String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val workouts = koinInject<com.himanshu.liftly.core.domain.repository.WorkoutRepository>()
    val settings = koinInject<com.himanshu.liftly.core.domain.repository.SettingsRepository>()
    val prefill = koinInject<com.himanshu.liftly.core.domain.usecase.GetLogSheetPrefillUseCase>()
    val parse = koinInject<com.himanshu.liftly.core.domain.usecase.ParseRepInputUseCase>()
    val log = koinInject<com.himanshu.liftly.core.domain.usecase.LogExerciseSetsUseCase>()
    val haptics = koinInject<com.himanshu.liftly.core.platform.Haptics>()
    val holder = remember(workoutExerciseId) {
        LogSheetState(workoutExerciseId, workouts, settings, prefill, parse, log, haptics, scope)
    }
    val state = holder.state

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LogSheetContent(
            state = state,
            weightField = holder.weightField,
            repsField = holder.repsField,
            onNudgeWeight = holder::nudgeWeight,
            onSetCountChange = holder::changeSetCount,
            onSave = { holder.save { summary, undo -> onSaved(state.exerciseName, summary, undo) } },
            onFormGuide = { onFormGuide(state.exerciseId) },
        )
    }
}

@Composable
fun LogSheetContent(
    state: LogSheetUiState,
    weightField: TextFieldState,
    repsField: TextFieldState,
    onNudgeWeight: (Int) -> Unit,
    onSetCountChange: (Int) -> Unit,
    onSave: () -> Unit,
    onFormGuide: () -> Unit,
) {
    val spacing = LiftlyTheme.spacing
    val firstField = remember { FocusRequester() }
    // Numeric keyboard opens on the first editable field right away.
    LaunchedEffect(state.isLoading) { if (!state.isLoading) runCatching { firstField.requestFocus() } }

    Column(
        Modifier.fillMaxWidth().padding(horizontal = spacing.md).navigationBarsPadding().imePadding(),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(state.exerciseName, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            AssistChip(
                onClick = onFormGuide,
                label = { Text(stringResource(Res.string.log_form)) },
                leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
            )
        }
        Text(
            text = state.lastHint?.let { stringResource(Res.string.log_last_when, it, relativeDays(state.lastPerformedAt)) }
                ?: stringResource(Res.string.log_first_time),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.showsWeight) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                RepeatingIconButton({ onNudgeWeight(-1) }, stringResource(Res.string.log_weight_down)) {
                    Text("−", style = MaterialTheme.typography.headlineSmall)
                }
                OutlinedTextField(
                    state = weightField,
                    label = { Text(stringResource(Res.string.log_weight)) },
                    suffix = { Text(state.unit.symbol) },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    isError = state.weightError != null,
                    supportingText = state.weightError?.let { { Text(it.message()) } },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f).focusRequester(firstField),
                )
                RepeatingIconButton({ onNudgeWeight(1) }, stringResource(Res.string.log_weight_up)) {
                    Text("+", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.log_sets), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            NumberStepper(
                value = state.setCount.toString(),
                onDecrement = { onSetCountChange(-1) },
                onIncrement = { onSetCountChange(1) },
                decrementDescription = stringResource(Res.string.log_sets_down),
                incrementDescription = stringResource(Res.string.log_sets_up),
                canDecrement = state.setCount > 1,
            )
        }

        OutlinedTextField(
            state = repsField,
            label = { Text(stringResource(Res.string.log_reps)) },
            placeholder = { Text(stringResource(Res.string.log_reps_hint)) },
            lineLimits = TextFieldLineLimits.SingleLine,
            isError = state.repsError != null,
            supportingText = state.repsError?.let { { Text(it.message()) } },
            textStyle = MaterialTheme.typography.headlineSmall,
            // Phone keypad: digits plus * + # on every platform, so "8*4" and "8+8+7" can be typed.
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            onKeyboardAction = { onSave() }, // keyboard "Done" = Save
            modifier = Modifier.fillMaxWidth().then(if (state.showsWeight) Modifier else Modifier.focusRequester(firstField)),
        )
        if (state.extraSets.isNotEmpty()) {
            Text(
                stringResource(Res.string.log_warmups_kept, state.extraSets.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(onClick = onSave, enabled = state.canSave, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(stringResource(Res.string.log_save), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(0.dp).padding(bottom = spacing.sm))
    }
}

@Composable
private fun LogSheetError.message(): String = stringResource(
    when (this) {
        LogSheetError.WEIGHT_INVALID -> Res.string.log_error_weight_invalid
        LogSheetError.WEIGHT_RANGE -> Res.string.log_error_weight_range
        LogSheetError.REPS_INVALID, LogSheetError.REPS_MISSING -> Res.string.log_error_reps_invalid
        LogSheetError.REPS_RANGE -> Res.string.log_error_reps_range
    },
)

@Composable
private fun relativeDays(at: Instant?): String {
    if (at == null) return ""
    val zone = TimeZone.currentSystemDefault()
    val days = at.toLocalDateTime(zone).date.daysUntil(Clock.System.now().toLocalDateTime(zone).date)
    return when (days) {
        0 -> stringResource(Res.string.days_today)
        1 -> stringResource(Res.string.days_yesterday)
        else -> stringResource(Res.string.days_ago, days)
    }
}
