package com.mariafonseca.financeanalytics.core.analytics

import com.mariafonseca.financeanalytics.core.common.Money
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.LocalDate

/**
 * End-to-end coverage of the real client stack (PR-014 §16): Retrofit
 * request -> a deterministic local HTTP server standing in for the FastAPI
 * process -> AnalyticsApiClientImpl -> AnalyticsRepositoryImpl -> domain
 * mapping. No live analytics deployment is involved — MockWebServer plays
 * the PR-013 API's actual documented response shape, taken verbatim from
 * the generated openapi.json.
 */
class AnalyticsApiIntegrationTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: AnalyticsRepository

    private val transaction = Transaction(
        id = 1,
        date = LocalDate.of(2026, 8, 1),
        merchant = "Coffee Shop",
        amount = Money(-450),
        category = "Food",
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        val api = retrofit.create(AnalyticsApi::class.java)
        repository = AnalyticsRepositoryImpl(AnalyticsApiClientImpl(api))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `a successful server response is mapped end-to-end into a domain result`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(SUCCESS_RESPONSE_BODY))

        val result = repository.analyse(listOf(transaction))

        assertTrue(result.isSuccess)
        val analysis = result.getOrThrow()
        assertEquals(1, analysis.summary.transactionCount)
        assertEquals(-4.5, analysis.summary.totalExpenses, 0.0001)
        assertEquals(1, analysis.insights.size)
        assertEquals("Restaurant spending increased", analysis.insights.single().title)
        assertEquals(1, analysis.anomalies.size)
        assertEquals(1, analysis.recurring.size)
        assertEquals("Netflix", analysis.recurring.single().merchant)

        val recordedRequest = server.takeRequest()
        assertEquals("/analytics/analyse", recordedRequest.path)
        assertEquals("POST", recordedRequest.method)
    }

    @Test
    fun `a 500 server response surfaces as a categorized failure`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"Internal analytics failure."}"""))

        val result = repository.analyse(listOf(transaction))

        assertTrue(result.isFailure)
    }

    private companion object {
        val SUCCESS_RESPONSE_BODY = """
            {
              "summary": {
                "transaction_count": 1,
                "income_count": 0,
                "expense_count": 1,
                "total_income": 0.0,
                "total_expenses": -4.5,
                "net_savings": -4.5,
                "date_range_start": "2026-08-01",
                "date_range_end": "2026-08-01",
                "unique_merchant_count": 1,
                "unique_category_count": 1
              },
              "insights": [
                {
                  "id": "insight-1",
                  "type": "negative",
                  "title": "Restaurant spending increased",
                  "description": "Restaurant spending increased compared to last month.",
                  "severity": "warning",
                  "confidence": 0.87,
                  "related_transaction_ids": ["1"],
                  "metadata": {},
                  "category": "Food",
                  "merchant": null,
                  "amount": 4.5,
                  "comparison_period": "last_month"
                }
              ],
              "anomalies": [
                {
                  "transaction_id": "1",
                  "anomaly_score": 3.1,
                  "is_anomaly": true,
                  "method": "z-score",
                  "reason": "Amount is far above the merchant's usual range.",
                  "reference_context": {}
                }
              ],
              "recurring": [
                {
                  "merchant": "Netflix",
                  "currency": "EUR",
                  "is_recurring": true,
                  "classification": "Recurring",
                  "confidence_score": 0.95,
                  "frequency": "monthly",
                  "occurrences": 6,
                  "median_amount": -12.99,
                  "amount_variation": 0.0,
                  "median_interval_days": 30.0,
                  "interval_variation": 1.0,
                  "first_seen": "2026-01-05",
                  "last_seen": "2026-07-05",
                  "reason": "Consistent monthly charge.",
                  "transaction_ids": ["3", "4"]
                }
              ],
              "metadata": {
                "requested_transaction_count": 1,
                "processed_transaction_count": 1,
                "duplicate_row_count": 0,
                "duplicate_id_count": 0,
                "invalid_date_count": 0,
                "invalid_amount_count": 0,
                "insight_count": 1,
                "anomaly_count": 1,
                "recurring_count": 1
              }
            }
        """.trimIndent()
    }
}
