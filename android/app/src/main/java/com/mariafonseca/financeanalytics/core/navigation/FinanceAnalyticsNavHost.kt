package com.mariafonseca.financeanalytics.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mariafonseca.financeanalytics.core.common.AppDataState
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
    val appDataState by appDataViewModel.appDataState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = Destinations.EMPTY_ROUTE,
    ) {
        composable(Destinations.EMPTY_ROUTE) {
            // AppDataViewModel checks Room for already-imported data on
            // startup (see its doc comment); once that resolves to
            // DataAvailable, skip straight to the app shell instead of
            // making a returning user click through the welcome screen
            // and import flow again.
            LaunchedEffect(appDataState) {
                if (appDataState is AppDataState.DataAvailable) {
                    navController.navigate(Destinations.APP_ROUTE) {
                        popUpTo(Destinations.EMPTY_ROUTE) { inclusive = true }
                    }
                }
            }
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
