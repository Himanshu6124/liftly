package com.himanshu.liftly.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.himanshu.liftly.core.designsystem.component.SectionHeader
import com.himanshu.liftly.core.designsystem.component.workoutDisplayName
import com.himanshu.liftly.core.designsystem.theme.LiftlyTheme
import com.himanshu.liftly.core.domain.util.SetFormatter
import com.himanshu.liftly.feature.home.resources.Res
import com.himanshu.liftly.feature.home.resources.home_greeting
import com.himanshu.liftly.feature.home.resources.home_in_progress
import com.himanshu.liftly.feature.home.resources.home_in_progress_detail
import com.himanshu.liftly.feature.home.resources.home_last_workout
import com.himanshu.liftly.feature.home.resources.home_last_workout_detail
import com.himanshu.liftly.feature.home.resources.home_new_routine
import com.himanshu.liftly.feature.home.resources.home_next_up
import com.himanshu.liftly.feature.home.resources.home_no_routines
import com.himanshu.liftly.feature.home.resources.home_resume
import com.himanshu.liftly.feature.home.resources.home_routine_exercises
import com.himanshu.liftly.feature.home.resources.home_start_empty
import com.himanshu.liftly.feature.home.resources.home_start_routine
import com.himanshu.liftly.feature.home.resources.home_sync_failed
import com.himanshu.liftly.feature.home.resources.home_sync_local_only
import com.himanshu.liftly.feature.home.resources.home_sync_synced
import com.himanshu.liftly.feature.home.resources.home_sync_syncing
import com.himanshu.liftly.feature.home.resources.home_your_routines
import com.himanshu.liftly.feature.routines.list.RoutineCard
import com.himanshu.liftly.feature.routines.list.RoutineRow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    onOpenActiveWorkout: () -> Unit,
    onEditRoutine: (String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                HomeEffect.OpenActiveWorkout -> onOpenActiveWorkout()
            }
        }
    }
    HomeContent(state = state, onIntent = viewModel::onIntent, onEditRoutine = onEditRoutine, modifier = modifier)
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onIntent: (HomeIntent) -> Unit,
    onEditRoutine: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LiftlyTheme.spacing
    LazyColumn(
        modifier = modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item(key = "greeting") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(stringResource(Res.string.home_greeting), style = MaterialTheme.typography.headlineMedium)
                AssistChip(onClick = {}, label = { Text(state.syncLabel.text()) })
            }
        }
        // One big primary action: resume if something is running, else the next routine.
        item(key = "primary") {
            when {
                state.inProgress != null -> ResumeCard(state.inProgress) { onIntent(HomeIntent.ResumeWorkout) }
                state.nextRoutine != null -> NextRoutineCard(state.nextRoutine) { onIntent(HomeIntent.StartRoutine(state.nextRoutine.id)) }
            }
        }
        if (state.isLoading) return@LazyColumn
        if (state.inProgress == null) {
            item(key = "empty") {
                OutlinedButton(onClick = {
                    onIntent(HomeIntent.StartEmptyWorkout)
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text(stringResource(Res.string.home_start_empty))
                }
            }
        }
        state.calendar?.let { cal -> item(key = "consistency") { ConsistencyCard(cal) } }
        state.lastWorkout?.let { last -> item(key = "last") { LastWorkout(last, state) } }

        item(key = "routines-header") { SectionHeader(stringResource(Res.string.home_your_routines), Modifier.padding(top = spacing.sm)) }
        if (state.routines.isEmpty()) {
            item(key = "no-routines") {
                Text(
                    stringResource(Res.string.home_no_routines),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.routines, key = { it.id }) { row ->
            RoutineCard(
                row = row,
                onStart = { onIntent(HomeIntent.StartRoutine(row.id)) },
                onEdit = { onEditRoutine(row.id) },
                onDelete = null,
                onClick = { onIntent(HomeIntent.StartRoutine(row.id)) }, // Home: tap = start
            )
        }
        item(key = "new-routine") {
            TextButton(onClick = { onEditRoutine(null) }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(Res.string.home_new_routine), Modifier.padding(start = spacing.sm))
            }
        }
    }
}

@Composable
private fun NextRoutineCard(routine: RoutineRow, onStart: () -> Unit) {
    val spacing = LiftlyTheme.spacing
    Card(
        onClick = onStart,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(stringResource(Res.string.home_next_up).uppercase(), style = MaterialTheme.typography.labelMedium)
            Text(routine.name, style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(Res.string.home_routine_exercises, routine.exerciseCount, routine.exerciseNames),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(stringResource(Res.string.home_start_routine, routine.name), Modifier.padding(start = spacing.sm))
            }
        }
    }
}

@Composable
private fun ResumeCard(banner: InProgressBanner, onClick: () -> Unit) {
    val spacing = LiftlyTheme.spacing
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(stringResource(Res.string.home_in_progress).uppercase(), style = MaterialTheme.typography.labelMedium)
            Text(workoutDisplayName(banner.name, banner.startedAt), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(Res.string.home_in_progress_detail, banner.loggedCount, banner.exerciseCount),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(stringResource(Res.string.home_resume), Modifier.padding(start = spacing.sm))
            }
        }
    }
}

@Composable
private fun LastWorkout(last: LastWorkoutCard, state: HomeUiState) {
    val spacing = LiftlyTheme.spacing
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(stringResource(Res.string.home_last_workout).uppercase(), style = MaterialTheme.typography.labelMedium)
            Text(workoutDisplayName(last.name, last.startedAt), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    Res.string.home_last_workout_detail,
                    last.startedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString(),
                    last.loggedCount,
                    last.exerciseCount,
                    SetFormatter.weight(last.volumeKg, state.unit),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SyncLabel.text() = stringResource(
    when (this) {
        SyncLabel.LOCAL_ONLY -> Res.string.home_sync_local_only
        SyncLabel.SYNCING -> Res.string.home_sync_syncing
        SyncLabel.SYNCED -> Res.string.home_sync_synced
        SyncLabel.FAILED -> Res.string.home_sync_failed
    },
)
