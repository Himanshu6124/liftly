package com.himanshu.liftly.feature.workout.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.himanshu.liftly.core.designsystem.component.LiftlyCard
import com.himanshu.liftly.core.designsystem.component.MuscleDiagram
import com.himanshu.liftly.core.designsystem.component.PrBadge
import com.himanshu.liftly.core.designsystem.component.label
import com.himanshu.liftly.core.designsystem.component.workoutDisplayName
import com.himanshu.liftly.core.designsystem.theme.LiftlyTheme
import com.himanshu.liftly.core.domain.model.PrType
import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.repository.RoutineRepository
import com.himanshu.liftly.core.domain.repository.SettingsRepository
import com.himanshu.liftly.core.domain.repository.WorkoutRepository
import com.himanshu.liftly.core.domain.util.AppScope
import com.himanshu.liftly.core.domain.util.SetFormatter
import com.himanshu.liftly.core.domain.util.WorkoutStats
import com.himanshu.liftly.feature.workout.active.formatElapsed
import com.himanshu.liftly.feature.workout.resources.Res
import com.himanshu.liftly.feature.workout.resources.pr_est_1rm
import com.himanshu.liftly.feature.workout.resources.pr_max_reps
import com.himanshu.liftly.feature.workout.resources.pr_max_volume
import com.himanshu.liftly.feature.workout.resources.pr_max_weight
import com.himanshu.liftly.feature.workout.resources.snackbar_routine_saved
import com.himanshu.liftly.feature.workout.resources.snackbar_targets_updated
import com.himanshu.liftly.feature.workout.resources.summary_done
import com.himanshu.liftly.feature.workout.resources.summary_duration
import com.himanshu.liftly.feature.workout.resources.summary_exercises
import com.himanshu.liftly.feature.workout.resources.summary_exercises_value
import com.himanshu.liftly.feature.workout.resources.summary_muscles
import com.himanshu.liftly.feature.workout.resources.summary_prs
import com.himanshu.liftly.feature.workout.resources.summary_sets
import com.himanshu.liftly.feature.workout.resources.summary_title
import com.himanshu.liftly.feature.workout.resources.summary_volume
import com.himanshu.liftly.feature.workout.resources.workout_save_as_routine
import com.himanshu.liftly.feature.workout.resources.workout_update_routine
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Instant

@Immutable
data class SummaryUiState(
    val name: String = "",
    val startedAt: Instant? = null,
    val routineId: String? = null,
    val stats: WorkoutStats? = null,
    val unit: WeightUnit = WeightUnit.KG,
)

sealed interface SummaryEffect {
    data class RoutineSaved(val name: String) : SummaryEffect
    data object TargetsUpdated : SummaryEffect
}

class WorkoutSummaryViewModel(
    private val workoutId: String,
    workouts: WorkoutRepository,
    private val routines: RoutineRepository,
    settings: SettingsRepository,
    private val appScope: AppScope,
) : ViewModel() {
    val state: StateFlow<SummaryUiState> = combine(workouts.observeWorkout(workoutId), settings.settings) { w, s ->
        if (w ==
            null
        ) {
            SummaryUiState(unit = s.weightUnit)
        } else {
            SummaryUiState(w.workout.name, w.workout.startedAt, w.workout.routineId, WorkoutStats.of(w), s.weightUnit)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SummaryUiState())

    private val _effects = Channel<SummaryEffect>(Channel.BUFFERED)
    val effects: Flow<SummaryEffect> = _effects.receiveAsFlow()

    fun saveAsRoutine(name: String) = viewModelScope.launch {
        appScope.async { routines.saveWorkoutAsRoutine(workoutId, name) }.await()
        _effects.send(SummaryEffect.RoutineSaved(name.trim()))
    }

    fun updateRoutine() {
        val routineId = state.value.routineId ?: return
        viewModelScope.launch {
            appScope.async { routines.updateTargetsFromWorkout(routineId, workoutId) }.await()
            _effects.send(SummaryEffect.TargetsUpdated)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkoutSummaryScreen(
    workoutId: String,
    onDone: () -> Unit,
    viewModel: WorkoutSummaryViewModel = koinViewModel(key = "summary-$workoutId") { parametersOf(workoutId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val spacing = LiftlyTheme.spacing
    val snackbar = remember { SnackbarHostState() }
    var showSave by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { e ->
            snackbar.showSnackbar(
                when (e) {
                    is SummaryEffect.RoutineSaved -> getString(Res.string.snackbar_routine_saved, e.name)
                    SummaryEffect.TargetsUpdated -> getString(Res.string.snackbar_targets_updated)
                },
            )
        }
    }
    val stats = state.stats

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.summary_title)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(spacing.md).heightIn(min = 56.dp)) {
                Text(stringResource(Res.string.summary_done))
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            state.startedAt?.let { Text(workoutDisplayName(state.name, it), style = MaterialTheme.typography.headlineSmall) }
            if (stats != null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Stat(stringResource(Res.string.summary_duration), stats.duration?.let(::formatElapsed) ?: "–")
                    Stat(stringResource(Res.string.summary_volume), SetFormatter.weight(stats.volumeKg, state.unit))
                    Stat(stringResource(Res.string.summary_sets), stats.workingSets.toString())
                    Stat(
                        stringResource(Res.string.summary_exercises),
                        stringResource(Res.string.summary_exercises_value, stats.logged, stats.skipped),
                    )
                }
                if (stats.records.isNotEmpty()) {
                    Text(stringResource(Res.string.summary_prs), style = MaterialTheme.typography.titleMedium)
                    stats.records.forEach { (exercise, pr) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            PrBadge(exercise)
                            Text("$exercise · ${stringResource(pr.type.label())}", modifier = Modifier.weight(1f))
                            Text(
                                if (pr.type == PrType.MAX_REPS) pr.value.toLong().toString() else SetFormatter.weight(pr.value, state.unit),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                }
                if (stats.muscles.isNotEmpty()) {
                    Text(stringResource(Res.string.summary_muscles), style = MaterialTheme.typography.titleMedium)
                    MuscleDiagram(
                        intensity = stats.muscles,
                        contentDescription = stats.muscles.keys.map { it.label() }.joinToString(),
                        modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp).align(Alignment.CenterHorizontally),
                    )
                }
            }
            OutlinedButton(onClick = { showSave = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.workout_save_as_routine))
            }
            if (state.routineId != null) {
                OutlinedButton(onClick = viewModel::updateRoutine, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.workout_update_routine))
                }
            }
        }
    }
    if (showSave) {
        SaveRoutineDialog(
            defaultName = state.startedAt?.let { workoutDisplayName(state.name, it) }.orEmpty(),
            onSave = {
                showSave = false
                viewModel.saveAsRoutine(it)
            },
            onDismiss = { showSave = false },
        )
    }
}

@Composable
private fun Stat(label: String, value: String) {
    LiftlyCard(Modifier.widthIn(min = 150.dp)) {
        Column(Modifier.padding(LiftlyTheme.spacing.md)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

internal fun PrType.label() = when (this) {
    PrType.EST_1RM -> Res.string.pr_est_1rm
    PrType.MAX_WEIGHT -> Res.string.pr_max_weight
    PrType.MAX_REPS -> Res.string.pr_max_reps
    PrType.MAX_VOLUME -> Res.string.pr_max_volume
}
