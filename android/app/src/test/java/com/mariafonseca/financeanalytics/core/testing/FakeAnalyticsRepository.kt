package com.mariafonseca.financeanalytics.core.testing

import com.mariafonseca.financeanalytics.core.analytics.AnalyticsRepository
import com.mariafonseca.financeanalytics.core.analytics.model.AnalyticsResult
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction

/** Shared [AnalyticsRepository] test double, used by any ViewModel test that triggers an analysis. */
class FakeAnalyticsRepository(
    var result: () -> Result<AnalyticsResult>,
) : AnalyticsRepository {
    var callCount = 0
        private set

    override suspend fun analyse(transactions: List<Transaction>): Result<AnalyticsResult> {
        callCount++
        return result()
    }
}
