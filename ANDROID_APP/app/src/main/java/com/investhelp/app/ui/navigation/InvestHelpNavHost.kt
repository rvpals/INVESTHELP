package com.investhelp.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.investhelp.app.ui.account.AccountDetailScreen
import com.investhelp.app.ui.account.AccountFormScreen
import com.investhelp.app.ui.account.AccountListScreen
import com.investhelp.app.ui.account.AccountViewModel
import com.investhelp.app.ui.dashboard.DashboardScreen
import com.investhelp.app.ui.dashboard.DashboardViewModel
import com.investhelp.app.ui.item.ItemDetailScreen
import com.investhelp.app.ui.item.ItemFormScreen
import com.investhelp.app.ui.item.ItemViewModel
import com.investhelp.app.ui.settings.SettingsScreen
import com.investhelp.app.ui.settings.SettingsViewModel
import com.investhelp.app.ui.simulation.SimulationScreen
import com.investhelp.app.ui.simulation.SimulationViewModel
import com.investhelp.app.ui.performance.AccountPerformanceScreen
import com.investhelp.app.ui.performance.AccountPerformanceViewModel
import com.investhelp.app.ui.ai.AiTickerScreen
import com.investhelp.app.ui.ai.AiViewModel
import com.investhelp.app.ui.nextday.NextDayActionsScreen
import com.investhelp.app.ui.nextday.NextDayActionsViewModel
import com.investhelp.app.ui.sqlexplorer.SqlExplorerScreen
import com.investhelp.app.ui.sqlexplorer.SqlExplorerViewModel
import com.investhelp.app.ui.sqlexplorer.SqlResultScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.ui.transaction.AnalyzePriceScreen
import com.investhelp.app.ui.transaction.AnalyzePriceViewModel
import com.investhelp.app.ui.transaction.TransactionFormScreen
import com.investhelp.app.ui.transaction.TransactionListScreen
import com.investhelp.app.ui.transaction.TransactionViewModel
import com.investhelp.app.ui.watchlist.WatchListScreen
import com.investhelp.app.ui.watchlist.WatchListViewModel
import com.investhelp.app.ui.help.HelpScreen
import com.investhelp.app.ui.positions.PositionDetailScreen

