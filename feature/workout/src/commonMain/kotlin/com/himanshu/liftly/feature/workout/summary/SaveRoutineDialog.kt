package com.himanshu.liftly.feature.workout.summary

import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.himanshu.liftly.feature.workout.resources.Res
import com.himanshu.liftly.feature.workout.resources.workout_cancel
import com.himanshu.liftly.feature.workout.resources.workout_routine_name
import com.himanshu.liftly.feature.workout.resources.workout_save
import com.himanshu.liftly.feature.workout.resources.workout_save_as_routine
import org.jetbrains.compose.resources.stringResource

/** Name pre-filled from the workout: usually one tap. */
@Composable
internal fun SaveRoutineDialog(defaultName: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val name = remember { TextFieldState(defaultName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.workout_save_as_routine)) },
        text = {
            OutlinedTextField(
                state = name,
                label = { Text(stringResource(Res.string.workout_routine_name)) },
                lineLimits = TextFieldLineLimits.SingleLine,
            )
        },
        confirmButton = {
            Button(onClick = { onSave(name.text.toString()) }, enabled = name.text.isNotBlank()) {
                Text(stringResource(Res.string.workout_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.workout_cancel)) } },
    )
}
