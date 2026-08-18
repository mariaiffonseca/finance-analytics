package com.mariafonseca.financeanalytics.core.designsystem.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mariafonseca.financeanalytics.core.designsystem.LocalFinanceColors
import com.mariafonseca.financeanalytics.features.overview.presentation.CategorySlice

private val DonutDiameter = 112.dp
private val DonutStrokeWidth = 16.dp
private const val START_ANGLE_DEGREES = -90f
private const val FULL_SWEEP_DEGREES = 360.0

/**
 * Category-spending donut (docs/project/05_DESIGN_SYSTEM.md §14): 112dp
 * outer diameter, 16dp ring width, background-coloured centre. The largest
 * category ([slices] must already be sorted descending by share, as
 * [com.mariafonseca.financeanalytics.features.overview.presentation.toMonthlyOverview]
 * returns them) uses the accent colour; the rest use a neutral ramp so the
 * chart never relies on many saturated colours.
 */
@Composable
fun CategoryDonutChart(slices: List<CategorySlice>, modifier: Modifier = Modifier) {
    val colors = LocalFinanceColors.current

    Canvas(modifier = modifier.size(DonutDiameter)) {
        val strokeWidthPx = DonutStrokeWidth.toPx()
        val arcTopLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)
        val arcSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)

        if (slices.isEmpty()) {
            drawArc(
                color = colors.divider,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidthPx),
                topLeft = arcTopLeft,
                size = arcSize,
            )
            return@Canvas
        }

        var startAngle = START_ANGLE_DEGREES
        slices.forEachIndexed { index, slice ->
            val sweepAngle = (slice.share * FULL_SWEEP_DEGREES).toFloat()
            drawArc(
                color = categorySliceColor(index, colors),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidthPx),
                topLeft = arcTopLeft,
                size = arcSize,
            )
            startAngle += sweepAngle
        }
    }
}
