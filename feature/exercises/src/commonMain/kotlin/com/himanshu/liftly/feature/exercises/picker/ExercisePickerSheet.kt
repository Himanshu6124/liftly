package com.himanshu.liftly.feature.exercises.picker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.himanshu.liftly.core.designsystem.component.LiftlyChip
import com.himanshu.liftly.core.designsystem.component.SearchFieldDefaults
import com.himanshu.liftly.core.designsystem.component.SectionHeader
import com.himanshu.liftly.core.designsystem.component.label
import com.himanshu.liftly.core.designsystem.theme.LiftlyTheme
import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.MuscleGroup
import com.himanshu.liftly.feature.exercises.library.ExerciseRowItem
import com.himanshu.liftly.feature.exercises.preview.FormPreviewSheet
import com.himanshu.liftly.feature.exercises.resources.Res
import com.himanshu.liftly.feature.exercises.resources.exercises_clear_search
import com.himanshu.liftly.feature.exercises.resources.exercises_no_results_title
import com.himanshu.liftly.feature.exercises.resources.exercises_search_hint
import com.himanshu.liftly.feature.exercises.resources.exercises_section_favorites
import com.himanshu.liftly.feature.exercises.resources.exercises_section_results
import com.himanshu.liftly.feature.exercises.resources.picker_add
import com.himanshu.liftly.feature.exercises.resources.picker_create
import com.himanshu.liftly.feature.exercises.resources.picker_form_info
import com.himanshu.liftly.feature.exercises.resources.picker_section_recent
import com.himanshu.liftly.feature.exercises.resources.picker_section_suggested
import com.himanshu.liftly.feature.exercises.resources.picker_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Multi-select exercise picker used by the active workout (and routines later).
 * Tap toggles selection; ⓘ or long-press opens the form preview; "Add (n)" confirms in selection order.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerSheet(
    contextExerciseIds: List<String>,
    onAdd: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    onOpenGuide: (String) -> Unit,
) {
    val viewModel: ExercisePickerViewModel = koinViewModel { parametersOf(contextExerciseIds) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    var previewId by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        ExercisePickerContent(
            state = state,
            query = query,
            onIntent = viewModel::onIntent,
            onPreview = { previewId = it },
            onCreate = { showCreate = true },
            onAdd = { onAdd(state.selected) },
        )
    }

    previewId?.let { id ->
        FormPreviewSheet(
            exerciseId = id,
            onDismiss = { previewId = null },
            onSeeFullGuide = {
                previewId = null
                onOpenGuide(id)
            },
        )
    }
    if (showCreate) {
        CreateExerciseDialog(
            initial = customDefaults(query, state.muscleGroup, state.equipment),
            onCreate = {
                showCreate = false
                viewModel.onIntent(ExercisePickerIntent.CreateCustom(it))
            },
            onDismiss = { showCreate = false },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExercisePickerContent(
    state: ExercisePickerUiState,
    query: String,
    onIntent: (ExercisePickerIntent) -> Unit,
    onPreview: (String) -> Unit,
    onCreate: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LiftlyTheme.spacing
    val focus = LocalFocusManager.current
    val selected = state.selected.toSet()

    Column(modifier.fillMaxHeight(0.92f)) {
        Text(
            stringResource(Res.string.picker_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = spacing.md),
        )
        TextField(
            value = query,
            onValueChange = { onIntent(ExercisePickerIntent.QueryChanged(it)) },
            placeholder = { Text(stringResource(Res.string.exercises_search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onIntent(ExercisePickerIntent.QueryChanged("")) }) {
                        Icon(Icons.Filled.Clear, contentDescription = stringResource(Res.string.exercises_clear_search))
                    }
                }
            },
            singleLine = true,
            shape = SearchFieldDefaults.shape,
            colors = SearchFieldDefaults.colors(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md, vertical = spacing.sm),
        )
        PickerFilters(state.muscleGroup, state.equipment, onIntent)

        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = spacing.md)) {
            if (state.noResults) {
                item(key = "no-results") {
                    Column(Modifier.fillMaxWidth().padding(spacing.lg), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(Res.string.exercises_no_results_title), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            if (query.isNotBlank()) {
                // Offered even when there are results: the user may want their own variation.
                item(key = "create") {
                    OutlinedButton(
                        onClick = onCreate,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md, vertical = spacing.sm),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(stringResource(Res.string.picker_create, query.trim()), Modifier.padding(start = spacing.sm))
                    }
                }
            }
            state.sections.forEach { section ->
                stickyHeader(key = "h-${section.key}", contentType = "header") {
                    SectionHeader(section.title(), Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow))
                }
                items(section.rows, key = { "${section.key}-${it.id}" }, contentType = { "row" }) { row ->
                    ExerciseRowItem(
                        row = row,
                        selected = row.id in selected,
                        onClick = { onIntent(ExercisePickerIntent.SelectionToggled(row.id)) },
                        onLongClick = { onPreview(row.id) },
                        trailing = {
                            IconButton(onClick = { onPreview(row.id) }) {
                                Icon(Icons.Filled.Info, contentDescription = stringResource(Res.string.picker_form_info, row.name))
                            }
                        },
                    )
                }
            }
        }

        HorizontalDivider()
        // Primary action at the bottom, reachable with the thumb.
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(spacing.md)) {
            Button(
                onClick = onAdd,
                enabled = state.selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Text(stringResource(Res.string.picker_add, state.selected.size))
            }
        }
    }
}

@Composable
private fun PickerSection.title(): String = when (kind) {
    PickerSection.Kind.SUGGESTED -> stringResource(Res.string.picker_section_suggested)
    PickerSection.Kind.RECENT -> stringResource(Res.string.picker_section_recent)
    PickerSection.Kind.FAVORITES -> stringResource(Res.string.exercises_section_favorites)
    PickerSection.Kind.RESULTS -> stringResource(Res.string.exercises_section_results)
    PickerSection.Kind.LETTER -> letter.toString()
}

@Composable
private fun PickerFilters(group: MuscleGroup?, equipment: Equipment?, onIntent: (ExercisePickerIntent) -> Unit) {
    val spacing = LiftlyTheme.spacing
    LazyRow(contentPadding = PaddingValues(horizontal = spacing.md), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
        items(MuscleGroup.entries, key = { "g-${it.name}" }) { g ->
            LiftlyChip(selected = group == g, onClick = {
                onIntent(ExercisePickerIntent.MuscleGroupToggled(g))
            }, label = { Text(g.label()) })
        }
        // Fixed height: a fillMaxHeight divider inside a LazyRow stretches the whole row once it's visible.
        item(key = "divider") { VerticalDivider(Modifier.padding(vertical = spacing.sm).height(32.dp)) }
        items(Equipment.entries, key = { "e-${it.name}" }) { e ->
            LiftlyChip(selected = equipment == e, onClick = {
                onIntent(ExercisePickerIntent.EquipmentToggled(e))
            }, label = { Text(e.label()) })
        }
    }
}
