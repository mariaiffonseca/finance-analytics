package com.mariafonseca.financeanalytics.features.`import`.data

import com.mariafonseca.financeanalytics.core.common.Money
import com.mariafonseca.financeanalytics.features.`import`.model.ImportRowError
import com.mariafonseca.financeanalytics.features.`import`.model.ImportRowErrorReason
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeParseException

sealed interface ImportRowResult {
    data class Valid(val transaction: Transaction) : ImportRowResult
    data class Invalid(val error: ImportRowError) : ImportRowResult
}

/**
 * Validates one raw CSV row against [ImportColumnMapping] and maps it
 * straight to a domain [Transaction] when valid (PR-006 sections 4 and 6).
 *
 * Documented format assumptions:
 * - Date: ISO-8601 `yyyy-MM-dd`.
 * - Amount: optional sign, digits, optional `.` + 1-2 decimal places, no
 *   thousands separators. Sign is preserved as-is (negative = expense,
 *   positive = income, per [Money]) — this pipeline never infers or flips
 *   sign from a separate "type" column.
 * - Currency: the domain model has no currency field yet (deferred to the
 *   currency setting in PR-009), so a `Currency` column — if the file has
 *   one — is validated for consistency across the whole file but is not
 *   persisted. A file mixing currencies is rejected row-by-row rather than
 *   silently merged, since this app cannot yet represent that.
 *
 * One instance is meant to validate all rows of a single file in order,
 * since it tracks the file's currency across calls.
 */
class ImportRowValidator {

    private var expectedCurrency: String? = null

    fun validate(rowNumber: Int, values: List<String>, mapping: ImportColumnMapping): ImportRowResult {
        val date = values.getOrNull(mapping.dateColumn)?.trim().orEmpty()
        val merchant = values.getOrNull(mapping.merchantColumn)?.trim().orEmpty()
        val amount = values.getOrNull(mapping.amountColumn)?.trim().orEmpty()
        val category = mapping.categoryColumn
            ?.let { values.getOrNull(it)?.trim() }
            ?.takeUnless { it.isEmpty() }
        val currency = mapping.currencyColumn
            ?.let { values.getOrNull(it)?.trim() }
            ?.takeUnless { it.isEmpty() }

        if (date.isEmpty()) return invalid(rowNumber, ImportRowErrorReason.MISSING_DATE, "Date is required")
        if (merchant.isEmpty()) return invalid(rowNumber, ImportRowErrorReason.MISSING_MERCHANT, "Merchant is required")
        if (amount.isEmpty()) return invalid(rowNumber, ImportRowErrorReason.MISSING_AMOUNT, "Amount is required")

        val parsedDate = parseDate(date)
            ?: return invalid(rowNumber, ImportRowErrorReason.INVALID_DATE, "Date '$date' is not in yyyy-MM-dd format")
        val minorUnits = parseAmount(amount)
            ?: return invalid(rowNumber, ImportRowErrorReason.INVALID_AMOUNT, "Amount '$amount' is not a valid number")

        if (mapping.currencyColumn != null) {
            if (currency == null) {
                return invalid(
                    rowNumber,
                    ImportRowErrorReason.CURRENCY_MISMATCH,
                    "Currency is required on every row once the file declares a Currency column",
                )
            }
            val expected = expectedCurrency
            if (expected == null) {
                expectedCurrency = currency
            } else if (!currency.equals(expected, ignoreCase = true)) {
                return invalid(
                    rowNumber,
                    ImportRowErrorReason.CURRENCY_MISMATCH,
                    "Currency '$currency' does not match the file's currency '$expected'; " +
                        "mixed-currency files are not supported yet",
                )
            }
        }

        return ImportRowResult.Valid(
            Transaction(
                date = parsedDate,
                merchant = merchant,
                amount = Money(minorUnits),
                category = category,
            ),
        )
    }

    private fun parseDate(value: String): LocalDate? =
        try {
            LocalDate.parse(value)
        } catch (e: DateTimeParseException) {
            null
        }

    private fun parseAmount(value: String): Long? {
        if (!AMOUNT_PATTERN.matches(value)) return null
        return try {
            // BigDecimal#longValueExact (API 19) rather than
            // BigInteger#longValueExact (API 31, above our minSdk 26).
            BigDecimal(value).setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact()
        } catch (e: ArithmeticException) {
            null
        }
    }

    private fun invalid(rowNumber: Int, reason: ImportRowErrorReason, message: String) =
        ImportRowResult.Invalid(ImportRowError(rowNumber, reason, message))

    companion object {
        private val AMOUNT_PATTERN = Regex("""[+-]?\d+(\.\d{1,2})?""")
    }
}
