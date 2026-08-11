package com.mariafonseca.financeanalytics.features.insights.presentation

import androidx.annotation.StringRes
import com.mariafonseca.financeanalytics.R

/**
 * The filter set from docs/project/05_DESIGN_SYSTEM.md section 17. This only
 * tracks which filter is selected — it does not filter or generate insights.
 */
enum class InsightFilter(@param:StringRes val labelRes: Int) {
    RECENT(R.string.insight_filter_recent),
    SPENDING(R.string.insight_filter_spending),
    TRENDS(R.string.insight_filter_trends),
    ANOMALIES(R.string.insight_filter_anomalies),
    RECURRING(R.string.insight_filter_recurring),
}
