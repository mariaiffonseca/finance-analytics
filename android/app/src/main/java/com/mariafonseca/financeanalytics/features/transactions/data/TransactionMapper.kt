package com.mariafonseca.financeanalytics.features.transactions.data

import com.mariafonseca.financeanalytics.core.common.Money
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    date = date,
    merchant = merchant,
    amount = Money(amountMinorUnits),
    category = category,
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    date = date,
    merchant = merchant,
    amountMinorUnits = amount.minorUnits,
    category = category,
)
