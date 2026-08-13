package com.mariafonseca.financeanalytics.features.transactions.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TransactionRepositoryImplTest {

    private val entity = TransactionEntity(
        id = 1,
        date = LocalDate.of(2026, 8, 1),
        merchant = "Coffee Shop",
        amountMinorUnits = -450,
        category = "Food",
    )

    @Test
    fun `observeTransactions maps entities emitted by the dao`() = runTest {
        val dao = FakeTransactionDao(entities = listOf(entity))
        val repository = TransactionRepositoryImpl(dao)

        val transactions = repository.observeTransactions().first()

        assertEquals(listOf(entity.toDomain()), transactions)
    }

    @Test
    fun `getTransaction returns null when the dao has no match`() = runTest {
        val dao = FakeTransactionDao()
        val repository = TransactionRepositoryImpl(dao)

        assertNull(repository.getTransaction(id = 99))
    }

    @Test
    fun `insertTransactions maps domain models to entities before delegating to the dao`() = runTest {
        val dao = FakeTransactionDao()
        val repository = TransactionRepositoryImpl(dao)
        val transaction = entity.toDomain()

        repository.insertTransactions(listOf(transaction))

        assertEquals(listOf(transaction.toEntity()), dao.insertedEntities)
    }

    @Test
    fun `deleteAllTransactions delegates to the dao`() = runTest {
        val dao = FakeTransactionDao(entities = listOf(entity))
        val repository = TransactionRepositoryImpl(dao)

        repository.deleteAllTransactions()

        assertEquals(emptyList<TransactionEntity>(), dao.observeAll().first())
    }
}

private class FakeTransactionDao(
    entities: List<TransactionEntity> = emptyList(),
) : TransactionDao {

    private val entitiesFlow = MutableStateFlow(entities)
    val insertedEntities = mutableListOf<TransactionEntity>()

    override fun observeAll(): Flow<List<TransactionEntity>> = entitiesFlow

    override fun observeByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<TransactionEntity>> =
        entitiesFlow

    override suspend fun getById(id: Long): TransactionEntity? =
        entitiesFlow.value.firstOrNull { it.id == id }

    override suspend fun count(): Int = entitiesFlow.value.size

    override suspend fun insertAll(transactions: List<TransactionEntity>) {
        insertedEntities += transactions
        entitiesFlow.value = entitiesFlow.value + transactions
    }

    override suspend fun deleteAll() {
        entitiesFlow.value = emptyList()
    }
}
