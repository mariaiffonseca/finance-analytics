package com.mariafonseca.financeanalytics.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mariafonseca.financeanalytics.features.`import`.presentation.ImportScreen
import com.mariafonseca.financeanalytics.features.workspace.presentation.AppShellScreen
import com.mariafonseca.financeanalytics.features.workspace.presentation.EmptyScreen
import com.mariafonseca.financeanalytics.features.workspace.presentation.WorkspaceViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun FinanceAnalyticsNavHost() {
    val navController = rememberNavController()
    // Resolved once at this scope (rather than each screen's own default
    // koinViewModel() call) so Import's Continue action and the Empty screen
    // share the same appDataState instance.
    val workspaceViewModel: WorkspaceViewModel = koinViewModel()

    NavHost(
        navController = navController,
        startDestination = Destinations.EMPTY_ROUTE,
    ) {
        composable(Destinations.EMPTY_ROUTE) {
            EmptyScreen(
                onImportClick = { navController.navigate(Destinations.IMPORT_ROUTE) },
                viewModel = workspaceViewModel,
            )
        }
        composable(Destinations.IMPORT_ROUTE) {
            ImportScreen(
                onBack = { navController.popBackStack() },
                onContinue = {
                    workspaceViewModel.onImportCompleted()
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
