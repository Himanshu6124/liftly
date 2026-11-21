package com.himanshu.liftly.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.himanshu.liftly.core.designsystem.theme.PlateColors

/** The weekly goal as a row of plates that get loaded as you train (red, blue, yellow, green, …). */
@Composable
fun WeekPlates(done: Int, goal: Int, description: String, modifier: Modifier = Modifier, height: Dp = 28.dp) {
    val empty = MaterialTheme.colorScheme.surfaceVariant
    val slots = maxOf(goal, done)
    Row(
        modifier.height(height).clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(slots) { i ->
            val color by animateColorAsState(
                if (i < done) PlateColors.Series[i % PlateColors.Series.size] else empty,
                animationSpec = tween(durationMillis = 400, delayMillis = i * 80),
                label = "weekPlate",
            )
            Box(Modifier.weight(1f).fillMaxHeight().background(color, RoundedCornerShape(8.dp)))
        }
    }
}
