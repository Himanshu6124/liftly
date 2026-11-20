package com.himanshu.liftly.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** One point of a [LineChart]: x is a timestamp (epoch millis), y the value. */
@Immutable
data class ChartPoint(val x: Long, val y: Double)

/**
 * Minimal time-series line chart drawn on a Canvas (no chart library): three horizontal grid
 * lines with labels, the line with dots, and the selected point highlighted. Tapping selects the
 * nearest point. Points must be sorted by x. Screen readers get [description] instead of the drawing.
 */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    description: String,
    formatY: (Double) -> String,
    formatX: (Long) -> String,
    modifier: Modifier = Modifier,
    selectedIndex: Int? = null,
    onSelect: (Int) -> Unit = {},
    height: Dp = 200.dp,
) {
    val colors = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val measurer = rememberTextMeasurer()
    val chart = ChartGeometry(points)
    val gridValues = listOf(chart.minY, (chart.minY + chart.maxY) / 2, chart.maxY)
    // Y labels get their own gutter so points never sit on top of them; tap handling needs its width too.
    val gutter = remember { FloatArray(1) }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = description }
            .pointerInput(points) {
                detectTapGestures { tap ->
                    if (points.isEmpty()) return@detectTapGestures
                    val area = chart.area(gutter[0], size.width.toFloat(), size.height.toFloat(), density)
                    val nearest = points.indices.minBy { abs(area.xOf(points[it].x) - tap.x) }
                    onSelect(nearest)
                }
            },
    ) {
        if (points.isEmpty()) return@Canvas
        val yLabels = gridValues.map { measurer.measure(formatY(it), labelStyle) }
        gutter[0] = yLabels.maxOf { it.size.width } + 8.dp.toPx()
        val area = chart.area(gutter[0], size.width, size.height, density)

        // Grid: min, middle and max values, labelled in the gutter.
        gridValues.zip(yLabels).forEach { (v, label) ->
            val y = area.yOf(v)
            drawLine(
                colors.outlineVariant,
                Offset(area.left, y),
                Offset(area.right, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
            )
            drawText(label, topLeft = Offset(0f, (y - label.size.height / 2f).coerceAtLeast(0f)))
        }
        // X labels: first and last date.
        val bottom = size.height
        drawLabel(measurer, formatX(points.first().x), labelStyle, Offset(area.left, bottom), alignRight = false)
        if (points.size > 1) {
            drawLabel(measurer, formatX(points.last().x), labelStyle, Offset(area.right, bottom), alignRight = true)
        }

        val offsets = points.map { Offset(area.xOf(it.x), area.yOf(it.y)) }
        if (offsets.size > 1) {
            val path = Path().apply {
                moveTo(offsets.first().x, offsets.first().y)
                offsets.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, colors.primary, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        offsets.forEachIndexed { i, o ->
            val selected = i == selectedIndex
            drawCircle(colors.primary, radius = (if (selected) 7 else 4).dp.toPx(), center = o)
            if (selected) drawCircle(colors.surface, radius = 3.dp.toPx(), center = o)
        }
    }
}

private fun DrawScope.drawLabel(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    anchor: Offset,
    alignRight: Boolean,
) {
    // Bottom-anchored x-axis label.
    val layout = measurer.measure(text, style)
    val x = if (alignRight) anchor.x - layout.size.width else anchor.x
    drawText(layout, topLeft = Offset(x, anchor.y - layout.size.height))
}

/** Value range with a little headroom so a flat line isn't glued to the edges. */
private class ChartGeometry(points: List<ChartPoint>) {
    val minX = points.minOfOrNull { it.x } ?: 0L
    val maxX = points.maxOfOrNull { it.x } ?: 0L
    val minY: Double
    val maxY: Double

    init {
        val lo = points.minOfOrNull { it.y } ?: 0.0
        val hi = points.maxOfOrNull { it.y } ?: 0.0
        val pad = if (hi > lo) (hi - lo) * PADDING else maxOf(hi * PADDING, 1.0)
        minY = (lo - pad).coerceAtLeast(0.0)
        maxY = hi + pad
    }

    fun area(gutter: Float, width: Float, height: Float, density: Float) = Area(
        left = gutter + EDGE_DP * density,
        right = width - EDGE_DP * density,
        top = TOP_DP * density,
        bottom = height - X_LABEL_HEIGHT_DP * density,
        geometry = this,
    )

    class Area(val left: Float, val right: Float, val top: Float, val bottom: Float, val geometry: ChartGeometry) {
        fun xOf(x: Long): Float {
            val span = geometry.maxX - geometry.minX
            if (span == 0L) return (left + right) / 2
            return left + (right - left) * ((x - geometry.minX).toFloat() / span)
        }

        fun yOf(y: Double): Float {
            val span = geometry.maxY - geometry.minY
            return bottom - (bottom - top) * ((y - geometry.minY) / span).toFloat()
        }
    }

    private companion object {
        const val PADDING = 0.1
        const val EDGE_DP = 8f
        const val TOP_DP = 16f
        const val X_LABEL_HEIGHT_DP = 24f
    }
}
