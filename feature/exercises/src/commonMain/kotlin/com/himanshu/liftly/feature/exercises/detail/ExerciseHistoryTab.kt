package com.himanshu.liftly.feature.exercises.detail

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.himanshu.liftly.core.designsystem.component.ChartPoint
import com.himanshu.liftly.core.designsystem.component.LiftlyCard
import com.himanshu.liftly.core.designsystem.component.LiftlyChip
import com.himanshu.liftly.core.designsystem.component.LineChart
import com.himanshu.liftly.core.designsystem.component.PillTabs
import com.himanshu.liftly.core.designsystem.component.SectionHeader
import com.himanshu.liftly.core.designsystem.component.workoutDisplayName
import com.himanshu.liftly.core.designsystem.theme.LiftlyTheme
import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.util.DateLabels
import com.himanshu.liftly.core.domain.util.ProgressMetric
import com.himanshu.liftly.core.domain.util.ProgressRange
import com.himanshu.liftly.core.domain.util.SetFormatter
import com.himanshu.liftly.feature.exercises.resources.Res
import com.himanshu.liftly.feature.exercises.resources.progress_best_weight
import com.himanshu.liftly.feature.exercises.resources.progress_chart_description
import com.himanshu.liftly.feature.exercises.resources.progress_distance
import com.himanshu.liftly.feature.exercises.resources.progress_distance_value
import com.himanshu.liftly.feature.exercises.resources.progress_est_1rm
import com.himanshu.liftly.feature.exercises.resources.progress_longest_set
import com.himanshu.liftly.feature.exercises.resources.progress_max_reps
import com.himanshu.liftly.feature.exercises.resources.progress_no_points
import com.himanshu.liftly.feature.exercises.resources.progress_one_point
import com.himanshu.liftly.feature.exercises.resources.progress_range_all
import com.himanshu.liftly.feature.exercises.resources.progress_range_label
import com.himanshu.liftly.feature.exercises.resources.progress_range_month
import com.himanshu.liftly.feature.exercises.resources.progress_range_quarter
import com.himanshu.liftly.feature.exercises.resources.progress_range_year
import com.himanshu.liftly.feature.exercises.resources.progress_reps_value
import com.himanshu.liftly.feature.exercises.resources.progress_sessions
import com.himanshu.liftly.feature.exercises.resources.progress_total_reps
import com.himanshu.liftly.feature.exercises.resources.progress_volume
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

/** Progress chart (metric + range) on top, every session below, newest first. */
@Composable
internal fun HistoryTab(state: ExerciseDetailUiState, onIntent: (ExerciseDetailIntent) -> Unit) {
    val spacing = LiftlyTheme.spacing
    LazyColumn(contentPadding = PaddingValues(vertical = spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        item(key = "chart") { ProgressCard(state.progress, state.unit, onIntent) }
        item(key = "sessions-header") { SectionHeader(stringResource(Res.string.progress_sessions)) }
        items(state.sessions, key = { it.workoutId }) { s ->
            ListItem(
                overlineContent = { Text(s.performedAt.date()) },
                headlineContent = { Text(workoutDisplayName(s.workoutName, s.performedAt)) },
                supportingContent = s.summary?.let { { Text(it) } },
            )
        }
    }
}

@Composable
private fun ProgressCard(progress: ProgressUi, unit: WeightUnit, onIntent: (ExerciseDetailIntent) -> Unit) {
    val spacing = LiftlyTheme.spacing
    val metric = progress.metric ?: return
    Column(Modifier.padding(horizontal = spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        if (progress.metrics.size > 1) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                progress.metrics.forEach { m ->
                    LiftlyChip(
                        selected = m == metric,
                        onClick = { onIntent(ExerciseDetailIntent.MetricSelected(m)) },
                        label = { Text(stringResource(m.label())) },
                    )
                }
            }
        }
        LiftlyCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                val repsTemplate = stringResource(Res.string.progress_reps_value, VALUE)
                val distanceTemplate = stringResource(Res.string.progress_distance_value, VALUE)
                // Plain (non-composable) so the chart can call it while drawing.
                val format = { v: Double -> metric.format(v, unit, repsTemplate, distanceTemplate) }
                val selected = progress.selected?.let { progress.points.getOrNull(it) }
                Text(stringResource(metric.label()), style = MaterialTheme.typography.labelLarge)
                if (selected != null) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        Text(
                            format(selected.value),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            selected.at.date(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
                when (progress.points.size) {
                    0 -> Hint(stringResource(Res.string.progress_no_points))

                    1 -> Hint(stringResource(Res.string.progress_one_point))

                    else -> {
                        val first = progress.points.first()
                        val last = progress.points.last()
                        LineChart(
                            points = progress.points.map { ChartPoint(it.at.toEpochMilliseconds(), it.value) },
                            description = stringResource(
                                Res.string.progress_chart_description,
                                stringResource(metric.label()),
                                progress.points.size,
                                format(first.value),
                                first.at.date(),
                                format(last.value),
                                last.at.date(),
                            ),
                            formatY = format,
                            formatX = { Instant.fromEpochMilliseconds(it).date() },
                            selectedIndex = progress.selected,
                            onSelect = { onIntent(ExerciseDetailIntent.PointSelected(it)) },
                        )
                    }
                }
                RangeSelector(progress.range, onIntent)
            }
        }
    }
}

@Composable
private fun RangeSelector(range: ProgressRange, onIntent: (ExerciseDetailIntent) -> Unit) {
    val labels = ProgressRange.entries.map { stringResource(it.label()) }
    PillTabs(
        options = labels,
        selectedIndex = range.ordinal,
        onSelect = { onIntent(ExerciseDetailIntent.RangeSelected(ProgressRange.entries[it])) },
        descriptions = labels.map { stringResource(Res.string.progress_range_label, it) },
    )
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().height(80.dp).padding(top = LiftlyTheme.spacing.md),
    )
}

private fun Instant.date() = DateLabels.short(this)

private fun ProgressMetric.label(): StringResource = when (this) {
    ProgressMetric.EST_1RM -> Res.string.progress_est_1rm
    ProgressMetric.BEST_WEIGHT -> Res.string.progress_best_weight
    ProgressMetric.VOLUME -> Res.string.progress_volume
    ProgressMetric.MAX_REPS -> Res.string.progress_max_reps
    ProgressMetric.TOTAL_REPS -> Res.string.progress_total_reps
    ProgressMetric.LONGEST_SET -> Res.string.progress_longest_set
    ProgressMetric.DISTANCE -> Res.string.progress_distance
}

private fun ProgressRange.label(): StringResource = when (this) {
    ProgressRange.MONTH -> Res.string.progress_range_month
    ProgressRange.QUARTER -> Res.string.progress_range_quarter
    ProgressRange.YEAR -> Res.string.progress_range_year
    ProgressRange.ALL -> Res.string.progress_range_all
}

private const val VALUE = "\u0000"

private fun ProgressMetric.format(value: Double, unit: WeightUnit, repsTemplate: String, distanceTemplate: String): String =
    when (this) {
        ProgressMetric.EST_1RM, ProgressMetric.BEST_WEIGHT, ProgressMetric.VOLUME -> SetFormatter.weight(value, unit)
        ProgressMetric.MAX_REPS, ProgressMetric.TOTAL_REPS -> repsTemplate.replace(VALUE, value.toLong().toString())
        ProgressMetric.LONGEST_SET -> SetFormatter.clock(value.toInt())
        ProgressMetric.DISTANCE -> distanceTemplate.replace(VALUE, value.toLong().toString())
    }
