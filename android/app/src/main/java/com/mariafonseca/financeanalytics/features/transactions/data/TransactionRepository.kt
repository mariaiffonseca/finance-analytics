package com.mariafonseca.financeanalytics.features.transactions.data

import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TransactionRepository {
    fun observeTransactions(): Flow<List<Transaction>>
    fun observeTransactions(startDate: LocalDate, endDate: LocalDate): Flow<List<Transaction>>
    suspend fun getTransaction(id: Long): Transaction?
    suspend fun insertTransactions(transactions: List<Transaction>)
    suspend fun deleteAllTransactions()
}
