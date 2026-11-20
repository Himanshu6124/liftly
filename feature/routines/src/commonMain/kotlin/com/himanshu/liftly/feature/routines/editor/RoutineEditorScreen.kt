package com.himanshu.liftly.feature.routines.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.himanshu.liftly.core.designsystem.component.ExerciseThumbnail
import com.himanshu.liftly.core.designsystem.theme.LiftlyTheme
import com.himanshu.liftly.core.domain.util.SetFormatter.symbol
import com.himanshu.liftly.feature.exercises.picker.ExercisePickerSheet
import com.himanshu.liftly.feature.routines.resources.Res
import com.himanshu.liftly.feature.routines.resources.editor_add_exercises
import com.himanshu.liftly.feature.routines.resources.editor_back
import com.himanshu.liftly.feature.routines.resources.editor_edit_title
import com.himanshu.liftly.feature.routines.resources.editor_empty
import com.himanshu.liftly.feature.routines.resources.editor_move_down
import com.himanshu.liftly.feature.routines.resources.editor_move_up
import com.himanshu.liftly.feature.routines.resources.editor_name
import com.himanshu.liftly.feature.routines.resources.editor_name_hint
import com.himanshu.liftly.feature.routines.resources.editor_new_title
import com.himanshu.liftly.feature.routines.resources.editor_remove
import com.himanshu.liftly.feature.routines.resources.editor_reps
import com.himanshu.liftly.feature.routines.resources.editor_save
import com.himanshu.liftly.feature.routines.resources.editor_sets
import com.himanshu.liftly.feature.routines.resources.editor_weight
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorScreen(
    routineId: String?,
    onDone: () -> Unit,
    onOpenGuide: (String) -> Unit,
    viewModel: RoutineEditorViewModel = koinViewModel(key = "routine-editor-${routineId ?: "new"}") { parametersOf(routineId) },
) {
    val spacing = LiftlyTheme.spacing
    var showPicker by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel) { viewModel.saved.collect { onDone() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (viewModel.isNew) Res.string.editor_new_title else Res.string.editor_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.editor_back))
                    }
                },
                actions = {
                    Button(onClick = viewModel::save, enabled = viewModel.canSave, modifier = Modifier.padding(end = spacing.sm)) {
                        Text(stringResource(Res.string.editor_save))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item(key = "name") {
                OutlinedTextField(
                    state = viewModel.name,
                    label = { Text(stringResource(Res.string.editor_name)) },
                    placeholder = { Text(stringResource(Res.string.editor_name_hint)) },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (!viewModel.isLoading && viewModel.items.isEmpty()) {
                item(key = "empty") {
                    Text(
                        stringResource(Res.string.editor_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            itemsIndexed(viewModel.items, key = { i, item -> item.lineId ?: "new-$i-${item.exerciseId}" }) { _, item ->
                EditorRow(item, viewModel.unit.symbol, onRemove = { viewModel.remove(item) }, onMove = { viewModel.move(item, it) })
            }
            item(key = "add") {
                FilledTonalButton(onClick = { showPicker = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(stringResource(Res.string.editor_add_exercises), Modifier.padding(start = spacing.sm))
                }
            }
        }
    }

    if (showPicker) {
        ExercisePickerSheet(
            contextExerciseIds = viewModel.items.map { it.exerciseId },
            onAdd = { ids ->
                showPicker = false
                viewModel.addExercises(ids)
            },
            onDismiss = { showPicker = false },
            onOpenGuide = { id ->
                showPicker = false
                onOpenGuide(id)
            },
        )
    }
}

@Composable
private fun EditorRow(item: EditorItem, unit: String, onRemove: () -> Unit, onMove: (Int) -> Unit) {
    val spacing = LiftlyTheme.spacing
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(spacing.sm), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                ExerciseThumbnail(item.imagePaths, contentDescription = null)
                Text(item.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { onMove(-1) }) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(Res.string.editor_move_up, item.name))
                }
                IconButton(onClick = { onMove(1) }) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(Res.string.editor_move_down, item.name))
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.editor_remove, item.name))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Target(item.sets, stringResource(Res.string.editor_sets), null, KeyboardType.Number, Modifier.weight(1f))
                Target(item.reps, stringResource(Res.string.editor_reps), null, KeyboardType.Number, Modifier.weight(1f))
                Target(item.weight, stringResource(Res.string.editor_weight), unit, KeyboardType.Decimal, Modifier.weight(1.3f))
            }
        }
    }
}

@Composable
private fun Target(field: TextFieldState, label: String, suffix: String?, type: KeyboardType, modifier: Modifier) {
    OutlinedTextField(
        state = field,
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(keyboardType = type),
        modifier = modifier,
    )
}
