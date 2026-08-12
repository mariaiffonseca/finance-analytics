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
        ).build()
    }
    single<TransactionDao> { get<FinanceAnalyticsDatabase>().transactionDao() }
}
