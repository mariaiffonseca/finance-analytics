package com.mariafonseca.financeanalytics.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mariafonseca.financeanalytics.features.transactions.data.TransactionDao
import com.mariafonseca.financeanalytics.features.transactions.data.TransactionEntity

@Database(entities = [TransactionEntity::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class FinanceAnalyticsDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}
