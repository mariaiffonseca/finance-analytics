package com.mariafonseca.financeanalytics.features.transactions.data

import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeTransactions(): Flow<List<Transaction>>
    suspend fun getTransaction(id: Long): Transaction?
    suspend fun insertTransactions(transactions: List<Transaction>)
    suspend fun deleteAllTransactions()
}
