package com.mariafonseca.financeanalytics.core.testing

import com.mariafonseca.financeanalytics.features.transactions.data.TransactionRepository
import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Shared in-memory [TransactionRepository] test double, backed by a [MutableStateFlow]. */
class FakeTransactionRepository(
    initialTransactions: List<Transaction> = emptyList(),
) : TransactionRepository {

    private val transactionsFlow = MutableStateFlow(initialTransactions)
    val insertedTransactions: List<Transaction> get() = transactionsFlow.value

    override fun observeTransactions(): Flow<List<Transaction>> = transactionsFlow

    override suspend fun getTransaction(id: Long): Transaction? =
        transactionsFlow.value.firstOrNull { it.id == id }

    override suspend fun insertTransactions(transactions: List<Transaction>) {
        transactionsFlow.value = transactionsFlow.value + transactions
    }

    override suspend fun deleteAllTransactions() {
        transactionsFlow.value = emptyList()
    }
}
