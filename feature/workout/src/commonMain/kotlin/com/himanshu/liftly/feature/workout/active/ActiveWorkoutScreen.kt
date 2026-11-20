package com.himanshu.liftly.feature.workout.active

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.himanshu.liftly.core.designsystem.component.EmptyState
import com.himanshu.liftly.core.designsystem.component.defaultWorkoutName
import com.himanshu.liftly.core.designsystem.theme.LiftlyTheme
import com.himanshu.liftly.feature.exercises.picker.ExercisePickerSheet
import com.himanshu.liftly.feature.exercises.preview.FormPreviewSheet
import com.himanshu.liftly.feature.workout.logsheet.LogSheet
import com.himanshu.liftly.feature.workout.resources.Res
import com.himanshu.liftly.feature.workout.resources.snackbar_logged
import com.himanshu.liftly.feature.workout.resources.snackbar_removed
import com.himanshu.liftly.feature.workout.resources.snackbar_routine_saved
import com.himanshu.liftly.feature.workout.resources.snackbar_skipped
import com.himanshu.liftly.feature.workout.resources.snackbar_targets_updated
import com.himanshu.liftly.feature.workout.resources.snackbar_undo
import com.himanshu.liftly.feature.workout.resources.workout_add_exercise
import com.himanshu.liftly.feature.workout.resources.workout_cancel
import com.himanshu.liftly.feature.workout.resources.workout_discard
import com.himanshu.liftly.feature.workout.resources.workout_discard_confirm
import com.himanshu.liftly.feature.workout.resources.workout_discard_message
import com.himanshu.liftly.feature.workout.resources.workout_discard_title
import com.himanshu.liftly.feature.workout.resources.workout_empty_message
import com.himanshu.liftly.feature.workout.resources.workout_empty_title
import com.himanshu.liftly.feature.workout.resources.workout_finish
import com.himanshu.liftly.feature.workout.resources.workout_finish_anyway
import com.himanshu.liftly.feature.workout.resources.workout_finish_title
import com.himanshu.liftly.feature.workout.resources.workout_keep_going
import com.himanshu.liftly.feature.workout.resources.workout_more
import com.himanshu.liftly.feature.workout.resources.workout_rename
import com.himanshu.liftly.feature.workout.resources.workout_routine_name
import com.himanshu.liftly.feature.workout.resources.workout_save
import com.himanshu.liftly.feature.workout.resources.workout_save_as_routine
import com.himanshu.liftly.feature.workout.resources.workout_update_routine
import com.himanshu.liftly.feature.workout.rest.RestTimerButton
import com.himanshu.liftly.feature.workout.summary.SaveRoutineDialog
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun ActiveWorkoutScreen(
    onClose: (finishedWorkoutId: String?) -> Unit,
    onOpenGuide: (exerciseId: String) -> Unit,
    viewModel: ActiveWorkoutViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scrollRequests = remember { MutableSharedFlow<String>(extraBufferCapacity = 1) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ActiveWorkoutEffect.Closed -> onClose(effect.workoutId.takeIf { effect.finished })

                is ActiveWorkoutEffect.ScrollTo -> scrollRequests.tryEmit(effect.workoutExerciseId)

                is ActiveWorkoutEffect.Info -> snackbar.showSnackbar(
                    when (effect.kind) {
                        ActiveWorkoutEffect.Info.Kind.ROUTINE_SAVED -> getString(Res.string.snackbar_routine_saved, effect.arg)
                        ActiveWorkoutEffect.Info.Kind.TARGETS_UPDATED -> getString(Res.string.snackbar_targets_updated)
                    },
                )

                is ActiveWorkoutEffect.Undoable -> {
                    val message = getString(
                        when (effect.kind) {
                            ActiveWorkoutEffect.Undoable.Kind.SKIPPED -> Res.string.snackbar_skipped
                            ActiveWorkoutEffect.Undoable.Kind.REMOVED -> Res.string.snackbar_removed
                            ActiveWorkoutEffect.Undoable.Kind.LOGGED -> Res.string.snackbar_logged
                        },
                        effect.exerciseName,
                    )
                    snackbar.currentSnackbarData?.dismiss()
                    val result = snackbar.showSnackbar(message, getString(Res.string.snackbar_undo), duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) viewModel.onIntent(ActiveWorkoutIntent.Undo)
                }
            }
        }
    }
    // Opened with nothing in progress (e.g. finished on another screen): just leave.
    LaunchedEffect(state.noWorkout) { if (state.noWorkout) onClose(null) }

    ActiveWorkoutContent(
        state = state,
        snackbar = snackbar,
        scrollRequests = scrollRequests,
        onIntent = viewModel::onIntent,
        onOpenGuide = onOpenGuide,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutContent(
    state: ActiveWorkoutUiState,
    snackbar: SnackbarHostState,
    scrollRequests: Flow<String>,
    onIntent: (ActiveWorkoutIntent) -> Unit,
    onOpenGuide: (String) -> Unit,
) {
    val spacing = LiftlyTheme.spacing
    var showPicker by remember { mutableStateOf(false) }
    var previewId by remember { mutableStateOf<String?>(null) }

    // Local copy so drag-reordering is instant; the DB order is written once the drag ends.
    var cards by remember { mutableStateOf(state.cards) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(state.cards) { if (!dragging) cards = state.cards }

    val listState = rememberLazyListState()
    LaunchedEffect(scrollRequests) {
        scrollRequests.collect { id ->
            val index = cards.indexOfFirst { it.workoutExerciseId == id }
            if (index >= 0) listState.animateScrollToItem(index)
        }
    }
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        cards = cards.toMutableList().apply { add(to.index, removeAt(from.index)) }.toImmutableList()
    }

    Scaffold(
        topBar = { WorkoutTopBar(state, onIntent) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (state.cards.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(LiftlyTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    RestTimerButton(state.restEndsAt, state.restSuggestion, state.restRemembered, onIntent)
                    ExtendedFloatingActionButton(
                        onClick = { showPicker = true },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text(stringResource(Res.string.workout_add_exercise)) },
                    )
                }
            }
        },
    ) { padding ->
        if (!state.isLoading && state.cards.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Add,
                title = stringResource(Res.string.workout_empty_title),
                message = stringResource(Res.string.workout_empty_message),
                actionLabel = stringResource(Res.string.workout_add_exercise),
                onAction = { showPicker = true },
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = spacing.md, end = spacing.md, top = spacing.sm, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                items(cards, key = { it.workoutExerciseId }) { card ->
                    ReorderableItem(reorderState, key = card.workoutExerciseId) { isDragging ->
                        SwipeableExerciseCard(
                            card = card,
                            isDragging = isDragging,
                            highlighted = state.highlightedId == card.workoutExerciseId,
                            onClick = { onIntent(ActiveWorkoutIntent.OpenLogSheet(card.workoutExerciseId)) },
                            onFormGuide = { previewId = card.exerciseId },
                            onIntent = onIntent,
                            modifier = Modifier.longPressDraggableHandle(
                                onDragStarted = { dragging = true },
                                onDragStopped = {
                                    dragging = false
                                    onIntent(ActiveWorkoutIntent.CommitOrder(cards.map { it.workoutExerciseId }))
                                },
                            ),
                        )
                    }
                }
            }
        }
    }

    if (showPicker) {
        ExercisePickerSheet(
            contextExerciseIds = state.exerciseIds,
            onAdd = { ids ->
                showPicker = false
                onIntent(ActiveWorkoutIntent.AddExercises(ids))
            },
            onDismiss = { showPicker = false },
            onOpenGuide = { id ->
                showPicker = false
                onOpenGuide(id)
            },
        )
    }
    previewId?.let { id ->
        FormPreviewSheet(exerciseId = id, onDismiss = { previewId = null }, onSeeFullGuide = {
            previewId = null
            onOpenGuide(id)
        })
    }
    state.logSheetFor?.let { id ->
        LogSheet(
            workoutExerciseId = id,
            onSaved = { _, records, undo -> onIntent(ActiveWorkoutIntent.Logged(id, undo, records.isNotEmpty())) },
            onDismiss = { onIntent(ActiveWorkoutIntent.CloseLogSheet) },
            onFormGuide = { exerciseId -> previewId = exerciseId },
        )
    }
    if (state.showFinishSheet) FinishSheet(state.pendingCount, onIntent)
    if (state.showDiscardDialog) DiscardDialog(onIntent)
    if (state.showSaveRoutineDialog) {
        SaveRoutineDialog(
            defaultName = state.name.ifBlank { state.startedAt?.let { defaultWorkoutName(it) }.orEmpty() },
            onSave = { onIntent(ActiveWorkoutIntent.SaveAsRoutine(it)) },
            onDismiss = { onIntent(ActiveWorkoutIntent.SaveAsRoutineCancelled) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutTopBar(state: ActiveWorkoutUiState, onIntent: (ActiveWorkoutIntent) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val displayName = state.name.ifBlank { state.startedAt?.let { defaultWorkoutName(it) }.orEmpty() }

    TopAppBar(
        title = {
            if (editing) {
                var draft by remember { mutableStateOf(state.name.ifBlank { displayName }) }
                val focus = remember { FocusRequester() }
                LaunchedEffect(Unit) { focus.requestFocus() }
                TextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        editing = false
                        onIntent(ActiveWorkoutIntent.Rename(draft))
                    }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                    modifier = Modifier.focusRequester(focus),
                )
            } else {
                Column {
                    TextButton(onClick = { editing = true }, contentPadding = PaddingValues(0.dp)) {
                        Text(
                            displayName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                    }
                    state.startedAt?.let { ElapsedTime(it) }
                }
            }
        },
        actions = {
            Button(onClick = { onIntent(ActiveWorkoutIntent.FinishRequested) }) { Text(stringResource(Res.string.workout_finish)) }
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(Res.string.workout_more))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text(stringResource(Res.string.workout_rename)) }, onClick = {
                    menuOpen = false
                    editing = true
                })
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.workout_save_as_routine)) },
                    onClick = {
                        menuOpen = false
                        onIntent(ActiveWorkoutIntent.SaveAsRoutineRequested)
                    },
                )
                if (state.routineId != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.workout_update_routine)) },
                        onClick = {
                            menuOpen = false
                            onIntent(ActiveWorkoutIntent.UpdateRoutineTargets)
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.workout_discard), color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menuOpen = false
                        onIntent(ActiveWorkoutIntent.DiscardRequested)
                    },
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinishSheet(pendingCount: Int, onIntent: (ActiveWorkoutIntent) -> Unit) {
    val spacing = LiftlyTheme.spacing
    ModalBottomSheet(onDismissRequest = { onIntent(ActiveWorkoutIntent.FinishCancelled) }) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = spacing.md).navigationBarsPadding().padding(bottom = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(stringResource(Res.string.workout_finish_title, pendingCount), style = MaterialTheme.typography.titleLarge)
            Button(onClick = { onIntent(ActiveWorkoutIntent.FinishConfirmed) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(stringResource(Res.string.workout_finish_anyway))
            }
            OutlinedButton(
                onClick = { onIntent(ActiveWorkoutIntent.FinishCancelled) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Text(stringResource(Res.string.workout_keep_going))
            }
        }
    }
}

/** The one destructive action that asks for confirmation (everything else has Undo). */
@Composable
private fun DiscardDialog(onIntent: (ActiveWorkoutIntent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onIntent(ActiveWorkoutIntent.DiscardCancelled) },
        title = { Text(stringResource(Res.string.workout_discard_title)) },
        text = { Text(stringResource(Res.string.workout_discard_message)) },
        confirmButton = {
            Button(
                onClick = { onIntent(ActiveWorkoutIntent.DiscardConfirmed) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(Res.string.workout_discard_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = { onIntent(ActiveWorkoutIntent.DiscardCancelled) }) {
                Text(stringResource(Res.string.workout_cancel))
            }
        },
    )
}
