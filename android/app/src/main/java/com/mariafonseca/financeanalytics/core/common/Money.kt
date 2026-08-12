package com.mariafonseca.financeanalytics.core.common

/**
 * Signed monetary amount in minor units (e.g. cents), never Float/Double, so
 * persisted and aggregated values stay exact. Negative amounts are expenses,
 * positive amounts are income.
 */
@JvmInline
value class Money(val minorUnits: Long)
