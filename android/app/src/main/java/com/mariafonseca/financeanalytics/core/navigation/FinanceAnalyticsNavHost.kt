package com.mariafonseca.financeanalytics.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.mariafonseca.financeanalytics.placeholderDestination

@Composable
fun FinanceAnalyticsNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destinations.PLACEHOLDER_ROUTE,
    ) {
        placeholderDestination()
    }
}
