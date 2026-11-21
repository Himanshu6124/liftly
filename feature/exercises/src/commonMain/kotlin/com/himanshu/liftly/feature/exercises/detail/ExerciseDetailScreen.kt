package com.himanshu.liftly.feature.exercises.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.himanshu.liftly.core.designsystem.component.EmptyState
import com.himanshu.liftly.core.designsystem.component.ExerciseDemo
import com.himanshu.liftly.core.designsystem.component.LiftlyCard
import com.himanshu.liftly.core.designsystem.component.MuscleDiagram
import com.himanshu.liftly.core.designsystem.component.PillTabs
import com.himanshu.liftly.core.designsystem.component.label
import com.himanshu.liftly.core.designsystem.component.muscleIntensity
import com.himanshu.liftly.core.designsystem.theme.LiftlyColors
import com.himanshu.liftly.core.designsystem.theme.LiftlyTheme
import com.himanshu.liftly.core.domain.model.PrType
import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.util.DateLabels
import com.himanshu.liftly.core.domain.util.SetFormatter
import com.himanshu.liftly.feature.exercises.resources.Res
import com.himanshu.liftly.feature.exercises.resources.detail_back
import com.himanshu.liftly.feature.exercises.resources.detail_cues
import com.himanshu.liftly.feature.exercises.resources.detail_demo_description
import com.himanshu.liftly.feature.exercises.resources.detail_history_empty_message
import com.himanshu.liftly.feature.exercises.resources.detail_history_empty_title
import com.himanshu.liftly.feature.exercises.resources.detail_how_to
import com.himanshu.liftly.feature.exercises.resources.detail_mistakes
import com.himanshu.liftly.feature.exercises.resources.detail_muscles
import com.himanshu.liftly.feature.exercises.resources.detail_not_found
import com.himanshu.liftly.feature.exercises.resources.detail_note
import com.himanshu.liftly.feature.exercises.resources.detail_note_hint
import com.himanshu.liftly.feature.exercises.resources.detail_primary
import com.himanshu.liftly.feature.exercises.resources.detail_records_empty_message
import com.himanshu.liftly.feature.exercises.resources.detail_records_empty_title
import com.himanshu.liftly.feature.exercises.resources.detail_secondary
import com.himanshu.liftly.feature.exercises.resources.detail_tab_form
import com.himanshu.liftly.feature.exercises.resources.detail_tab_history
import com.himanshu.liftly.feature.exercises.resources.detail_tab_records
import com.himanshu.liftly.feature.exercises.resources.detail_watch_video
import com.himanshu.liftly.feature.exercises.resources.exercises_favorite_add
import com.himanshu.liftly.feature.exercises.resources.exercises_favorite_remove
import com.himanshu.liftly.feature.exercises.resources.record_est_1rm
import com.himanshu.liftly.feature.exercises.resources.record_max_reps
import com.himanshu.liftly.feature.exercises.resources.record_max_volume
import com.himanshu.liftly.feature.exercises.resources.record_max_weight
import com.himanshu.liftly.feature.exercises.resources.record_reps_value
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseDetailViewModel = koinViewModel(key = exerciseId) { parametersOf(exerciseId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ExerciseDetailContent(state = state, onIntent = viewModel::onIntent, onBack = onBack, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailContent(
    state: ExerciseDetailUiState,
    onIntent: (ExerciseDetailIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val exercise = state.exercise
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(exercise?.name.orEmpty(), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.detail_back))
                    }
                },
                actions = {
                    if (exercise != null) {
                        IconButton(onClick = { onIntent(ExerciseDetailIntent.ToggleFavorite) }) {
                            Icon(
                                imageVector = if (exercise.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = stringResource(
                                    if (exercise.isFavorite) Res.string.exercises_favorite_remove else Res.string.exercises_favorite_add,
                                    exercise.name,
                                ),
                                tint = if (exercise.isFavorite) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.notFound -> EmptyState(Icons.Filled.Close, stringResource(Res.string.detail_not_found), "")

                exercise == null -> Unit

                else -> {
                    PillTabs(
                        options = DetailTab.entries.map { stringResource(it.title()) },
                        selectedIndex = state.tab.ordinal,
                        onSelect = { onIntent(ExerciseDetailIntent.TabSelected(DetailTab.entries[it])) },
                        modifier = Modifier.padding(horizontal = LiftlyTheme.spacing.md, vertical = LiftlyTheme.spacing.sm),
                    )
                    when (state.tab) {
                        DetailTab.FORM -> FormTab(exercise, state.noteDraft, onIntent)

                        DetailTab.HISTORY -> if (state.sessions.isEmpty()) {
                            EmptyState(
                                Icons.Filled.DateRange,
                                stringResource(Res.string.detail_history_empty_title),
                                stringResource(Res.string.detail_history_empty_message),
                            )
                        } else {
                            HistoryTab(state, onIntent)
                        }

                        DetailTab.RECORDS -> if (state.records.isEmpty()) {
                            EmptyState(
                                Icons.Filled.Star,
                                stringResource(Res.string.detail_records_empty_title),
                                stringResource(Res.string.detail_records_empty_message),
                            )
                        } else {
                            RecordsTab(state.records, state.unit)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordsTab(records: List<RecordUi>, unit: WeightUnit) {
    val spacing = LiftlyTheme.spacing
    LazyColumn(contentPadding = PaddingValues(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        items(records.size, key = { records[it].type.name }) { i ->
            val r = records[i]
            LiftlyCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(r.type.title()), style = MaterialTheme.typography.titleSmall)
                        Text(
                            DateLabels.short(r.achievedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        when (r.type) {
                            PrType.MAX_REPS -> stringResource(Res.string.record_reps_value, r.value.toLong().toString())
                            else -> SetFormatter.weight(r.value, unit)
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private fun PrType.title() = when (this) {
    PrType.EST_1RM -> Res.string.record_est_1rm
    PrType.MAX_WEIGHT -> Res.string.record_max_weight
    PrType.MAX_REPS -> Res.string.record_max_reps
    PrType.MAX_VOLUME -> Res.string.record_max_volume
}

private fun DetailTab.title() = when (this) {
    DetailTab.FORM -> Res.string.detail_tab_form
    DetailTab.HISTORY -> Res.string.detail_tab_history
    DetailTab.RECORDS -> Res.string.detail_tab_records
}

/** Cues and mistakes come right after the demo: that's what people check between sets. */
@Composable
private fun FormTab(exercise: ExerciseDetailUi, noteDraft: String, onIntent: (ExerciseDetailIntent) -> Unit) {
    val spacing = LiftlyTheme.spacing
    LazyColumn(
        contentPadding = PaddingValues(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "demo") {
            ExerciseDemo(
                imagePaths = exercise.imagePaths,
                contentDescription = stringResource(Res.string.detail_demo_description, exercise.name),
                modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
            )
        }
        if (exercise.videoUrl != null) {
            item(key = "video") {
                OutlinedButton(onClick = { onIntent(ExerciseDetailIntent.WatchVideo) }) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text(stringResource(Res.string.detail_watch_video), Modifier.padding(start = spacing.sm))
                }
            }
        }
        bulletSection("cues", Res.string.detail_cues, exercise.formCues) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = LiftlyColors.Success, modifier = Modifier.size(20.dp))
        }
        bulletSection("mistakes", Res.string.detail_mistakes, exercise.commonMistakes) {
            Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
        }
        item(key = "muscles") { MusclesSection(exercise) }
        if (exercise.instructions.isNotEmpty()) {
            item(key = "how-title") { Heading(stringResource(Res.string.detail_how_to)) }
            itemsIndexed(exercise.instructions, key = { i, _ -> "step-$i" }) { i, step ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Text("${i + 1}.", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(step, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item(key = "note") {
            OutlinedTextField(
                value = noteDraft,
                onValueChange = { onIntent(ExerciseDetailIntent.NoteChanged(it)) },
                label = { Text(stringResource(Res.string.detail_note)) },
                placeholder = { Text(stringResource(Res.string.detail_note_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
        }
    }
}

private fun LazyListScope.bulletSection(
    key: String,
    title: org.jetbrains.compose.resources.StringResource,
    lines: List<String>,
    icon: @Composable () -> Unit,
) {
    if (lines.isEmpty()) return
    item(key = "$key-title") { Heading(stringResource(title)) }
    itemsIndexed(lines, key = { i, _ -> "$key-$i" }) { _, line ->
        Row(horizontalArrangement = Arrangement.spacedBy(LiftlyTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            icon()
            Text(line, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun MusclesSection(exercise: ExerciseDetailUi) {
    val spacing = LiftlyTheme.spacing
    val primary = exercise.primaryMuscle.label()
    val secondary = exercise.secondaryMuscles.map { it.label() }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Heading(stringResource(Res.string.detail_muscles))
        MuscleDiagram(
            intensity = muscleIntensity(exercise.primaryMuscle, exercise.secondaryMuscles),
            contentDescription = (listOf(primary) + secondary).joinToString(),
            modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp).align(Alignment.CenterHorizontally),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            AssistChip(onClick = {}, label = { Text("${stringResource(Res.string.detail_primary)}: $primary") })
            secondary.forEach { name ->
                AssistChip(onClick = {}, label = { Text("${stringResource(Res.string.detail_secondary)}: $name") })
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
}
