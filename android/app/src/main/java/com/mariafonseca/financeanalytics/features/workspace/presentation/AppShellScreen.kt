package com.mariafonseca.financeanalytics.features.workspace.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mariafonseca.financeanalytics.core.designsystem.LocalFinanceColors
import com.mariafonseca.financeanalytics.core.navigation.AppTabDestinations

private data class TabItem(
    val route: String,
    val label: String,
    val icon: @Composable (Color) -> Unit,
)

private val tabItems = listOf(
    TabItem(AppTabDestinations.OVERVIEW_ROUTE, "Overview") { OverviewTabIcon(it) },
    TabItem(AppTabDestinations.INSIGHTS_ROUTE, "Insights") { InsightsTabIcon(it) },
    TabItem(AppTabDestinations.TRANSACTIONS_ROUTE, "Transactions") { TransactionsTabIcon(it) },
)

@Composable
fun AppShellScreen() {
    val tabNavController = rememberNavController()
    val colors = LocalFinanceColors.current

    // The outer NavHost clears Empty/Import from its back stack on entering the
    // app phase, so without this a single system back-press here exits the app
    // outright. There's no deeper navigation yet to fall back to, so swallow it.
    BackHandler {}

    Scaffold(
        bottomBar = {
            val backStackEntry by tabNavController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination

            NavigationBar {
                tabItems.forEach { tab ->
                    val selected = currentRoute?.hierarchy?.any { it.route == tab.route } == true
                    val onClick = remember(tab.route) {
                        {
                            tabNavController.navigate(tab.route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                    NavigationBarItem(
                        selected = selected,
                        onClick = onClick,
                        icon = {
                            val tint = if (selected) colors.accent else colors.textSecondary
                            tab.icon(tint)
                        },
                        label = { Text(text = tab.label) },
                    )
                }
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = tabNavController,
            startDestination = AppTabDestinations.OVERVIEW_ROUTE,
            modifier = Modifier.padding(paddingValues),
        ) {
            composable(AppTabDestinations.OVERVIEW_ROUTE) { OverviewStubScreen() }
            composable(AppTabDestinations.INSIGHTS_ROUTE) { InsightsStubScreen() }
            composable(AppTabDestinations.TRANSACTIONS_ROUTE) { TransactionsStubScreen() }
        }
    }
}
