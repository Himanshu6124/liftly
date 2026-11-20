package com.himanshu.liftly.feature.exercises.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.himanshu.liftly.core.designsystem.component.ExerciseDemo
import com.himanshu.liftly.core.designsystem.component.MuscleDiagram
import com.himanshu.liftly.core.designsystem.component.label
import com.himanshu.liftly.core.designsystem.component.muscleIntensity
import com.himanshu.liftly.core.designsystem.theme.LiftlyColors
import com.himanshu.liftly.core.designsystem.theme.LiftlyTheme
import com.himanshu.liftly.feature.exercises.resources.Res
import com.himanshu.liftly.feature.exercises.resources.detail_cues
import com.himanshu.liftly.feature.exercises.resources.detail_demo_description
import com.himanshu.liftly.feature.exercises.resources.preview_no_guide
import com.himanshu.liftly.feature.exercises.resources.preview_see_full_guide
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Quick look at form without leaving the current screen: demo, muscles, key cues. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormPreviewSheet(exerciseId: String, onDismiss: () -> Unit, onSeeFullGuide: () -> Unit) {
    val viewModel: FormPreviewViewModel = koinViewModel(key = "preview-$exerciseId") { parametersOf(exerciseId) }
    val exercise by viewModel.exercise.collectAsStateWithLifecycle()
    val spacing = LiftlyTheme.spacing

    ModalBottomSheet(onDismissRequest = onDismiss) {
        val e = exercise ?: return@ModalBottomSheet
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = spacing.md).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(e.name, style = MaterialTheme.typography.titleLarge)
            ExerciseDemo(
                imagePaths = e.imagePaths,
                contentDescription = stringResource(Res.string.detail_demo_description, e.name),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                MuscleDiagram(
                    intensity = muscleIntensity(e.primaryMuscle, e.secondaryMuscles),
                    contentDescription = (listOf(e.primaryMuscle) + e.secondaryMuscles).map { it.label() }.joinToString(),
                    modifier = Modifier.widthIn(max = 140.dp),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Text(e.primaryMuscle.label(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    e.secondaryMuscles.forEach { Text(it.label(), style = MaterialTheme.typography.bodyMedium) }
                }
            }
            if (e.formCues.isEmpty()) {
                Text(stringResource(Res.string.preview_no_guide), style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(stringResource(Res.string.detail_cues), style = MaterialTheme.typography.titleMedium)
                e.formCues.forEach { cue ->
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = LiftlyColors.Success,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(cue, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            TextButton(onClick = onSeeFullGuide, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(Res.string.preview_see_full_guide))
            }
        }
    }
}
