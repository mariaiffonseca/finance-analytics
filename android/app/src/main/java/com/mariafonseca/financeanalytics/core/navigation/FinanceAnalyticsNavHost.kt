package com.mariafonseca.financeanalytics.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mariafonseca.financeanalytics.features.workspace.presentation.AppShellScreen
import com.mariafonseca.financeanalytics.features.workspace.presentation.EmptyScreen
import com.mariafonseca.financeanalytics.features.workspace.presentation.ImportStubScreen

@Composable
fun FinanceAnalyticsNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destinations.EMPTY_ROUTE,
    ) {
        composable(Destinations.EMPTY_ROUTE) {
            EmptyScreen(onImportClick = { navController.navigate(Destinations.IMPORT_ROUTE) })
        }
        composable(Destinations.IMPORT_ROUTE) {
            ImportStubScreen(
                onBack = { navController.popBackStack() },
                onContinue = {
                    navController.navigate(Destinations.APP_ROUTE) {
                        popUpTo(Destinations.EMPTY_ROUTE) { inclusive = true }
                    }
                },
            )
        }
        composable(Destinations.APP_ROUTE) {
            AppShellScreen()
        }
    }
}
