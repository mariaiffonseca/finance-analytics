package com.mariafonseca.financeanalytics.core.designsystem

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colour tokens, per docs/project/05_DESIGN_SYSTEM.md section 4.
 * Feature code should read these instead of raw hex values or MaterialTheme's
 * default M3 roles, which don't carry this product's semantic meaning
 * (e.g. positive/warning/accent-tint surfaces).
 */
data class FinanceColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val text: Color,
    val textSecondary: Color,
    val divider: Color,
    val dividerStrong: Color,
    val accent: Color,
    val accentDeep: Color,
    val accentTint: Color,
    val positive: Color,
    val positiveDeep: Color,
    val positiveTint: Color,
    val warningDeep: Color,
    val warningTint: Color,
    val errorTint: Color,
)

val LightFinanceColors = FinanceColors(
    background = LightBackground,
    surface = LightSurface,
    surfaceElevated = LightSurfaceElevated,
    text = LightText,
    textSecondary = LightTextSecondary,
    divider = LightDivider,
    dividerStrong = LightDividerStrong,
    accent = LightAccent,
    accentDeep = LightAccentDeep,
    accentTint = LightAccentTint,
    positive = LightPositive,
    positiveDeep = LightPositiveDeep,
    positiveTint = LightPositiveTint,
    warningDeep = LightWarningDeep,
    warningTint = LightWarningTint,
    errorTint = LightErrorTint,
)

val DarkFinanceColors = FinanceColors(
    background = DarkBackground,
    surface = DarkSurface,
    surfaceElevated = DarkSurfaceElevated,
    text = DarkText,
    textSecondary = DarkTextSecondary,
    divider = DarkDivider,
    dividerStrong = DarkDividerStrong,
    accent = DarkAccent,
    accentDeep = DarkAccentDeep,
    accentTint = DarkAccentTint,
    positive = DarkPositive,
    positiveDeep = DarkPositiveDeep,
    positiveTint = DarkPositiveTint,
    warningDeep = DarkWarningDeep,
    warningTint = DarkWarningTint,
    errorTint = DarkErrorTint,
)

val LocalFinanceColors = staticCompositionLocalOf<FinanceColors> {
    error("No FinanceColors provided — wrap content in FinanceAnalyticsTheme.")
}
