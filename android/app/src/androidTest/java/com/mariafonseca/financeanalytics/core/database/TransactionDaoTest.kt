package com.mariafonseca.financeanalytics.core.database

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.mariafonseca.financeanalytics.features.transactions.data.TransactionDao
import com.mariafonseca.financeanalytics.features.transactions.data.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class TransactionDaoTest {

    private lateinit var database: FinanceAnalyticsDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, FinanceAnalyticsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.transactionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndObserveAll_returnsInsertedTransactionsOrderedByDateDescending() = runBlocking {
        dao.insertAll(
            listOf(
                TransactionEntity(date = LocalDate.of(2026, 8, 1), merchant = "A", amountMinorUnits = -100, category = null),
                TransactionEntity(date = LocalDate.of(2026, 8, 5), merchant = "B", amountMinorUnits = -200, category = null),
            ),
        )

        val transactions = dao.observeAll().first()

        assertEquals(listOf("B", "A"), transactions.map { it.merchant })
    }

    @Test
    fun observeByDateRange_filtersTransactionsOutsideRange() = runBlocking {
        dao.insertAll(
            listOf(
                TransactionEntity(date = LocalDate.of(2026, 7, 1), merchant = "Out of range", amountMinorUnits = -100, category = null),
                TransactionEntity(date = LocalDate.of(2026, 8, 10), merchant = "In range", amountMinorUnits = -200, category = null),
            ),
        )

        val transactions = dao.observeByDateRange(
            startDate = LocalDate.of(2026, 8, 1),
            endDate = LocalDate.of(2026, 8, 31),
        ).first()

        assertEquals(listOf("In range"), transactions.map { it.merchant })
    }

    @Test
    fun getById_returnsNullWhenNoTransactionMatches() = runBlocking {
        assertNull(dao.getById(id = 42))
    }

    @Test
    fun count_reflectsNumberOfPersistedTransactions() = runBlocking {
        dao.insertAll(
            listOf(
                TransactionEntity(date = LocalDate.of(2026, 8, 1), merchant = "A", amountMinorUnits = -100, category = null),
                TransactionEntity(date = LocalDate.of(2026, 8, 2), merchant = "B", amountMinorUnits = -200, category = null),
            ),
        )

        assertEquals(2, dao.count())
    }

    @Test
    fun deleteAll_removesAllPersistedTransactions() = runBlocking {
        dao.insertAll(
            listOf(TransactionEntity(date = LocalDate.of(2026, 8, 1), merchant = "A", amountMinorUnits = -100, category = null)),
        )

        dao.deleteAll()

        assertEquals(emptyList<TransactionEntity>(), dao.observeAll().first())
    }
}
