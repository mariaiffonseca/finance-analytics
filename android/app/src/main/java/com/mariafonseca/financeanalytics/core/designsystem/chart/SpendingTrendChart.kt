package com.mariafonseca.financeanalytics.core.designsystem.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mariafonseca.financeanalytics.core.designsystem.LocalFinanceColors

private val ChartHeight = 80.dp
private const val LINE_STROKE_WIDTH_PX = 4f
private const val POINT_RADIUS_PX = 5f

/**
 * Minimal spending-trend line chart (docs/project/05_DESIGN_SYSTEM.md §14):
 * an accent line over a lightly tinted fill, small data points, no axes or
 * gridlines. [values] must already be in chronological order. A single
 * value renders as a flat line across the full width rather than one dot,
 * since one point alone cannot show a trend.
 */
@Composable
fun SpendingTrendChart(values: List<Double>, modifier: Modifier = Modifier) {
    val colors = LocalFinanceColors.current
    val displayValues = if (values.size == 1) listOf(values[0], values[0]) else values

    Canvas(modifier = modifier.fillMaxWidth().height(ChartHeight)) {
        if (displayValues.isEmpty()) return@Canvas

        val minValue = displayValues.min()
        val maxValue = displayValues.max()
        val range = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0
        val stepX = size.width / (displayValues.size - 1)

        val points = displayValues.mapIndexed { index, value ->
            val normalized = (value - minValue) / range
            Offset(x = stepX * index, y = size.height - (normalized * size.height).toFloat())
        }

        val linePath = Path().apply {
            points.forEachIndexed { index, point ->
                if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
        }
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }

        drawPath(path = fillPath, color = colors.accentTint, style = Fill)
        drawPath(path = linePath, color = colors.accent, style = Stroke(width = LINE_STROKE_WIDTH_PX))
        points.forEach { point -> drawCircle(color = colors.accent, radius = POINT_RADIUS_PX, center = point) }
    }
}
