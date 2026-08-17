package com.mariafonseca.financeanalytics.core.analytics

import org.koin.dsl.module
import retrofit2.Retrofit

val analyticsModule = module {
    single<AnalyticsApi> { get<Retrofit>().create(AnalyticsApi::class.java) }
    single<AnalyticsApiClient> { AnalyticsApiClientImpl(get()) }
    single<AnalyticsRepository> { AnalyticsRepositoryImpl(get()) }
}
