package com.himanshu.liftly.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.himanshu.liftly.core.designsystem.theme.LiftlyTheme
import com.himanshu.liftly.core.domain.util.TrainingCalendar
import com.himanshu.liftly.feature.home.resources.Res
import com.himanshu.liftly.feature.home.resources.home_goal_description
import com.himanshu.liftly.feature.home.resources.home_goal_left
import com.himanshu.liftly.feature.home.resources.home_goal_met
import com.himanshu.liftly.feature.home.resources.home_goal_progress
import com.himanshu.liftly.feature.home.resources.home_heatmap_description
import com.himanshu.liftly.feature.home.resources.home_heatmap_legend_less
import com.himanshu.liftly.feature.home.resources.home_heatmap_legend_more
import com.himanshu.liftly.feature.home.resources.home_streak
import com.himanshu.liftly.feature.home.resources.home_streak_none
import com.himanshu.liftly.feature.home.resources.home_this_week
import org.jetbrains.compose.resources.stringResource

/** Weekly goal ring + streak on top, the last 16 weeks as a day heatmap below. */
@Composable
internal fun ConsistencyCard(calendar: TrainingCalendar, modifier: Modifier = Modifier) {
    val spacing = LiftlyTheme.spacing
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                GoalRing(calendar)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.home_this_week), style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (calendar.goalMet) {
                            stringResource(Res.string.home_goal_met)
                        } else {
                            stringResource(Res.string.home_goal_left, calendar.goal - calendar.thisWeekCount)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (calendar.streakWeeks > 0) {
                            stringResource(Res.string.home_streak, calendar.streakWeeks)
                        } else {
                            stringResource(Res.string.home_streak_none)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (calendar.streakWeeks >
                            0
                        ) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            Heatmap(calendar)
        }
    }
}

@Composable
private fun GoalRing(calendar: TrainingCalendar) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val progress = MaterialTheme.colorScheme.primary
    val description = stringResource(Res.string.home_goal_description, calendar.thisWeekCount, calendar.goal)
    Box(Modifier.size(72.dp).clearAndSetSemantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(72.dp)) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            val fraction = (calendar.thisWeekCount.toFloat() / calendar.goal).coerceAtMost(1f)
            if (fraction > 0f) {
                drawArc(
                    progress,
                    -90f,
                    360f * fraction,
                    false,
                    Offset(inset, inset),
                    arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            stringResource(Res.string.home_goal_progress, calendar.thisWeekCount, calendar.goal),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/** Columns are Monday-first weeks (oldest left), rows are weekdays; shade = working sets that day. */
@Composable
private fun Heatmap(calendar: TrainingCalendar) {
    val empty = MaterialTheme.colorScheme.surfaceVariant
    val full = MaterialTheme.colorScheme.primary
    val levels = listOf(empty, full.copy(alpha = 0.35f), full.copy(alpha = 0.65f), full)
    val trained = calendar.days.count { it.workouts > 0 }
    val description = stringResource(Res.string.home_heatmap_description, trained, calendar.days.size)
    Column(verticalArrangement = Arrangement.spacedBy(LiftlyTheme.spacing.xs)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(TrainingCalendar.WEEKS / DAYS_PER_WEEK.toFloat())
                .semantics { contentDescription = description },
        ) {
            val gap = 3.dp.toPx()
            val cell = (size.width - gap * (TrainingCalendar.WEEKS - 1)) / TrainingCalendar.WEEKS
            calendar.days.forEachIndexed { i, day ->
                val col = i / DAYS_PER_WEEK
                val row = i % DAYS_PER_WEEK
                drawRoundRect(
                    color = levels[level(day.workingSets, day.workouts)],
                    topLeft = Offset(col * (cell + gap), row * (cell + gap)),
                    size = Size(cell, cell),
                    cornerRadius = CornerRadius(cell * 0.2f),
                )
            }
        }
        Legend(levels)
    }
}

@Composable
private fun Legend(levels: List<Color>) {
    val spacing = LiftlyTheme.spacing
    Row(
        Modifier.fillMaxWidth().clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(spacing.xs, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val style = MaterialTheme.typography.labelSmall
        Text(stringResource(Res.string.home_heatmap_legend_less), style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
        levels.forEach { c -> Canvas(Modifier.size(10.dp)) { drawRoundRect(c, cornerRadius = CornerRadius(2.dp.toPx())) } }
        Text(stringResource(Res.string.home_heatmap_legend_more), style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 0 = rest day; then light / medium / heavy by working sets (a workout with no working sets still counts). */
private fun level(workingSets: Int, workouts: Int): Int = when {
    workouts == 0 -> 0
    workingSets < LIGHT_MAX_SETS -> 1
    workingSets < MEDIUM_MAX_SETS -> 2
    else -> 3
}

private const val DAYS_PER_WEEK = 7
private const val LIGHT_MAX_SETS = 10
private const val MEDIUM_MAX_SETS = 20
