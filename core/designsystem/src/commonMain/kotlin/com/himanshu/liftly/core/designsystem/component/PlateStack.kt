package com.himanshu.liftly.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.himanshu.liftly.core.designsystem.theme.PlateColors

/**
 * One side of a loaded barbell, drawn like the real thing: the sleeve, then the plates heaviest
 * first in competition colors. [perSideKg] are plate weights in kg (convert pound plates first).
 * [description] is what screen readers hear ("Per side: 25 + 15"); empty hides it when nearby text says the same.
 */
@Composable
fun PlateStack(perSideKg: List<Double>, description: String, modifier: Modifier = Modifier, height: Dp = 28.dp) {
    val sleeve = MaterialTheme.colorScheme.outline
    val plateWidth = height * PLATE_WIDTH_RATIO
    val gap = 1.5.dp
    val sleeveWidth = height * SLEEVE_RATIO
    val width = sleeveWidth + (plateWidth + gap) * perSideKg.size
    Canvas(
        modifier.width(width).height(height).clearAndSetSemantics { if (description.isNotEmpty()) contentDescription = description },
    ) {
        val h = size.height
        val pw = plateWidth.toPx()
        val g = gap.toPx()
        val sleeveH = h * SLEEVE_HEIGHT_RATIO
        drawRoundRect(sleeve, Offset(0f, (h - sleeveH) / 2), Size(size.width, sleeveH), CornerRadius(sleeveH / 2))
        var x = sleeveWidth.toPx()
        perSideKg.forEach { kg ->
            val ph = h * heightFraction(kg)
            drawRoundRect(PlateColors.forPlate(kg), Offset(x, (h - ph) / 2), Size(pw, ph), CornerRadius(pw * 0.35f))
            x += pw + g
        }
    }
}

/** Big plates are full height (same diameter); change plates get smaller. */
private fun heightFraction(kg: Double): Float = when {
    kg >= 10.0 -> 1f
    kg >= 5.0 -> 0.72f
    kg >= 2.5 -> 0.56f
    else -> 0.46f
}

private const val PLATE_WIDTH_RATIO = 0.26f
private const val SLEEVE_RATIO = 0.5f
private const val SLEEVE_HEIGHT_RATIO = 0.18f
