package com.himanshu.liftly.feature.routines.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.himanshu.liftly.core.designsystem.component.EmptyState
import com.himanshu.liftly.core.designsystem.component.LiftlyCard
import com.himanshu.liftly.core.designsystem.component.floatingBarAwareInsets
import com.himanshu.liftly.core.designsystem.theme.LiftlyTheme
import com.himanshu.liftly.feature.routines.resources.Res
import com.himanshu.liftly.feature.routines.resources.routines_delete
import com.himanshu.liftly.feature.routines.resources.routines_deleted
import com.himanshu.liftly.feature.routines.resources.routines_edit
import com.himanshu.liftly.feature.routines.resources.routines_empty_message
import com.himanshu.liftly.feature.routines.resources.routines_empty_title
import com.himanshu.liftly.feature.routines.resources.routines_exercises
import com.himanshu.liftly.feature.routines.resources.routines_new
import com.himanshu.liftly.feature.routines.resources.routines_options
import com.himanshu.liftly.feature.routines.resources.routines_start
import com.himanshu.liftly.feature.routines.resources.routines_undo
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun RoutinesScreen(
    onOpenWorkout: () -> Unit,
    onEditRoutine: (String?) -> Unit,
    viewModel: RoutinesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                RoutinesEffect.OpenWorkout -> onOpenWorkout()

                is RoutinesEffect.Deleted -> {
                    val result = snackbar.showSnackbar(
                        getString(Res.string.routines_deleted, effect.name),
                        getString(Res.string.routines_undo),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.onIntent(RoutinesIntent.UndoDelete)
                }
            }
        }
    }
    RoutinesContent(state, snackbar, viewModel::onIntent, onEditRoutine)
}

@Composable
fun RoutinesContent(
    state: RoutinesUiState,
    snackbar: SnackbarHostState,
    onIntent: (RoutinesIntent) -> Unit,
    onEditRoutine: (String?) -> Unit,
) {
    val spacing = LiftlyTheme.spacing
    var rows by remember { mutableStateOf(state.routines) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(state.routines) { if (!dragging) rows = state.routines }
    val listState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        rows = rows.toMutableList().apply { add(to.index, removeAt(from.index)) }.toImmutableList()
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        contentWindowInsets = floatingBarAwareInsets(),
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onEditRoutine(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(Res.string.routines_new)) },
            )
        },
    ) { padding ->
        if (!state.isLoading && rows.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.List,
                title = stringResource(Res.string.routines_empty_title),
                message = stringResource(Res.string.routines_empty_message),
                actionLabel = stringResource(Res.string.routines_new),
                onAction = { onEditRoutine(null) },
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = spacing.md, end = spacing.md, top = spacing.md, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            items(rows, key = { it.id }) { row ->
                ReorderableItem(reorder, key = row.id) {
                    RoutineCard(
                        row = row,
                        onStart = { onIntent(RoutinesIntent.Start(row.id)) },
                        onEdit = { onEditRoutine(row.id) },
                        onDelete = { onIntent(RoutinesIntent.Delete(row.id)) },
                        modifier = Modifier.longPressDraggableHandle(
                            onDragStarted = { dragging = true },
                            onDragStopped = {
                                dragging = false
                                onIntent(RoutinesIntent.Reorder(rows.map { it.id }))
                            },
                        ),
                    )
                }
            }
        }
    }
}

/** ▶ starts it (1 tap); tapping the card does [onClick] (edit on the Routines tab, start on Home). */
@Composable
fun RoutineCard(
    row: RoutineRow,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = onEdit,
) {
    val spacing = LiftlyTheme.spacing
    var menu by remember { mutableStateOf(false) }
    LiftlyCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Column(Modifier.weight(1f)) {
                Text(row.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(Res.string.routines_exercises, row.exerciseCount) +
                        if (row.exerciseNames.isNotEmpty()) " · ${row.exerciseNames}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(Res.string.routines_options, row.name))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(Res.string.routines_edit)) }, onClick = {
                        menu = false
                        onEdit()
                    })
                    if (onDelete != null) {
                        DropdownMenuItem(text = { Text(stringResource(Res.string.routines_delete)) }, onClick = {
                            menu = false
                            onDelete()
                        })
                    }
                }
            }
            FilledIconButton(onClick = onStart, enabled = row.exerciseCount > 0, modifier = Modifier.padding(start = spacing.xs)) {
                Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(Res.string.routines_start, row.name))
            }
        }
    }
}
