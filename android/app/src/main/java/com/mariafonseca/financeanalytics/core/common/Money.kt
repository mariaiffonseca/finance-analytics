package com.mariafonseca.financeanalytics.core.common

import java.util.Locale
import kotlin.math.abs

/**
 * Signed monetary amount in minor units (e.g. cents), never Float/Double, so
 * persisted and aggregated values stay exact. Negative amounts are expenses,
 * positive amounts are income.
 */
@JvmInline
value class Money(val minorUnits: Long)

private const val MINOR_UNITS_PER_MAJOR_UNIT = 100.0

/** Matches the scaling AnalyticsRequestMapper already sends to the API. */
fun Money.toMajorUnits(): Double = minorUnits / MINOR_UNITS_PER_MAJOR_UNIT

/**
 * Two-decimal, absolute-value formatting shared by every screen that
 * displays a monetary amount. Not locale/currency-aware yet — deferred to
 * the currency setting (docs/project/05_DESIGN_SYSTEM.md §26, still a
 * placeholder screen), same limitation the API's summary already has (no
 * currency field).
 */
fun formatMajorUnits(value: Double): String = String.format(Locale.US, "%.2f", abs(value))
