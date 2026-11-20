package com.himanshu.liftly.feature.history.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.himanshu.liftly.core.designsystem.component.ExerciseThumbnail
import com.himanshu.liftly.core.designsystem.component.PrBadge
import com.himanshu.liftly.core.designsystem.component.workoutDisplayName
import com.himanshu.liftly.core.designsystem.theme.LiftlyTheme
import com.himanshu.liftly.core.domain.model.ActiveWorkout
import com.himanshu.liftly.core.domain.model.ActiveWorkoutExercise
import com.himanshu.liftly.core.domain.model.ExerciseStatus
import com.himanshu.liftly.core.domain.model.SetType
import com.himanshu.liftly.core.domain.model.UserSettings
import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.repository.SettingsRepository
import com.himanshu.liftly.core.domain.repository.WorkoutRepository
import com.himanshu.liftly.core.domain.util.SetFormatter
import com.himanshu.liftly.core.domain.util.toDraftSets
import com.himanshu.liftly.feature.history.HistoryUndo
import com.himanshu.liftly.feature.history.resources.Res
import com.himanshu.liftly.feature.history.resources.history_back
import com.himanshu.liftly.feature.history.resources.history_delete
import com.himanshu.liftly.feature.history.resources.history_more
import com.himanshu.liftly.feature.history.resources.history_not_logged
import com.himanshu.liftly.feature.history.resources.history_skipped
import com.himanshu.liftly.feature.history.resources.history_tap_to_edit
import com.himanshu.liftly.feature.history.resources.history_warmup
import com.himanshu.liftly.feature.workout.logsheet.LogSheet
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class WorkoutDetailViewModel(
    private val workoutId: String,
    private val workouts: WorkoutRepository,
    settings: SettingsRepository,
    private val undo: HistoryUndo,
) : ViewModel() {
    val state: StateFlow<Pair<ActiveWorkout?, UserSettings>> = combine(workouts.observeWorkout(workoutId), settings.settings) { w, s ->
        w to
            s
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null to UserSettings())

    /** Deletes, then hands Undo to the History list (this screen closes right away). */
    fun delete(onDeleted: () -> Unit) = viewModelScope.launch {
        workouts.deleteWorkout(workoutId)?.let(undo::offer)
        onDeleted()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    workoutId: String,
    onBack: () -> Unit,
    onOpenGuide: (String) -> Unit,
    viewModel: WorkoutDetailViewModel = koinViewModel(key = "history-$workoutId") { parametersOf(workoutId) },
) {
    val (workout, settings) = viewModel.state.collectAsStateWithLifecycle().value
    val spacing = LiftlyTheme.spacing
    var menu by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workout?.let { workoutDisplayName(it.workout.name, it.workout.startedAt) }.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.history_back))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(Res.string.history_more))
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.history_delete), color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menu = false
                                    viewModel.delete(onBack) // Undo is offered on the History list
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        val w = workout ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item(key = "hint") {
                Text(
                    stringResource(Res.string.history_tap_to_edit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(w.exercises, key = { it.workoutExercise.id }) { e ->
                ExerciseBlock(e, settings.weightUnit, onClick = { editing = e.workoutExercise.id })
            }
        }
    }

    editing?.let { id ->
        LogSheet(workoutExerciseId = id, onSaved = { _, _, _ -> editing = null }, onDismiss = { editing = null }, onFormGuide = onOpenGuide)
    }
}

@Composable
private fun ExerciseBlock(e: ActiveWorkoutExercise, unit: WeightUnit, onClick: () -> Unit) {
    val spacing = LiftlyTheme.spacing
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                ExerciseThumbnail(e.exercise.imagePaths, contentDescription = null, size = 40.dp)
                Text(e.exercise.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (e.personalRecords.isNotEmpty()) PrBadge(e.exercise.name)
            }
            when (e.workoutExercise.status) {
                ExerciseStatus.SKIPPED -> Text(
                    stringResource(Res.string.history_skipped),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                ExerciseStatus.PENDING -> Text(
                    stringResource(Res.string.history_not_logged),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                ExerciseStatus.LOGGED -> e.sets.toDraftSets().forEachIndexed { i, s ->
                    val single = SetFormatter.single(s, unit)
                    val prefix = if (s.setType == SetType.WARMUP) stringResource(Res.string.history_warmup) else "${i + 1}."
                    val rpe = s.rpe?.let { " @ RPE ${SetFormatter.number(it, WeightUnit.KG)}" }.orEmpty()
                    Text("$prefix  $single$rpe", style = MaterialTheme.typography.bodyMedium)
                }
            }
            e.workoutExercise.notes?.let {
                Text("“$it”", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
