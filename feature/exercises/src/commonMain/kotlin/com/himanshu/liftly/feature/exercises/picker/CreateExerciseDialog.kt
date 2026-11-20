package com.himanshu.liftly.feature.exercises.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.himanshu.liftly.core.designsystem.component.label
import com.himanshu.liftly.core.designsystem.theme.LiftlyTheme
import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.Muscle
import com.himanshu.liftly.core.domain.model.NewCustomExercise
import com.himanshu.liftly.core.domain.model.TrackingType
import com.himanshu.liftly.feature.exercises.resources.Res
import com.himanshu.liftly.feature.exercises.resources.picker_cancel
import com.himanshu.liftly.feature.exercises.resources.picker_create_confirm
import com.himanshu.liftly.feature.exercises.resources.picker_create_equipment
import com.himanshu.liftly.feature.exercises.resources.picker_create_muscle
import com.himanshu.liftly.feature.exercises.resources.picker_create_name
import com.himanshu.liftly.feature.exercises.resources.picker_create_title
import com.himanshu.liftly.feature.exercises.resources.picker_create_tracking
import com.himanshu.liftly.feature.exercises.resources.tracking_distance
import com.himanshu.liftly.feature.exercises.resources.tracking_reps_only
import com.himanshu.liftly.feature.exercises.resources.tracking_time
import com.himanshu.liftly.feature.exercises.resources.tracking_weight_reps
import org.jetbrains.compose.resources.stringResource

/** Everything is pre-filled from the search text and active filters; usually one tap to confirm. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CreateExerciseDialog(
    initial: NewCustomExercise,
    onCreate: (NewCustomExercise) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }
    val spacing = LiftlyTheme.spacing
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.picker_create_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { draft = draft.copy(name = it) },
                    label = { Text(stringResource(Res.string.picker_create_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Label(stringResource(Res.string.picker_create_muscle))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Muscle.entries.forEach { m ->
                        FilterChip(selected = draft.primaryMuscle == m, onClick = {
                            draft = draft.copy(primaryMuscle = m)
                        }, label = { Text(m.label()) })
                    }
                }
                Label(stringResource(Res.string.picker_create_equipment))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Equipment.entries.forEach { e ->
                        FilterChip(selected = draft.equipment == e, onClick = {
                            draft = draft.copy(equipment = e)
                        }, label = { Text(e.label()) })
                    }
                }
                Label(stringResource(Res.string.picker_create_tracking))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    TrackingType.entries.forEach { t ->
                        FilterChip(selected = draft.trackingType == t, onClick = {
                            draft = draft.copy(trackingType = t)
                        }, label = { Text(t.label()) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(draft) }, enabled = draft.name.isNotBlank()) {
                Text(stringResource(Res.string.picker_create_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.picker_cancel)) } },
    )
}

@Composable
private fun Label(text: String) = Text(text, style = MaterialTheme.typography.labelLarge)

@Composable
private fun TrackingType.label(): String = stringResource(
    when (this) {
        TrackingType.WEIGHT_REPS -> Res.string.tracking_weight_reps
        TrackingType.REPS_ONLY -> Res.string.tracking_reps_only
        TrackingType.TIME -> Res.string.tracking_time
        TrackingType.DISTANCE -> Res.string.tracking_distance
    },
)
