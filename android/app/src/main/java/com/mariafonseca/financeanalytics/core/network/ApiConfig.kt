package com.mariafonseca.financeanalytics.core.network

import com.mariafonseca.financeanalytics.BuildConfig

/**
 * Single point of configuration for API base URLs (PR-014 §14) — feature
 * code should read this object instead of embedding a URL directly, so
 * there is exactly one place to change per environment. The value itself
 * comes from the build (see app/build.gradle.kts's `analyticsApiBaseUrl()`),
 * not a literal here, so it can be overridden per-machine via the gitignored
 * android/local.properties without touching source.
 */
object ApiConfig {
    val ANALYTICS_BASE_URL: String = BuildConfig.ANALYTICS_API_BASE_URL
}
