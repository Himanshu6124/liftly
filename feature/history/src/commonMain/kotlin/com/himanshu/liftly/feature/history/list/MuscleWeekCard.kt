package com.himanshu.liftly.feature.history.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.himanshu.liftly.core.designsystem.component.LiftlyCard
import com.himanshu.liftly.core.designsystem.component.MuscleDiagram
import com.himanshu.liftly.core.designsystem.component.label
import com.himanshu.liftly.core.designsystem.theme.LiftlyTheme
import com.himanshu.liftly.core.designsystem.theme.PlateColors
import com.himanshu.liftly.feature.history.resources.Res
import com.himanshu.liftly.feature.history.resources.muscles_bar
import com.himanshu.liftly.feature.history.resources.muscles_diagram
import com.himanshu.liftly.feature.history.resources.muscles_empty
import com.himanshu.liftly.feature.history.resources.muscles_hint
import com.himanshu.liftly.feature.history.resources.muscles_next_week
import com.himanshu.liftly.feature.history.resources.muscles_previous_week
import com.himanshu.liftly.feature.history.resources.muscles_title
import com.himanshu.liftly.feature.history.resources.muscles_week_last
import com.himanshu.liftly.feature.history.resources.muscles_week_of
import com.himanshu.liftly.feature.history.resources.muscles_week_this
import org.jetbrains.compose.resources.stringResource

/** Weekly sets per muscle: a body heatmap and one bar per trained muscle, browsable by week. */
@Composable
internal fun MuscleWeekCard(week: MuscleWeekUi, onShowWeek: (delta: Int) -> Unit, modifier: Modifier = Modifier) {
    val spacing = LiftlyTheme.spacing
    LiftlyCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.muscles_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics {
                            heading()
                        },
                    )
                    Text(week.title(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onShowWeek(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(Res.string.muscles_previous_week))
                }
                IconButton(onClick = { onShowWeek(1) }, enabled = week.canGoForward) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(Res.string.muscles_next_week))
                }
            }
            if (week.sets.isEmpty()) {
                Text(
                    stringResource(Res.string.muscles_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            val max = week.sets.first().second
            MuscleDiagram(
                intensity = week.sets.associate { (m, v) -> m to (v / max).toFloat().coerceIn(MIN_INTENSITY, 1f) },
                contentDescription = stringResource(Res.string.muscles_diagram),
                modifier = Modifier.height(180.dp).align(Alignment.CenterHorizontally),
            )
            week.sets.forEachIndexed { i, (muscle, sets) ->
                MuscleBar(muscle.label(), sets, max, PlateColors.Series[i % PlateColors.Series.size])
            }
            Text(
                stringResource(Res.string.muscles_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MuscleBar(label: String, sets: Double, max: Double, color: Color) {
    val spacing = LiftlyTheme.spacing
    val value = formatSets(sets)
    val description = stringResource(Res.string.muscles_bar, label, value)
    Row(
        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(96.dp), maxLines = 1)
        Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((sets / max).toFloat())
                    .clip(RoundedCornerShape(6.dp))
                    .background(color),
            )
        }
        Text(value, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(32.dp))
    }
}

@Composable
private fun MuscleWeekUi.title(): String = when (weekOffset) {
    0 -> stringResource(Res.string.muscles_week_this)
    -1 -> stringResource(Res.string.muscles_week_last)
    else -> stringResource(Res.string.muscles_week_of, weekStart.toString())
}

/** 12 → "12", 7.5 → "7.5". */
private fun formatSets(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()

/** Lightly trained muscles stay visible on the diagram. */
private const val MIN_INTENSITY = 0.25f
