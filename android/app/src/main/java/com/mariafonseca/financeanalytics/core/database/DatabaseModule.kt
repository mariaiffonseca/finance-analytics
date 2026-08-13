package com.mariafonseca.financeanalytics.core.database

import androidx.room.Room
import com.mariafonseca.financeanalytics.BuildConfig
import com.mariafonseca.financeanalytics.features.transactions.data.TransactionDao
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        val builder = Room.databaseBuilder(
            androidContext(),
            FinanceAnalyticsDatabase::class.java,
            "finance_analytics.db",
        )
        // Debug-only: no user-facing release has shipped yet, so on a dev
        // build there's no data worth preserving across a schema change —
        // this is the pre-release equivalent of a real Migration. A release
        // build intentionally has no fallback here: if a future version bump
        // ever ships without a real Migration, it should crash loudly
        // instead of silently wiping every user's transaction history.
        if (BuildConfig.DEBUG) {
            builder.fallbackToDestructiveMigration(dropAllTables = true)
        }
        builder.build()
    }
    single<TransactionDao> { get<FinanceAnalyticsDatabase>().transactionDao() }
}
