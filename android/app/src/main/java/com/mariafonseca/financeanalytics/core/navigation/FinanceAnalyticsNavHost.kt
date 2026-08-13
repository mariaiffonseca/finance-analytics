package com.mariafonseca.financeanalytics.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mariafonseca.financeanalytics.core.common.AppDataViewModel
import com.mariafonseca.financeanalytics.features.`import`.presentation.ImportScreen
import com.mariafonseca.financeanalytics.features.workspace.presentation.AppShellScreen
import com.mariafonseca.financeanalytics.features.workspace.presentation.EmptyScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun FinanceAnalyticsNavHost() {
    val navController = rememberNavController()
    // Hosted once at this scope, not tied to any one screen, since Import's
    // Continue action and future consumers (e.g. Settings' reset) all need
    // the same app-wide appDataState instance.
    val appDataViewModel: AppDataViewModel = koinViewModel()

    NavHost(
        navController = navController,
        startDestination = Destinations.EMPTY_ROUTE,
    ) {
        composable(Destinations.EMPTY_ROUTE) {
            EmptyScreen(
                onImportClick = { navController.navigate(Destinations.IMPORT_ROUTE) },
            )
        }
        composable(Destinations.IMPORT_ROUTE) {
            ImportScreen(
                onBack = { navController.popBackStack() },
                onContinue = {
                    appDataViewModel.onImportCompleted()
                    navController.navigate(Destinations.APP_ROUTE) {
                        popUpTo(Destinations.EMPTY_ROUTE) { inclusive = true }
                    }
                },
                // Deliberately does not call onImportCompleted(): nothing was
                // imported, so AppDataState stays NoData and AppShellScreen's
                // own empty-state copy carries the "no data yet" messaging.
                onSkip = {
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
