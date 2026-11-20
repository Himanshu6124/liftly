package com.himanshu.liftly.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.himanshu.liftly.core.domain.model.Muscle

/**
 * Simplified front + back body. Each [Muscle] maps to one or more regions.
 *
 * [intensity] gives a 0..1 value per muscle (1 = primary, 0.4 = secondary by default), so the same
 * composable can render a weekly volume heatmap later.
 */
@Composable
fun MuscleDiagram(
    intensity: Map<Muscle, Float>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    highlight: Color = MaterialTheme.colorScheme.primary,
    body: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    Canvas(
        modifier = modifier
            .aspectRatio(FIGURE_W * 2 / FIGURE_H)
            .semantics { this.contentDescription = contentDescription },
    ) {
        val scale = size.height / FIGURE_H
        drawFigure(offsetX = 0f, scale = scale, regions = FRONT, intensity = intensity, highlight = highlight, body = body)
        drawFigure(offsetX = FIGURE_W * scale, scale = scale, regions = BACK, intensity = intensity, highlight = highlight, body = body)
    }
}

/** Convenience for an exercise: primary muscle at full strength, secondaries at 40 %. */
fun muscleIntensity(primary: Muscle, secondary: List<Muscle>): Map<Muscle, Float> =
    secondary.associateWith { 0.4f } + (primary to 1f)

private const val FIGURE_W = 100f
private const val FIGURE_H = 200f

private sealed interface Shape {
    data class Oval(val cx: Float, val cy: Float, val w: Float, val h: Float) : Shape
    data class Rect(val l: Float, val t: Float, val r: Float, val b: Float, val radius: Float = 4f) : Shape
    data class Poly(val points: List<Pair<Float, Float>>) : Shape
}

private fun mirror(cx: Float, cy: Float, w: Float, h: Float) =
    listOf(Shape.Oval(cx, cy, w, h), Shape.Oval(FIGURE_W - cx, cy, w, h))

private val SILHOUETTE: List<Shape> = listOf(
    Shape.Oval(50f, 16f, 20f, 23f), // head
    Shape.Rect(45f, 25f, 55f, 34f, 2f), // neck
    Shape.Rect(29f, 32f, 71f, 92f, 12f), // torso
    Shape.Rect(32f, 86f, 68f, 106f, 8f), // hips
    Shape.Rect(17f, 35f, 30f, 97f, 6f), Shape.Rect(70f, 35f, 83f, 97f, 6f), // arms
    Shape.Oval(23.5f, 101f, 10f, 10f), Shape.Oval(76.5f, 101f, 10f, 10f), // hands
    Shape.Rect(33f, 100f, 49f, 186f, 7f), Shape.Rect(51f, 100f, 67f, 186f, 7f), // legs
    Shape.Oval(41f, 190f, 14f, 7f), Shape.Oval(59f, 190f, 14f, 7f), // feet
)

private val FRONT: Map<Muscle, List<Shape>> = mapOf(
    Muscle.NECK to listOf(Shape.Rect(46f, 26f, 54f, 33f, 2f)),
    Muscle.TRAPS to mirror(41f, 32f, 9f, 5f),
    Muscle.FRONT_DELTS to mirror(32f, 40f, 11f, 11f),
    Muscle.SIDE_DELTS to mirror(24.5f, 41f, 6f, 11f),
    Muscle.CHEST to mirror(41f, 48f, 17f, 13f),
    Muscle.BICEPS to mirror(23.5f, 58f, 9f, 17f),
    Muscle.FOREARMS to mirror(23.5f, 81f, 9f, 18f),
    Muscle.ABS to listOf(Shape.Rect(43f, 57f, 57f, 88f, 5f)),
    Muscle.OBLIQUES to mirror(36.5f, 72f, 7f, 18f),
    Muscle.ABDUCTORS to mirror(34f, 98f, 5f, 12f),
    Muscle.ADDUCTORS to mirror(46f, 110f, 5f, 16f),
    Muscle.QUADS to mirror(40f, 124f, 12f, 34f),
    Muscle.CALVES to mirror(41f, 162f, 8f, 22f),
)

private val BACK: Map<Muscle, List<Shape>> = mapOf(
    Muscle.TRAPS to listOf(Shape.Poly(listOf(50f to 27f, 63f to 36f, 50f to 56f, 37f to 36f))),
    Muscle.REAR_DELTS to mirror(32f, 40f, 11f, 10f),
    Muscle.SIDE_DELTS to mirror(24.5f, 41f, 6f, 11f),
    Muscle.UPPER_BACK to mirror(42f, 50f, 9f, 12f),
    Muscle.LATS to mirror(38f, 64f, 11f, 22f),
    Muscle.TRICEPS to mirror(23.5f, 58f, 9f, 17f),
    Muscle.FOREARMS to mirror(23.5f, 81f, 9f, 18f),
    Muscle.LOWER_BACK to listOf(Shape.Rect(44f, 70f, 56f, 88f, 4f)),
    Muscle.GLUTES to mirror(42f, 98f, 15f, 14f),
    Muscle.ABDUCTORS to mirror(34f, 98f, 5f, 12f),
    Muscle.HAMSTRINGS to mirror(41f, 128f, 12f, 30f),
    Muscle.CALVES to mirror(41f, 162f, 10f, 22f),
)

private fun DrawScope.drawFigure(
    offsetX: Float,
    scale: Float,
    regions: Map<Muscle, List<Shape>>,
    intensity: Map<Muscle, Float>,
    highlight: Color,
    body: Color,
) {
    SILHOUETTE.forEach { drawShape(it, offsetX, scale, body) }
    // Faint outline of every region so the body reads as muscular even when nothing is highlighted.
    regions.values.flatten().forEach { drawShape(it, offsetX, scale, highlight.copy(alpha = 0.08f)) }
    regions.forEach { (muscle, shapes) ->
        val value = intensity[muscle] ?: return@forEach
        if (value <= 0f) return@forEach
        shapes.forEach { drawShape(it, offsetX, scale, highlight.copy(alpha = value.coerceIn(0.15f, 1f))) }
    }
}

private fun DrawScope.drawShape(shape: Shape, offsetX: Float, scale: Float, color: Color) {
    fun x(v: Float) = offsetX + v * scale
    fun y(v: Float) = v * scale
    when (shape) {
        is Shape.Oval -> drawOval(
            color = color,
            topLeft = Offset(x(shape.cx - shape.w / 2), y(shape.cy - shape.h / 2)),
            size = Size(shape.w * scale, shape.h * scale),
        )

        is Shape.Rect -> drawRoundRect(
            color = color,
            topLeft = Offset(x(shape.l), y(shape.t)),
            size = Size((shape.r - shape.l) * scale, (shape.b - shape.t) * scale),
            cornerRadius = CornerRadius(shape.radius * scale),
        )

        is Shape.Poly -> drawPath(
            path = Path().apply {
                shape.points.forEachIndexed { i, (px, py) -> if (i == 0) moveTo(x(px), y(py)) else lineTo(x(px), y(py)) }
                close()
            },
            color = color,
        )
    }
}
