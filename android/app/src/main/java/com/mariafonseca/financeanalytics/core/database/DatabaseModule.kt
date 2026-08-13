package com.mariafonseca.financeanalytics.core.database

import androidx.room.Room
import com.mariafonseca.financeanalytics.features.transactions.data.TransactionDao
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            FinanceAnalyticsDatabase::class.java,
            "finance_analytics.db",
        )
            // No user-facing release has shipped yet, so there's no data to
            // preserve across a schema change — this is the pre-release
            // equivalent of a real Migration.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single<TransactionDao> { get<FinanceAnalyticsDatabase>().transactionDao() }
}
