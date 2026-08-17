package com.mariafonseca.financeanalytics

import android.app.Application
import com.mariafonseca.financeanalytics.core.analytics.analyticsModule
import com.mariafonseca.financeanalytics.core.common.commonModule
import com.mariafonseca.financeanalytics.core.database.databaseModule
import com.mariafonseca.financeanalytics.core.network.networkModule
import com.mariafonseca.financeanalytics.features.`import`.importModule
import com.mariafonseca.financeanalytics.features.insights.insightsModule
import com.mariafonseca.financeanalytics.features.overview.overviewModule
import com.mariafonseca.financeanalytics.features.transactions.transactionsModule
import com.mariafonseca.financeanalytics.features.workspace.workspaceModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class FinanceAnalyticsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            if (BuildConfig.DEBUG) {
                androidLogger()
            }
            androidContext(this@FinanceAnalyticsApplication)
            modules(
                commonModule,
                networkModule,
                databaseModule,
                analyticsModule,
                workspaceModule,
                overviewModule,
                insightsModule,
                transactionsModule,
                importModule,
            )
        }
    }
}
