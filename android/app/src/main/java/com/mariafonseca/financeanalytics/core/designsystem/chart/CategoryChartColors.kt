package com.mariafonseca.financeanalytics.core.designsystem.chart

import androidx.compose.ui.graphics.Color
import com.mariafonseca.financeanalytics.core.designsystem.FinanceColors

private val NeutralRampAlphas = listOf(0.45f, 0.32f, 0.22f, 0.14f)

/**
 * Colour for the category at [index] in a list already sorted descending
 * by share (docs/project/05_DESIGN_SYSTEM.md §14): the largest category
 * (index 0) uses the accent colour, the rest step down a neutral alpha
 * ramp rather than using multiple saturated colours. Shared by
 * [CategoryDonutChart] and its legend so the two never drift apart.
 */
fun categorySliceColor(index: Int, colors: FinanceColors): Color =
    if (index == 0) colors.accent else colors.textSecondary.copy(alpha = NeutralRampAlphas[(index - 1) % NeutralRampAlphas.size])