@Composable
fun InvestHelpNavHost(
    navController: NavHostController,
    startDestination: Any,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<DashboardRoute> {
            val viewModel: DashboardViewModel = hiltViewModel()
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToItem = { ticker ->
                    navController.navigate(ItemDetailRoute(ticker))
                },
                onNavigateToWatchList = {
                    navController.navigate(WatchListRoute) {
                        popUpTo(DashboardRoute) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable<AccountListRoute> {
            val viewModel: AccountViewModel = hiltViewModel()
            AccountListScreen(
                viewModel = viewModel,
                onNavigateToAccount = { accountId ->
                    navController.navigate(AccountDetailRoute(accountId))
                },
                onAddAccount = {
                    navController.navigate(AccountFormRoute())
                },
                onNavigateToPerformance = {
                    navController.navigate(AccountPerformanceRoute) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<AccountDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<AccountDetailRoute>()
            val viewModel: AccountViewModel = hiltViewModel()
            AccountDetailScreen(
                accountId = route.accountId,
                viewModel = viewModel,
                onEditAccount = {
                    navController.navigate(AccountFormRoute(route.accountId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<AccountFormRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<AccountFormRoute>()
            val viewModel: AccountViewModel = hiltViewModel()
            AccountFormScreen(
                accountId = if (route.accountId == -1L) null else route.accountId,
                viewModel = viewModel,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable<ItemDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ItemDetailRoute>()
            val viewModel: ItemViewModel = hiltViewModel()
            ItemDetailScreen(
                ticker = route.ticker,
                viewModel = viewModel,
                onEditItem = {
                    navController.navigate(ItemFormRoute(ticker = route.ticker))
                },
                onSimulate = { ticker, shares ->
                    navController.navigate(SimulationRoute(ticker = ticker, shares = shares))
                },
                onAi = { ticker ->
                    navController.navigate(AiTickerRoute(ticker = ticker))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<ItemFormRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ItemFormRoute>()
            val viewModel: ItemViewModel = hiltViewModel()
            ItemFormScreen(
                ticker = route.ticker.takeIf { it.isNotBlank() },
                accountId = if (route.accountId == -1L) null else route.accountId,
                viewModel = viewModel,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable<TransactionListRoute> {
            val viewModel: TransactionViewModel = hiltViewModel()
            TransactionListScreen(
                viewModel = viewModel,
                onAddTransaction = {
                    navController.navigate(TransactionFormRoute())
                },
                onEditTransaction = { transactionId ->
                    navController.navigate(TransactionFormRoute(transactionId))
                }
            )
        }

        composable<TransactionFormRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<TransactionFormRoute>()
            val viewModel: TransactionViewModel = hiltViewModel()
            val selectedPrice = backStackEntry.savedStateHandle
                .getStateFlow<String?>("selected_price", null)
                .collectAsStateWithLifecycle()
            TransactionFormScreen(
                transactionId = if (route.transactionId == -1L) null else route.transactionId,
                viewModel = viewModel,
                selectedPrice = selectedPrice.value,
                onAnalyzePrice = { ticker ->
                    navController.navigate(AnalyzePriceRoute(ticker))
                },
                onClearSelectedPrice = {
                    backStackEntry.savedStateHandle["selected_price"] = null
                },
                onViewItem = { ticker ->
                    navController.navigate(ItemDetailRoute(ticker))
                },
                onSimulate = { ticker, shares, days ->
                    navController.navigate(SimulationRoute(ticker = ticker, shares = shares, customDays = days))
                },
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable<AnalyzePriceRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<AnalyzePriceRoute>()
            val viewModel: AnalyzePriceViewModel = hiltViewModel()
            AnalyzePriceScreen(
                ticker = route.ticker,
                viewModel = viewModel,
                onSelectPrice = { price ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        "selected_price", price.toString()
                    )
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }


        composable<SimulationRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<SimulationRoute>()
            val viewModel: SimulationViewModel = hiltViewModel()
            SimulationScreen(
                viewModel = viewModel,
                initialTicker = route.ticker,
                initialShares = if (route.shares > 0.0) route.shares.toBigDecimal().stripTrailingZeros().toPlainString() else "",
                customDays = route.customDays
            )
        }

        composable<SettingsRoute> {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(viewModel = viewModel)
        }

        composable<SqlExplorerRoute> {
            val viewModel: SqlExplorerViewModel = hiltViewModel()
            SqlExplorerScreen(
                viewModel = viewModel,
                onRunQuery = { sql ->
                    navController.navigate(SqlResultRoute(sql))
                }
            )
        }

        composable<SqlResultRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<SqlResultRoute>()
            val viewModel: SqlExplorerViewModel = hiltViewModel()
            SqlResultScreen(
                initialSql = route.sql,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable<AiTickerRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<AiTickerRoute>()
            val aiViewModel: AiViewModel = hiltViewModel()
            AiTickerScreen(
                ticker = route.ticker,
                viewModel = aiViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable<NextDayActionsRoute> {
            val viewModel: NextDayActionsViewModel = hiltViewModel()
            NextDayActionsScreen(viewModel = viewModel)
        }

        composable<AccountPerformanceRoute> {
            val viewModel: AccountPerformanceViewModel = hiltViewModel()
            AccountPerformanceScreen(viewModel = viewModel)
        }

        composable<WatchListRoute> {
            val viewModel: WatchListViewModel = hiltViewModel()
            WatchListScreen(
                viewModel = viewModel,
                onNavigateToItem = { ticker ->
                    navController.navigate(ItemDetailRoute(ticker))
                }
            )
        }

        composable<PositionDetailRoute> {
            val viewModel: ItemViewModel = hiltViewModel()
            PositionDetailScreen(
                viewModel = viewModel,
                onNavigateToItem = { ticker ->
                    navController.navigate(ItemDetailRoute(ticker))
                }
            )
        }

        composable<HelpRoute> {
            HelpScreen()
        }
    }
}
