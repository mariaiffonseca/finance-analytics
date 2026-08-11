package com.mariafonseca.financeanalytics.features.workspace.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private val IconSize = 20.dp
private val StrokeWidth = 1.8.dp

@Composable
fun OverviewTabIcon(tint: Color) {
    val density = LocalDensity.current
    val stroke = remember(density) { Stroke(width = with(density) { StrokeWidth.toPx() }) }
    val offsets = remember(density) {
        val iconPx = with(density) { IconSize.toPx() }
        val cell = iconPx * 0.42f
        val gap = iconPx * 0.16f
        cell to listOf(
            Offset(0f, 0f),
            Offset(cell + gap, 0f),
            Offset(0f, cell + gap),
            Offset(cell + gap, cell + gap),
        )
    }
    val (cell, points) = offsets
    Canvas(modifier = Modifier.size(IconSize)) {
        points.forEach { offset ->
            drawRect(color = tint, topLeft = offset, size = Size(cell, cell), style = stroke)
        }
    }
}

@Composable
fun InsightsTabIcon(tint: Color) {
    val density = LocalDensity.current
    val stroke = remember(density) { Stroke(width = with(density) { StrokeWidth.toPx() }) }
    Canvas(modifier = Modifier.size(IconSize)) {
        val inset = size.width * 0.22f
        val side = size.width - inset * 2
        rotate(degrees = 45f) {
            drawRect(
                color = tint,
                topLeft = Offset(inset, inset),
                size = Size(side, side),
                style = stroke,
            )
        }
    }
}

@Composable
fun TransactionsTabIcon(tint: Color) {
    val density = LocalDensity.current
    val stroke = remember(density) { Stroke(width = with(density) { StrokeWidth.toPx() }) }
    Canvas(modifier = Modifier.size(IconSize)) {
        val strokePx = stroke.width
        val ys = listOf(size.height * 0.25f, size.height * 0.5f, size.height * 0.75f)
        ys.forEachIndexed { index, y ->
            val endX = if (index == ys.lastIndex) size.width * 0.6f else size.width
            drawLine(
                color = tint,
                start = Offset(0f, y),
                end = Offset(endX, y),
                strokeWidth = strokePx,
            )
        }
    }
}
