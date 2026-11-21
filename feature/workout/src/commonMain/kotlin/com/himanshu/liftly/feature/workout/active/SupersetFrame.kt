package com.himanshu.liftly.feature.workout.active

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.himanshu.liftly.core.domain.util.Supersets
import com.himanshu.liftly.feature.workout.resources.Res
import com.himanshu.liftly.feature.workout.resources.superset_label
import org.jetbrains.compose.resources.stringResource

/**
 * Groups superset cards visually: a "Superset A" label above the first card and a coloured rail
 * down the left of every card in the group. The rail runs through the gap between cards
 * ([gap] = the list spacing) so the group reads as one block. Cards outside a superset pass through.
 */
@Composable
internal fun SupersetFrame(card: ExerciseCardUi, gap: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
    val group = card.supersetGroup
    if (group == null) {
        content()
        return
    }
    val rail = MaterialTheme.colorScheme.tertiary
    Column {
        if (card.supersetStart) {
            Text(
                stringResource(Res.string.superset_label, Supersets.label(group)),
                style = MaterialTheme.typography.labelLarge,
                color = rail,
                modifier = Modifier.padding(start = RAIL_INSET, bottom = 4.dp).semantics { heading() },
            )
        }
        Box(
            Modifier
                .drawBehind {
                    val width = RAIL_WIDTH.toPx()
                    val extendDown = if (card.supersetEnd) 0f else gap.toPx()
                    drawRoundRect(
                        color = rail,
                        topLeft = Offset(0f, 0f),
                        size = Size(width, size.height + extendDown),
                        cornerRadius = CornerRadius(width / 2),
                    )
                }
                .padding(start = RAIL_INSET),
        ) { content() }
    }
}

private val RAIL_WIDTH = 4.dp
private val RAIL_INSET = 12.dp
