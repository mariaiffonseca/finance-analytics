package com.mariafonseca.financeanalytics.features.transactions.data

import com.mariafonseca.financeanalytics.features.transactions.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
) : TransactionRepository {

    override fun observeTransactions(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeTransactions(startDate: LocalDate, endDate: LocalDate): Flow<List<Transaction>> =
        transactionDao.observeByDateRange(startDate, endDate).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getTransaction(id: Long): Transaction? =
        transactionDao.getById(id)?.toDomain()

    override suspend fun insertTransactions(transactions: List<Transaction>) {
        transactionDao.insertAll(transactions.map { it.toEntity() })
    }

    override suspend fun deleteAllTransactions() {
        transactionDao.deleteAll()
    }
}
