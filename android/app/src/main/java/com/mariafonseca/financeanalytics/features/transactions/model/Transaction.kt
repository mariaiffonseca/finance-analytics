package com.mariafonseca.financeanalytics.features.transactions.model

import com.mariafonseca.financeanalytics.core.common.Money
import java.time.LocalDate

data class Transaction(
    val id: Long = 0,
    val date: LocalDate,
    val merchant: String,
    val amount: Money,
    val category: String? = null,
)
