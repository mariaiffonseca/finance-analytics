package com.mariafonseca.financeanalytics.core.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Shared HTTP client plumbing (PR-014 §1). Feature-specific API clients
 * (e.g. AnalyticsApi) build on this single [Retrofit] instance rather than
 * each configuring their own OkHttpClient/Json — keeps timeout and
 * serialization behaviour consistent across every API this app ever adds.
 *
 * No logging interceptor is registered here: analytics requests/responses
 * carry financial data, and PR-014 §17 forbids logging transaction payloads
 * or analytics responses. Do not add one without stripping bodies first.
 */
val networkModule = module {
    single {
        Json {
            // Tolerates response fields the API adds later without breaking
            // deserialization — the DTOs still model every field the actual
            // PR-013 contract defines today.
            ignoreUnknownKeys = true
        }
    }
    single {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }
    single {
        Retrofit.Builder()
            .baseUrl(ApiConfig.ANALYTICS_BASE_URL)
            .client(get())
            .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
