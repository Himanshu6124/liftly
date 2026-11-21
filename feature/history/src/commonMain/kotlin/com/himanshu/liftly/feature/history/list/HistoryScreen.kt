package com.himanshu.liftly.feature.history.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.himanshu.liftly.core.designsystem.component.EmptyState
import com.himanshu.liftly.core.designsystem.component.LiftlyCard
import com.himanshu.liftly.core.designsystem.component.PillTabs
import com.himanshu.liftly.core.designsystem.component.PrBadge
import com.himanshu.liftly.core.designsystem.component.SectionHeader
import com.himanshu.liftly.core.designsystem.component.floatingBarAwareInsets
import com.himanshu.liftly.core.designsystem.component.workoutDisplayName
import com.himanshu.liftly.core.designsystem.theme.LiftlyTheme
import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.model.WorkoutHistoryItem
import com.himanshu.liftly.core.domain.util.DateLabels
import com.himanshu.liftly.core.domain.util.RelativeWeek
import com.himanshu.liftly.core.domain.util.SetFormatter
import com.himanshu.liftly.core.domain.util.WeekGroup
import com.himanshu.liftly.feature.history.body.BodyScreen
import com.himanshu.liftly.feature.history.resources.Res
import com.himanshu.liftly.feature.history.resources.history_deleted
import com.himanshu.liftly.feature.history.resources.history_empty_message
import com.himanshu.liftly.feature.history.resources.history_empty_title
import com.himanshu.liftly.feature.history.resources.history_last_week
import com.himanshu.liftly.feature.history.resources.history_prs
import com.himanshu.liftly.feature.history.resources.history_row_detail
import com.himanshu.liftly.feature.history.resources.history_this_week
import com.himanshu.liftly.feature.history.resources.history_undo
import com.himanshu.liftly.feature.history.resources.history_week_of
import com.himanshu.liftly.feature.history.resources.tab_body
import com.himanshu.liftly.feature.history.resources.tab_workouts
import com.himanshu.liftly.feature.workout.active.formatElapsed
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** History: past workouts, and body measurements in a second tab. */
@Composable
fun HistoryScreen(onOpenWorkout: (String) -> Unit) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        PillTabs(
            options = listOf(stringResource(Res.string.tab_workouts), stringResource(Res.string.tab_body)),
            selectedIndex = tab,
            onSelect = { tab = it },
            modifier = Modifier.padding(horizontal = LiftlyTheme.spacing.md, vertical = LiftlyTheme.spacing.sm),
        )
        if (tab == 0) WorkoutsTab(onOpenWorkout) else BodyScreen()
    }
}

@Composable
private fun WorkoutsTab(onOpenWorkout: (String) -> Unit, viewModel: HistoryViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pending by viewModel.pendingUndo.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(pending) {
        if (pending == null) return@LaunchedEffect
        val result = snackbar.showSnackbar(
            getString(Res.string.history_deleted),
            getString(Res.string.history_undo),
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete() else viewModel.dismissUndo()
    }
    val muscleWeek by viewModel.muscleWeek.collectAsStateWithLifecycle()
    HistoryContent(state, muscleWeek, snackbar, onOpenWorkout, viewModel::showWeek)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryContent(
    state: HistoryUiState,
    muscleWeek: MuscleWeekUi,
    snackbar: SnackbarHostState,
    onOpenWorkout: (String) -> Unit,
    onShowWeek: (delta: Int) -> Unit,
) {
    val spacing = LiftlyTheme.spacing
    Scaffold(contentWindowInsets = floatingBarAwareInsets(), snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        if (!state.isLoading && state.weeks.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.DateRange,
                title = stringResource(Res.string.history_empty_title),
                message = stringResource(Res.string.history_empty_message),
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item(key = "muscles") {
                MuscleWeekCard(muscleWeek, onShowWeek, Modifier.padding(horizontal = spacing.md, vertical = spacing.sm))
            }
            state.weeks.forEach { week ->
                stickyHeader(key = "w-${week.weekStart}") {
                    SectionHeader(week.title(), Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background))
                }
                items(week.items, key = { it.workout.id }) { item ->
                    HistoryRow(item, state.unit, onClick = { onOpenWorkout(item.workout.id) })
                }
            }
        }
    }
}

@Composable
private fun WeekGroup.title(): String = when (relative) {
    RelativeWeek.THIS_WEEK -> stringResource(Res.string.history_this_week)
    RelativeWeek.LAST_WEEK -> stringResource(Res.string.history_last_week)
    RelativeWeek.EARLIER -> stringResource(Res.string.history_week_of, weekStart.toString())
}

@Composable
private fun HistoryRow(item: WorkoutHistoryItem, unit: WeightUnit, onClick: () -> Unit) {
    val spacing = LiftlyTheme.spacing
    val w = item.workout
    LiftlyCard(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md)) {
        Row(Modifier.padding(spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(workoutDisplayName(w.name, w.startedAt), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(
                        Res.string.history_row_detail,
                        DateLabels.short(w.startedAt),
                        w.endedAt?.let { formatElapsed(it - w.startedAt) } ?: "–",
                        item.exerciseCount,
                        SetFormatter.weight(item.volumeKg, unit),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.prCount > 0) PrBadge(stringResource(Res.string.history_prs, item.prCount))
        }
    }
}
